package com.bloomscorp.alfred.sync.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class SyncCompletePayload {
    private String status;
    private String error;
}
