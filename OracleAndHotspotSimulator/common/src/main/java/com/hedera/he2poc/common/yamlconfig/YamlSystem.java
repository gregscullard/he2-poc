package com.hedera.he2poc.common.yamlconfig;

public class YamlSystem {
    private long initialHotspotBalance = 2;

    public long getInitialHotspotBalance() {
        return initialHotspotBalance;
    }

    public void setInitialHotspotBalance(long initialHotspotBalance) {
        this.initialHotspotBalance = initialHotspotBalance;
    }
}
