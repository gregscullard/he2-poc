package com.hedera.oracle;

import java.util.TreeMap;

public class HotspotReports {
    private TreeMap<Long, HotspotReportsCounter> hotspotReports = new TreeMap<>();

    public TreeMap<Long, HotspotReportsCounter> getReports() {
        return hotspotReports;
    }
    public HotspotReportsCounter getReportByEpoch(long epoch, long epochStartSeconds) {
        if (hotspotReports.containsKey(epoch)) {
            return hotspotReports.get(epoch);
        } else {
            return new HotspotReportsCounter(epochStartSeconds);
        }
    }

    public void updateReportsCounter(long epoch, HotspotReportsCounter hotspotReportsCounter) {
        if (this.hotspotReports.size() == 51) {
            long epochToRemove = this.hotspotReports.firstKey();
            this.hotspotReports.remove(epochToRemove);
        }
        this.hotspotReports.put(epoch, hotspotReportsCounter);
    }
}
