package com.bloomscorp.alfred.sync.pojo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class SyncBatchAck {
    private List<Long> acked;
}
