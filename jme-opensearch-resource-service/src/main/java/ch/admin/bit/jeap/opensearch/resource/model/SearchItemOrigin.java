package ch.admin.bit.jeap.opensearch.resource.model;

import lombok.Data;

import java.time.Instant;

@Data
public class SearchItemOrigin {
    String id;
    String version;
    String bpId;
    Instant created;
    Instant modified;
    Object reference;
}
