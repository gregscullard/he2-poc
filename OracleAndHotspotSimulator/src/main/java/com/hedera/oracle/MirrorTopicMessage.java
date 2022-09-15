package com.hedera.oracle;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.protobuf.ByteString;
import java.util.Base64;

/**
 * Data class to map JSON to a java object
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public class MirrorTopicMessage {
    @JsonProperty("consensus_timestamp")
    public String consensusTimestamp = "";

    @JsonProperty("message")
    public String messageBase64 = "";

    public ByteString message() {
        if (messageBase64.isEmpty()) {
            byte[] bytes = Base64.getDecoder().decode(messageBase64);
            return ByteString.copyFrom(bytes);
        } else {
            return ByteString.empty();
        }
    }
}


