package com.hedera.yamlconfig;

public class YamlOracle {
    private boolean enabled = false;
    private int epochSeconds = 10;
    private int minEpochReports = 2;
    private int minWitnessReports = 0;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

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
