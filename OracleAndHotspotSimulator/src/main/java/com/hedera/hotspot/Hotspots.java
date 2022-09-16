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
    private final static NavigableMap<Integer, Hotspot> threadMap = new TreeMap<Integer, Hotspot>();
    private final YamlConfigManager yamlConfigManager;
    private final Secrets secrets;
    private final TopicId topicId;
    private final String network;
    private final List<YamlHotspot> hotspots;
    private int interval;
    private int maxId = 0;

    public Hotspots() throws FileNotFoundException {
        this.yamlConfigManager = new YamlConfigManager();
        this.secrets = new Secrets();
        this.network = secrets.network();
        this.topicId = TopicId.fromString(yamlConfigManager.getTopicId());
        this.hotspots = yamlConfigManager.getHotspots();
        this.interval = yamlConfigManager.getReportInterval();
        this.maxId = this.hotspots.size() - 1;
    }

    public String add(String name, String key) throws PrecheckStatusException, TimeoutException, ReceiptStatusException {
        Client client = Client.forName(secrets.network());
        client.setOperator(secrets.accountId(), secrets.privateKey());
        // create account
        PrivateKey privateKey = PrivateKey.fromString(key);
        TransactionResponse response = new AccountCreateTransaction()
                .setKey(privateKey)
                .setInitialBalance(new Hbar(5))
                .execute(client);
        TransactionReceipt receipt = response.getReceipt(client);

        TokenId tokenId = TokenId.fromString(yamlConfigManager.getTokenId());
        response = new TokenAssociateTransaction()
                .setTokenIds(List.of(tokenId))
                .setAccountId(receipt.accountId)
                .freezeWith(client)
                .sign(privateKey)
                .execute(client);
        response.getReceipt(client);

        client.close();
        // add hotspot
        YamlHotspot yamlHotspot = new YamlHotspot();
        yamlHotspot.setAccountId(receipt.accountId.toString());
        yamlHotspot.setPrivateKey(key);
        yamlHotspot.setId(this.maxId + 1);
        yamlHotspot.setName(name);

        this.hotspots.add(yamlHotspot);
        maxId += 1;

        return "success";
    }
    public String enable(int id) {
        if (! threadMap.containsKey(id)) {
            String result = startHotspot(id);
            return result;
        } else {
            return "invalid hotspot id";
        }
    }

    public String disable(int id) {
        if (threadMap.containsKey(id)) {
            threadMap.get(id).stop();
            threadMap.remove(id);
            return "success";
        } else {
            return "invalid hotspot id";
        }
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
        String name = hotspot.getName();
        List<String> paidAccounts = hotspot.getPaidAccounts();

        Hotspot hotspotInstance = new Hotspot(id, name, topicId, privateKey, accountId, network, paidAccounts, this);
        hotspotInstance.setIntervalMs(this.interval);
        Thread thread = new Thread(hotspotInstance);
        threadMap.put(id, hotspotInstance);
        thread.start();
        return result;
    }

    public int hotspotCount() {
        return this.threadMap.size();
    }

    public JsonObject getDetails() {
        JsonObject response = new JsonObject();
        JsonArray details = new JsonArray();
        for (YamlHotspot hotspot : yamlConfigManager.getHotspots()) {
            JsonObject hotspotJson = new JsonObject();
            hotspotJson.put("id", hotspot.getId());
            hotspotJson.put("name", hotspot.getName());
            hotspotJson.put("enabled", false);
            hotspotJson.put("accountId", hotspot.getAccountId());
            if (hotspot.getPaidAccounts().size() == 0) {
                JsonArray paidAccounts = new JsonArray();
                paidAccounts.add(hotspot.getAccountId());
                hotspotJson.put("paidAccounts", paidAccounts);
            } else {
                hotspotJson.put("paidAccounts", hotspot.getPaidAccounts());
            }
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
