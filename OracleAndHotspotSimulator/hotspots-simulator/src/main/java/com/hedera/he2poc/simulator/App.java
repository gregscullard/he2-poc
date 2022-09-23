package com.hedera.he2poc.simulator;

import com.hedera.he2poc.common.Secrets;
import com.hedera.he2poc.common.balancechecker.BalanceChecker;
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
        app.start(numHotspots);
    }

    public void start(int numHotspots) throws Exception {

        YamlConfigManager yamlConfigManager = new YamlConfigManager();
        Secrets secrets = new Secrets();
        if (yamlConfigManager.getHotspots().isEmpty()) {
            log.error("No hotspot details found in config.yaml");
            return;
        }
        if (yamlConfigManager.getTopicId().isEmpty()) {
            log.error("No topic configuration found in config.yaml");
            return;
        }
        Hotspots hotspots = new Hotspots();

        BalanceChecker balanceChecker = new BalanceChecker(yamlConfigManager.getHotspotAccountIds(), secrets.privateKey(), secrets.accountId(), secrets.network());
        Thread balanceCheckerThread = new Thread(balanceChecker);
        balanceCheckerThread.start();

        if (numHotspots == 0) {
            numHotspots = yamlConfigManager.getHotspots().size();
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