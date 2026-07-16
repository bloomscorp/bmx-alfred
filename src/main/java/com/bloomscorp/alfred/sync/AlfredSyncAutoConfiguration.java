package com.bloomscorp.alfred.sync;

import com.bloomscorp.alfred.support.AlfredConstants;
import com.bloomscorp.alfred.sync.dao.ISyncLogDAO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Boot auto-configuration for Alfred's sync feature.
 *
 * Activated when alfred.sync.enabled=true.
 * Consumers must also expose an ISyncLogDAO bean (e.g. LoomSyncLogDAO in Loom).
 *
 * Required properties:
 *   alfred.api.key           — API key sent as x-api-key header to the logger service
 *   alfred.api.secret        — HMAC signing secret, also validated on /alfred/sync/start
 *   alfred.sync.batchSize    — rows per batch (default 100)
 *   alfred.sync.maxLogsPerRun — cap on logs processed per sync trigger, 0 = unlimited (default 50000)
 *   alfred.sync.baseUrl      — logger service base URL (default AlfredConstants.DEFAULT_LOGGER_SERVICE_BASE_URL)
 */
@Configuration
@ConditionalOnProperty(name = "alfred.sync.enabled", havingValue = "true")
public class AlfredSyncAutoConfiguration {

    // @Value works here because this class IS a Spring-managed bean (registered via
    // META-INF auto-config), unlike SyncService/SyncController which are created via new.
    @Bean
    public SyncService syncService(
        ISyncLogDAO syncLogDAO,
        @Value("${alfred.api.key}") String apiKey,
        @Value("${alfred.api.secret}") String apiSecret,
        @Value("${alfred.sync.batchSize:100}") int batchSize,
        @Value("${alfred.sync.deleteAfterSync:true}") boolean deleteAfterSync,
        @Value("${alfred.sync.maxLogsPerRun:50000}") int maxLogsPerRun
    ) {
        return new SyncService(syncLogDAO, apiKey, apiSecret, batchSize, deleteAfterSync, maxLogsPerRun);
    }

    @Bean
    public SyncController syncController(
        SyncService syncService,
        @Value("${alfred.api.secret}") String apiSecret,
        @Value("${alfred.sync.baseUrl:" + AlfredConstants.DEFAULT_LOGGER_SERVICE_BASE_URL + "}") String loggerServiceBaseUrl
    ) {
        return new SyncController(syncService, apiSecret, loggerServiceBaseUrl);
    }
}
