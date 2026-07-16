package com.bloomscorp.alfred.sync.pojo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class SyncStartRequest {
    private String syncId;
}
