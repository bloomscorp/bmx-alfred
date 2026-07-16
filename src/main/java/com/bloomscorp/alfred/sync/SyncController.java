package com.bloomscorp.alfred.sync;

import com.bloomscorp.alfred.sync.pojo.SyncStartRequest;
import com.bloomscorp.alfred.sync.pojo.SyncStartResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.MessageDigest;
import java.util.Map;

// No @Service/@Component — registered as a @Bean by AlfredSyncAutoConfiguration.
// @Value injection does NOT work on beans created via new; all fields come from the constructor.
@RestController
@RequestMapping("/alfred/sync")
public class SyncController {

    private final String apiSecret;
    private final String loggerServiceBaseUrl;
    private final SyncService syncService;

    public SyncController(
        SyncService syncService,
        String apiSecret,
        String loggerServiceBaseUrl
    ) {
        this.syncService = syncService;
        this.apiSecret = apiSecret;
        this.loggerServiceBaseUrl = loggerServiceBaseUrl;
    }

    /**
     * Called by the logger service to initiate a sync.
     * Validates x-alfred-secret (must equal alfred.api.secret), responds with total
     * unsynced count, and starts the batch loop on a background thread.
     */
    @PostMapping("/start")
    public ResponseEntity<Object> start(
        @RequestHeader("x-alfred-secret") String receivedSecret,
        @RequestBody SyncStartRequest request
    ) {
        if (!timingSafeEquals(apiSecret, receivedSecret)) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        long total = syncService.countUnsynced();

        new Thread(() -> syncService.runBatchLoop(
            request.getSyncId(),
            loggerServiceBaseUrl
        )).start();

        return ResponseEntity.ok(new SyncStartResponse(total));
    }

    private boolean timingSafeEquals(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(), b.getBytes());
    }
}
