package com.bloomscorp.alfred.sync.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class SyncBatchPayload {
    private String syncId;
    private int batchIndex;
    private List<SyncLogRecord> logs;
}
