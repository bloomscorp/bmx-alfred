package com.bloomscorp.alfred.sync;

import com.bloomscorp.alfred.sync.dao.ISyncLogDAO;
import com.bloomscorp.alfred.sync.pojo.*;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;

// No @Service — instantiated explicitly by AlfredSyncAutoConfiguration.
// All config values come from the constructor, not @Value (which does not inject into
// manually-created beans).
public class SyncService {

    private static final Logger log = LoggerFactory.getLogger(SyncService.class);
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_BASE_MS = 1_000;
    private static final long BATCH_PACING_MS = 50;
    private static final int MAX_STUCK_ITERATIONS = 3;
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private final ISyncLogDAO syncLogDAO;
    private final String apiKey;
    private final String apiSecret;
    private final int batchSize;
    private final boolean deleteAfterSync;
    private final int maxLogsPerRun;

    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(CONNECT_TIMEOUT)
        .build();
    private final ObjectMapper objectMapper = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public SyncService(ISyncLogDAO syncLogDAO, String apiKey, String apiSecret, int batchSize, boolean deleteAfterSync, int maxLogsPerRun) {
        this.syncLogDAO = syncLogDAO;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.batchSize = batchSize;
        this.deleteAfterSync = deleteAfterSync;
        this.maxLogsPerRun = maxLogsPerRun;
    }

    public long countUnsynced() {
        return syncLogDAO.countUnsynced();
    }

    /**
     * Main batch loop — runs on a background thread started by SyncController.
     * Reads the local DB in pages, POSTs each batch to the logger service, marks acked rows,
     * then sends a COMPLETED/FAILED signal. Bulk-deletes synced rows on COMPLETED.
     */
    public void runBatchLoop(String syncId, String loggerServiceBaseUrl) {
        int batchIndex = 0;
        int stuckIterations = 0;
        int totalProcessed = 0;
        boolean capReached = false;

        try {
            while (true) {
                if (maxLogsPerRun > 0 && totalProcessed >= maxLogsPerRun) {
                    // Cap this run here rather than pushing through the whole backlog in one
                    // unsupervised background thread. Remaining rows are still synced=false,
                    // so the next sync trigger picks up right where this one stopped.
                    capReached = true;
                    break;
                }


                // Always re-query from the start: markSynced() below removes acked rows
                // from the WHERE synced=false result set, so the next unprocessed batch
                // is always at the front — an incrementing OFFSET here would skip rows
                // that fell out of the set from under it.
                List<SyncLogRecord> batch = syncLogDAO.fetchUnsyncedBatch(batchSize, 0);
                if (batch.isEmpty()) {
                    break;
                }

                SyncBatchPayload payload = SyncBatchPayload.builder()
                    .syncId(syncId)
                    .batchIndex(batchIndex)
                    .logs(batch)
                    .build();

                SyncBatchAck ack = postBatch(loggerServiceBaseUrl, syncId, payload);

                if (ack != null && ack.getAcked() != null && !ack.getAcked().isEmpty()) {
                    syncLogDAO.markSynced(ack.getAcked());
                    totalProcessed += ack.getAcked().size();
                    stuckIterations = 0;
                } else {
                    // Nothing acked: the same unsynced rows will be at the front of the
                    // next query again, so this isn't transient — the logger service is consistently
                    // rejecting this batch. Without this guard the loop would spin on
                    // it forever.
                    stuckIterations++;
                    if (stuckIterations >= MAX_STUCK_ITERATIONS) {
                        throw new RuntimeException(
                            "Batch stuck: no logs acked after " + MAX_STUCK_ITERATIONS
                                + " consecutive attempts (batchIndex=" + batchIndex + ") — aborting sync");
                    }
                }

                batchIndex++;

                Thread.sleep(BATCH_PACING_MS);
            }

            postComplete(loggerServiceBaseUrl, syncId, SyncCompletePayload.builder()
                .status("COMPLETED")
                .build());

            if (capReached) {
                log.info("[alfred-sync] syncId={} hit per-run cap ({} logs) — remaining backlog left unsynced for the next trigger",
                    syncId, maxLogsPerRun);
            }

            if (deleteAfterSync) {
                syncLogDAO.deleteSynced();
                log.info("[alfred-sync] syncId={} completed, synced rows deleted", syncId);
            } else {
                log.info("[alfred-sync] syncId={} completed, deleteAfterSync=false — rows retained", syncId);
            }

        } catch (Exception e) {
            log.error("[alfred-sync] syncId={} failed: {}", syncId, e.getMessage(), e);
            try {
                postComplete(loggerServiceBaseUrl, syncId, SyncCompletePayload.builder()
                    .status("FAILED")
                    .error(e.getMessage())
                    .build());
            } catch (Exception ex) {
                log.error("[alfred-sync] syncId={} could not send FAILED signal: {}", syncId, ex.getMessage());
            }
        }
    }

    // ─── Internal: HTTP calls ──────────────────────────────────────────────────

    private SyncBatchAck postBatch(String loggerServiceBaseUrl, String syncId, SyncBatchPayload payload)
        throws Exception {

        String url = loggerServiceBaseUrl + "/sync/" + syncId + "/batch";
        String body = objectMapper.writeValueAsString(payload);

        HttpResponse<String> response = postWithRetry(url, body);
        if (response.statusCode() == 409) {
            // Duplicate batch — the logger service already processed it; treat as success with empty ack
            log.warn("[alfred-sync] syncId={} batchIndex={} duplicate (409), skipping", syncId, payload.getBatchIndex());
            return new SyncBatchAck(List.of());
        }
        if (response.statusCode() != 200 && response.statusCode() != 201) {
            throw new RuntimeException("Batch POST failed: HTTP " + response.statusCode());
        }

        return objectMapper.readValue(response.body(), SyncBatchAck.class);
    }

    private void postComplete(String loggerServiceBaseUrl, String syncId, SyncCompletePayload payload)
        throws Exception {

        String url = loggerServiceBaseUrl + "/sync/" + syncId + "/complete";
        String body = objectMapper.writeValueAsString(payload);

        HttpResponse<String> response = postWithRetry(url, body);
        if (response.statusCode() != 200 && response.statusCode() != 201) {
            throw new RuntimeException("Complete POST failed: HTTP " + response.statusCode());
        }
    }

    /**
     * POST with up to MAX_RETRIES attempts and exponential backoff.
     * Signs each request with HMAC-SHA256.
     */
    private HttpResponse<String> postWithRetry(String url, String body) throws Exception {
        Exception lastError = null;

        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                String timestamp = String.valueOf(System.currentTimeMillis());
                String signature = hmacSha256(apiSecret, timestamp + "." + body);

                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Content-Type", "application/json")
                    .header("x-api-key", apiKey)
                    .header("x-signature", signature)
                    .header("x-timestamp", timestamp)
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();

                return httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            } catch (Exception e) {
                lastError = e;
                long delay = RETRY_BASE_MS * (1L << attempt);
                log.warn("[alfred-sync] attempt {}/{} failed for {}: {} — retry in {}ms",
                    attempt + 1, MAX_RETRIES, url, e.getMessage(), delay);
                Thread.sleep(delay);
            }
        }

        throw new RuntimeException("All " + MAX_RETRIES + " attempts failed for " + url, lastError);
    }

    // ─── Signing ──────────────────────────────────────────────────────────────

    private String hmacSha256(String secret, String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec keySpec = new SecretKeySpec(
            secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(keySpec);
        byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(raw);
    }
}
