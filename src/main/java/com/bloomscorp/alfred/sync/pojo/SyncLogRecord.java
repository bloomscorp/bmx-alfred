package com.bloomscorp.alfred.sync.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class SyncLogRecord {
    private long originalId;
    private String level;
    private String logger;
    private String message;
    private String dataDump;
    private long createdAt;
}
