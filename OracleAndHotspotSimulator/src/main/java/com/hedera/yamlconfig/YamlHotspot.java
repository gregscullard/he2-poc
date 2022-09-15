package com.hedera.yamlconfig;

import org.yaml.snakeyaml.Yaml;

public class YamlHotspot {
    private String accountId = "";
    private String privateKey;
    private int id = 0;
    private String region = "";

    public YamlHotspot(int id, String region, String accountId, String privateKey) {
        this.id = id;
        this.region = region;
        this.accountId = accountId;
        this.privateKey = privateKey;
    }
    public YamlHotspot() {

    }
    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }
}
