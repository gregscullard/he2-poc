package com.hedera.he2poc.common.yamlconfig;

public class YamlOracle {
    private int epochSeconds = 10;
    private int minEpochReports = 2;
    private int minWitnessReports = 0;

    public int getEpochSeconds() {
        return epochSeconds;
    }

    public void setEpochSeconds(int epochSeconds) {
        this.epochSeconds = epochSeconds;
    }

    public int getMinEpochReports() {
        return minEpochReports;
    }

    public void setMinEpochReports(int minEpochReports) {
        this.minEpochReports = minEpochReports;
    }

    public int getMinWitnessReports() {
        return minWitnessReports;
    }

    public void setMinWitnessReports(int minWitnessReports) {
        this.minWitnessReports = minWitnessReports;
    }
}
