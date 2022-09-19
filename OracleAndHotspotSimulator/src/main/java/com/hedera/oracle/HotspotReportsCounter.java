package com.hedera.oracle;

public class HotspotReportsCounter {
    private int beaconCount = 0;
    private int witnessCount = 0;
    private final long epochStartSeconds;
    private long rewardPaid = 0;
    private long partialRewardPaid = 0;

    public HotspotReportsCounter(long epochStartSeconds) {
        this.epochStartSeconds = epochStartSeconds;
    }

    public long getEpochStartSeconds() {
        return epochStartSeconds;
    }

    public long getRewardPaid() {
        return rewardPaid;
    }

    public long getPartialRewardPaid() {return partialRewardPaid;}

    public void setRewardPaid(long rewardPaid) {
        this.rewardPaid = rewardPaid;
    }

    public void setPartialRewardPaid(long partialRewardPaid) {
        this.partialRewardPaid = partialRewardPaid;
    }

    public int getBeaconCount() {
        return beaconCount;
    }

    public int getWitnessCount() {
        return witnessCount;
    }
    public void addWitnessCount() {
        this.witnessCount += 1;
    }
    public void addBeaconCount() {
        this.beaconCount += 1;
    }
}
