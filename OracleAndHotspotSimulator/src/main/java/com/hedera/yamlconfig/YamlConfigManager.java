package com.hedera.yamlconfig;

import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hotspot.Hotspot;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class YamlConfigManager {
    public YamlConfig yamlConfig;
    private Map<Integer, YamlHotspot> hotspotMap = new HashMap<>();

    public YamlConfigManager() throws FileNotFoundException {
        if (Files.exists(Path.of("config.yaml"))) {
            Yaml yaml = new Yaml();
            InputStream inputStream = new FileInputStream(new File("config.yaml"));
            this.yamlConfig = yaml.load(inputStream);
            for (YamlHotspot yamlHotspot : yamlConfig.getHotspots()) {
                hotspotMap.put(yamlHotspot.getId(), yamlHotspot);
            }
        } else {
            this.yamlConfig = new YamlConfig();
        }
    }

    public void save() throws IOException {
        Yaml yaml = new Yaml();
        Writer writer = new FileWriter("config.yaml");
        yaml.dump(yamlConfig, writer);
        writer.close();
    }

    // Token
    public String getTokenId() {
        return this.yamlConfig.getToken().getTokenId();
    }

    public String getNftTokenId() {
        return this.yamlConfig.getToken().getNftTokenId();
    }
    public void setTokenId(String tokenId) {
        this.yamlConfig.getToken().setTokenId(tokenId);
    }

    public void setNftTokenId(String tokenId) {
        this.yamlConfig.getToken().setNftTokenId(tokenId);
    }

    public String getTreasuryAccount() {
        return this.yamlConfig.getToken().getTreasuryAccount();
    }

    public String getTreasuryAccountKey() {
        return this.yamlConfig.getToken().getTreasuryAccountKey();
    }

    public String getTopicId() {
        return this.yamlConfig.getTopicId();
    }

    // Hotspots
    public List<YamlHotspot> getHotspots() {
        return yamlConfig.getHotspots();
    }
    public Map<Integer, YamlHotspot> getHotspotsAsMap() {
        return this.hotspotMap;
    }

    // REST api
    public int getApiPort() {
        return this.yamlConfig.getApi().getApiPort();
    }

    public String getApiKey() {
        return this.yamlConfig.getApi().getApiKey();
    }

    public String getHttpsKeyOrPass() {
        return this.yamlConfig.getApi().getHttpsKeyOrPass();
    }

    public int getApiVerticleCount() {
        return this.yamlConfig.getApi().getApiVerticleCount();
    }

    public String getHttpsCertificate() {
        return this.yamlConfig.getApi().getHttpsCertificate();
    }

    public void setTreasuryAccount(String treasuryAccount) {
        this.yamlConfig.getToken().setTreasuryAccount(treasuryAccount);
    }

    public void setTreasuryAccountKey(String treasuryAccountKey) {
        this.yamlConfig.getToken().setTreasuryAccountKey(treasuryAccountKey);
    }

    public void setHotspots(List<YamlHotspot> hotspots) {
        this.yamlConfig.setHotspots(hotspots);
    }

    public void setTopicId(String topicId) {
        this.yamlConfig.setTopicId(topicId);
    }

    public List<AccountId> getHotspotAccountIds() {
        return this.yamlConfig.getHotspotAcountIds();
    }

    public int getHotSpotsToStart() {
        return this.yamlConfig.getHotspotsToStart();
    }

    public int getReportInterval() {
        return this.yamlConfig.getReportInterval();
    }

    public boolean isHotspotsSimulator() {
        return this.yamlConfig.isHotspotsSimulator();
    }

    public boolean isOracle() {
        return this.yamlConfig.getOracle().isEnabled();
    }

    public int getOracleEpochSeconds() {
        return this.yamlConfig.getOracle().getEpochSeconds();
    }

    public int getOracleMinEpochReports() {
        return this.yamlConfig.getOracle().getMinEpochReports();
    }

    public int getOracleMinWitnessReports() {
        return this.yamlConfig.getOracle().getMinWitnessReports();
    }

    // System
    public long getInitialHotspotBalance() {
        return this.yamlConfig.getSystem().getInitialHotspotBalance();
    }
}
