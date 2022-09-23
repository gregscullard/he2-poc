package com.hedera.he2poc.common.yamlconfig;

public class YamlApi {
    private String apiKey = "";
    private int apiVerticleCount = 1;
    private String httpsCertificate = "";
    private String httpsKeyOrPass = "";
    private int apiPort = 8080;

    public int getApiPort() {
        return apiPort;
    }

    public void setApiPort(int apiPort) {
        this.apiPort = apiPort;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getHttpsKeyOrPass() {
        return httpsKeyOrPass;
    }

    public void setHttpsKeyOrPass(String httpsKeyOrPass) {
        this.httpsKeyOrPass = httpsKeyOrPass;
    }

    public String getHttpsCertificate() {
        return httpsCertificate;
    }

    public void setHttpsCertificate(String httpsCertificate) {
        this.httpsCertificate = httpsCertificate;
    }

    public int getApiVerticleCount() {
        return apiVerticleCount;
    }

    public void setApiVerticleCount(int apiVerticleCount) {
        this.apiVerticleCount = apiVerticleCount;
    }
}