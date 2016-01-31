package com.richardlucasapps.netlive;

import android.net.TrafficStats;

public class AppDataUsage {

    private String appName;
    private int uId;
    private long previousTotalData;

    public AppDataUsage(String appName, int uid) {
        this.appName = appName;
        this.uId = uid;
        this.previousTotalData = getTotalBytesTransferredSinceBoot();
    }

    public Long getTotalBytesTransferredSinceBoot() {
        return TrafficStats.getUidRxBytes(this.uId) + TrafficStats.getUidTxBytes(this.uId);
    }

    public Long getTransferRate() {
        long currentTotalData = getTotalBytesTransferredSinceBoot();
        long rate = currentTotalData - previousTotalData;
        previousTotalData = currentTotalData;
        return rate;
    }

    public String getAppName() {
        return appName;
    }

    @Override
    public String toString() {
        return String.valueOf(uId) + String.valueOf(previousTotalData);
    }
}
