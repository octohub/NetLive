package com.richardlucasapps.netlive.gauge;

import android.net.TrafficStats;

class AppDataUsage {

  private final String appName;
  private final int uid;
  private long previousTotalData;

  public AppDataUsage(String appName, int uid) {
    this.appName = appName;
    this.uid = uid;
    this.previousTotalData = getTotalBytesTransferredSinceBoot();
  }

  private Long getTotalBytesTransferredSinceBoot() {
    return TrafficStats.getUidRxBytes(this.uid) + TrafficStats.getUidTxBytes(this.uid);
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

  @Override public String toString() {
    return String.valueOf(uid) + String.valueOf(previousTotalData);
  }
}
