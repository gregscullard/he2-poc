package com.hedera.he2poc.common.yamlconfig;

import com.hedera.hashgraph.sdk.AccountId;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class YamlConfig {
    private String topicId = "";
    private YamlToken token = new YamlToken();
    private YamlApi api = new YamlApi();
    private List<YamlHotspot> hotspots = new ArrayList<>();

    private List<YamlHotspot> demoHotspots = new ArrayList<>();
    private int reportInterval = 100;

    private YamlOracle oracle = new YamlOracle();

    private YamlSystem system = new YamlSystem();

    public YamlSystem getSystem() {
        return this.system;
    }

    public void setSystem(YamlSystem system) {
        this.system = system;
    }

    public YamlOracle getOracle() {
        return this.oracle;
    }

    public void setOracle(YamlOracle oracle) {
        this.oracle = oracle;
    }

    public int getReportInterval() {
        return this.reportInterval;
    }

    public void setReportInterval(int reportInterval) {
        this.reportInterval = reportInterval;
    }

    // Topic
    public void setTopicId(String topicId) {
        this.topicId = topicId;
    }

    public String getTopicId() {
        return this.topicId;
    }

    public void setHotspots(List<YamlHotspot> hotspots) {
        this.hotspots = hotspots;
    }

    public List<YamlHotspot> getHotspots() {
        return this.hotspots;
    }

    public void setDemoHotspots(List<YamlHotspot> hotspots) {
        this.demoHotspots = hotspots;
    }

    public List<YamlHotspot> getDemoHotspots() {
        return this.demoHotspots;
    }

    public YamlApi getApi() {
        return this.api;
    }

    public void setApi(YamlApi api) {
        this.api = api;
    }

    public YamlToken getToken() {
        return this.token;
    }

    public void setToken(YamlToken token) {
        this.token = token;
    }

    public List<AccountId> getHotspotAcountIds(List<YamlHotspot> hotspots) {
        List<AccountId> accountIds = new ArrayList<>();
        for (int i=0; i<hotspots.size(); i++) {
            accountIds.add(AccountId.fromString(hotspots.get(i).getAccountId()));
        }
        return accountIds;
    }
}