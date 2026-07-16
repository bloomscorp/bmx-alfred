package com.bloomscorp.alfred.sync.dao;

import com.bloomscorp.alfred.sync.pojo.SyncLogRecord;

import java.util.List;

/**
 * DAO contract for sync operations on the local log table.
 * Implementing class must be registered as a Spring bean.
 */
public interface ISyncLogDAO {

    /** Count of log rows where synced = false. */
    long countUnsynced();

    /**
     * Fetch a page of unsynced log rows mapped to SyncLogRecord.
     * Results must be ordered by id ASC for deterministic pagination.
     */
    List<SyncLogRecord> fetchUnsyncedBatch(int limit, int offset);

    /** Mark all given row IDs as synced = true. */
    void markSynced(List<Long> ids);

    /** Delete all rows where synced = true. Call only after sync completes. */
    void deleteSynced();
}
