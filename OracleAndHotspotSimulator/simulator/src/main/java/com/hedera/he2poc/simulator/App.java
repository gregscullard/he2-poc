package com.hedera.he2poc.simulator;

import com.hedera.he2poc.common.yamlconfig.YamlConfigManager;
import com.hedera.he2poc.hotspot.Hotspots;
import lombok.extern.log4j.Log4j2;

import java.util.Random;

@Log4j2
public class App {
    public static void main(String[] args) throws Exception {
        App app = new App();
        int numHotspots = 0;
        if (args.length > 0) {
            numHotspots = Integer.parseInt(args[0]);
        }
        log.warn("Starting simulator with {} hotspots", numHotspots);
        app.start(numHotspots);
    }

    public void start(int numHotspots) throws Exception {
        boolean demo = false;

        YamlConfigManager yamlConfigManager = new YamlConfigManager(demo);
        if (yamlConfigManager.getHotspots().isEmpty()) {
            log.error("No hotspot details found in config.yaml");
            return;
        }
        if (yamlConfigManager.getTopicId().isEmpty()) {
            log.error("No topic configuration found in config.yaml");
            return;
        }
        Hotspots hotspots = new Hotspots(demo);

        if (numHotspots == 0) {
            numHotspots = yamlConfigManager.getHotspots().size();
        }
        if (numHotspots > yamlConfigManager.getHotspots().size()) {
            log.warn("Number of hotspots {} exceeds pre-configured hotspot list size", numHotspots);
            numHotspots = yamlConfigManager.getHotspots().size();
            log.warn("Running with {} hotspots ", numHotspots);
        }
        for (int i=1; i <= numHotspots; i++) {
            hotspots.startHotspot(i);
            // random delay between 1 and 3 seconds
            Random rand = new Random();
            int upperbound = 3;
            int delay = rand.nextInt(upperbound) + 1;
            Thread.sleep(delay * 1000);
        }
    }
}