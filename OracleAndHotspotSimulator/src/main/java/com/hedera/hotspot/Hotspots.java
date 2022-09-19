package com.hedera.hotspot;

import com.hedera.Secrets;
import com.hedera.hashgraph.sdk.*;
import com.hedera.yamlconfig.YamlConfigManager;
import com.hedera.yamlconfig.YamlHotspot;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import lombok.extern.log4j.Log4j2;

import java.io.FileNotFoundException;
import java.util.*;
import java.util.concurrent.TimeoutException;

@Log4j2
public final class Hotspots {
    private final NavigableMap<Integer, Hotspot> threadMap = new TreeMap<>();
    private final List<Integer> ids = new ArrayList<>();
    private final List<Integer> enabledIds = new ArrayList<>();
    private final YamlConfigManager yamlConfigManager;
    private final Secrets secrets;
    private final TopicId topicId;
    private final String network;
    private final Map<Integer, YamlHotspot> hotspots;
    private int interval;
    private final int configuredHotspotCount;
    private final long initialHotspotBalance;

    public Hotspots() throws FileNotFoundException {
        this.yamlConfigManager = new YamlConfigManager();
        this.secrets = new Secrets();
        this.network = secrets.network();
        this.topicId = TopicId.fromString(yamlConfigManager.getTopicId());
        this.hotspots = yamlConfigManager.getHotspotsAsMap();
        this.interval = yamlConfigManager.getReportInterval();
        this.configuredHotspotCount = yamlConfigManager.getHotspots().size();
        this.initialHotspotBalance = yamlConfigManager.getInitialHotspotBalance();

        for (int i=1; i <= this.hotspots.size(); i++) {
            ids.add(this.hotspots.get(i).getId());
        }
    }

    public List<Integer> getEnabledIds() {
        return enabledIds;
    }

    public Map<Integer, YamlHotspot> getHotspots() {
        return hotspots;
    }

    public void remove(int id) {
        // assumes the running thread has stopped itself
        if (threadMap.containsKey(id)) {
            threadMap.get(id).stop();
            try {
                hotspots.remove(id);
            } catch (Exception e) {
                log.error(e);
            }
            try {
                threadMap.remove(id);
            } catch (Exception e) {
                log.error(e);
            }
            for (int i=0; i<enabledIds.size(); i++) {
                if (enabledIds.get(i) == id) {
                    enabledIds.remove(i);
                    break;
                }
            }
        }
    }
    public String add(String name, String key, String nft) throws PrecheckStatusException, TimeoutException, ReceiptStatusException, FileNotFoundException {
        Client client = Client.forName(secrets.network());
        client.setOperator(secrets.accountId(), secrets.privateKey());
        // create account
        PrivateKey privateKey = PrivateKey.fromString(key);
        TransactionResponse response = new AccountCreateTransaction()
                .setKey(privateKey)
                .setInitialBalance(new Hbar(this.initialHotspotBalance))
                .setMaxAutomaticTokenAssociations(2)
                .execute(client);
        TransactionReceipt receipt = response.getReceipt(client);

        client.close();
        // add hotspot
        YamlHotspot yamlHotspot = new YamlHotspot();
        yamlHotspot.setAccountId(receipt.accountId.toString());
        yamlHotspot.setPrivateKey(key);
        yamlHotspot.setNft(nft);
        int maxId = ids.get(ids.size()-1) + 1;
        if (maxId < yamlConfigManager.getHotspots().size()) {
            maxId = yamlConfigManager.getHotspots().size();
        }
        yamlHotspot.setId(maxId);
        yamlHotspot.setName(name);

        log.debug("Adding {} to hotspots", maxId);
        this.hotspots.put(maxId, yamlHotspot);
        ids.add(maxId);
        startHotspot(maxId);

        return "success";
    }
    public String disable(int id) {
        if (threadMap.containsKey(id)) {
            threadMap.get(id).stop();
            enabledIds.remove(id);
            threadMap.remove(id);
            return "success";
        } else {
            return "invalid hotspot id";
        }
    }

    public String setInterval(int interval) {
        this.interval = interval;
        threadMap.forEach((id, thread) -> thread.setIntervalMs(this.interval));
        return "success";
    }

    public String setInterval(int id, int interval) {
        if (threadMap.containsKey(id)) {
            threadMap.get(id).setIntervalMs(interval);
            return "success";
        } else {
            return "hotspot not started";
        }
    }

    public String startHotspot(int id) throws FileNotFoundException, PrecheckStatusException, TimeoutException {
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
        String name = hotspot.getName();
        String nft = hotspot.getNft();
        List<String> paidAccounts = hotspot.getPaidAccounts();

        Hotspot hotspotInstance = new Hotspot(id, name, topicId, privateKey, accountId, network, paidAccounts, this.configuredHotspotCount, this, nft);
        hotspotInstance.setIntervalMs(this.interval);
        Thread thread = new Thread(hotspotInstance);
        thread.start();
        threadMap.put(id, hotspotInstance);
        enabledIds.add(id);

        return result;
    }
    public JsonObject getDetails() {
        JsonObject response = new JsonObject();
        JsonArray details = new JsonArray();
        for (Map.Entry<Integer, YamlHotspot> hotspot : hotspots.entrySet()) {
            JsonObject hotspotJson = new JsonObject();
            hotspotJson.put("id", hotspot.getValue().getId());
            hotspotJson.put("name", hotspot.getValue().getName());
            hotspotJson.put("enabled", false);
            hotspotJson.put("accountId", hotspot.getValue().getAccountId());
            hotspotJson.put("nft", hotspot.getValue().getNft());
            if (hotspot.getValue().getPaidAccounts().size() == 0) {
                JsonArray paidAccounts = new JsonArray();
                paidAccounts.add(hotspot.getValue().getAccountId());
                hotspotJson.put("paidAccounts", paidAccounts);
            } else {
                hotspotJson.put("paidAccounts", hotspot.getValue().getPaidAccounts());
            }
            if ( threadMap.containsKey(hotspot.getValue().getId())) {
                Hotspot hotspotInstance = threadMap.get(hotspot.getValue().getId());
                hotspotJson.put("runData", hotspotInstance.getDetails());
                hotspotJson.put("enabled", true);
            }
            details.add(hotspotJson);
        }

        response.put("hotspots", details);
        return response;
    }
}
