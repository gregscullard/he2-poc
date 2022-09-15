package com.hedera.yamlconfig;

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
    private int hotspotsToStart = 1;
    private int reportInterval = 100;

    private boolean hotspotsSimulator = false;
    private boolean oracle = false;

    public boolean isHotspotsSimulator() {
        return hotspotsSimulator;
    }

    public void setHotspotsSimulator(boolean hotspotsSimulator) {
        this.hotspotsSimulator = hotspotsSimulator;
    }

    public boolean isOracle() {
        return oracle;
    }

    public void setOracle(boolean oracle) {
        this.oracle = oracle;
    }

    public int getHotspotsToStart() {
        return hotspotsToStart;
    }

    public void setHotspotsToStart(int hotspotsToStart) {
        this.hotspotsToStart = hotspotsToStart;
    }

    public int getReportInterval() {
        return reportInterval;
    }

    public void setReportInterval(int reportInterval) {
        this.reportInterval = reportInterval;
    }

    // Topic
    public void setTopicId(String topicId) {
        this.topicId = topicId;
    }

    public String getTopicId() {
        return topicId;
    }

    public void setHotspots(List<YamlHotspot> hotspots) {
        this.hotspots = hotspots;
    }

    public List<YamlHotspot> getHotspots() {
        return hotspots;
    }

    public YamlApi getApi() {
        return api;
    }

    public void setApi(YamlApi api) {
        this.api = api;
    }

    public YamlToken getToken() {
        return token;
    }

    public void setToken(YamlToken token) {
        this.token = token;
    }

    public List<AccountId> getHotspotAcountIds() {
        List<AccountId> accountIds = new ArrayList<>();
        for (int i=0; i<hotspots.size(); i++) {
            accountIds.add(AccountId.fromString(hotspots.get(i).getAccountId()));
        }
        return accountIds;
    }

    public Map<Integer, AccountId> getHotspotIdAccountMap() {
        Map<Integer, AccountId> accountIds = new HashMap<>();
        for (int i=0; i<hotspots.size(); i++) {
            accountIds.put(hotspots.get(i).getId(), AccountId.fromString(hotspots.get(i).getAccountId()));
        }
        return accountIds;
    }

}