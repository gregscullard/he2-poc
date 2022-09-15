package com.hedera.hotspot;

import com.hedera.Secrets;
import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.TopicId;
import com.hedera.yamlconfig.YamlConfigManager;
import com.hedera.yamlconfig.YamlHotspot;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import lombok.extern.log4j.Log4j2;

import java.io.FileNotFoundException;
import java.util.*;

@Log4j2
public final class Hotspots {
//    private final static Map<Integer, Hotspot> threadMap = new HashMap<>();
    private final static NavigableMap<Integer, Hotspot> threadMap = new TreeMap<Integer, Hotspot>();
    private final YamlConfigManager yamlConfigManager;
    private final Secrets secrets;
    private final TopicId topicId;
    private final String network;
    private final List<YamlHotspot> hotspots;
    private int interval;

    public Hotspots() throws FileNotFoundException {
        this.yamlConfigManager = new YamlConfigManager();
        this.secrets = new Secrets();
        this.network = secrets.network();
        this.topicId = TopicId.fromString(yamlConfigManager.getTopicId());
        this.hotspots = yamlConfigManager.getHotspots();
        this.interval = yamlConfigManager.getReportInterval();
    }

    public String setInterval(int interval) {
        this.interval = interval;
        threadMap.forEach((id, thread) -> {
            thread.setIntervalMs(this.interval);
        });
        return "success";
    }

    public String setInterval(int id, int interval) {
        if (threadMap.containsKey(id)) {
            threadMap.get(id).setIntervalMs(interval);
            return "success";
        } else {
            return "hotstpot not started";
        }
    }

    public int getInterval() {
        return this.interval;
    }

    public String startHotspot() {
        String result = "success";
        for (int i=0; i<hotspots.size(); i++) {
            if ( ! threadMap.containsKey(i)) {
                result = startHotspot(i);
                return result;
            }
        }
        return ("Maximum number of hotspots started already");
    }
    public String startHotspot(int id) {
        String result = "success";
        if (threadMap.containsKey(id)) {
            result = "Hotspot id %d already started";
            result = String.format(result, id);
            log.warn(result);
            return result;
        }
        YamlHotspot hotspot = this.hotspots.get(id);
        PrivateKey privateKey = PrivateKey.fromString(hotspot.getPrivateKey());
        AccountId accountId = AccountId.fromString(hotspot.getAccountId());
        String region = hotspot.getRegion();

        Hotspot hotspotInstance = new Hotspot(id, region, topicId, privateKey, accountId, network);
        hotspotInstance.setIntervalMs(this.interval);
        Thread thread = new Thread(hotspotInstance);
        threadMap.put(id, hotspotInstance);
        thread.start();
        return result;
    }
    public String stopHotSpot() {
        String result = "success";
        if (threadMap.isEmpty()) {
            result = "No more hotspots to stop";
            log.warn(result);
            return result;
        }
        Integer lastKey = threadMap.lastEntry().getKey();
        result = stopHotSpot(lastKey);
        threadMap.remove(lastKey);
        return result;
    }
    public String stopHotSpot(int id) {
        String result = "success";
        if ( ! threadMap.containsKey(id)) {
            result = "Hotspot id %d already stopped";
            result = String.format(result, id);
            log.warn(result);
            return result;
        }
        threadMap.get(id).stop();
        return result;
    }
    public void stopAll() {
        threadMap.forEach((id, thread) -> {
            thread.stop();
        });
    }

    public JsonObject getDetails() {

        JsonObject response = new JsonObject();
        JsonArray details = new JsonArray();
        for (YamlHotspot hotspot : yamlConfigManager.getHotspots()) {
            JsonObject hotspotJson = new JsonObject();
            hotspotJson.put("id", hotspot.getId());
            hotspotJson.put("region", hotspot.getRegion());
            hotspotJson.put("enabled", false);
            if ( threadMap.containsKey(hotspot.getId())) {
                Hotspot hotspotInstance = threadMap.get(hotspot.getId());
                hotspotJson.put("runData", hotspotInstance.getDetails());
                hotspotJson.put("enabled", true);
            }
            details.add(hotspotJson);
        }

        response.put("hotspots", details);
        return response;
    }
}
