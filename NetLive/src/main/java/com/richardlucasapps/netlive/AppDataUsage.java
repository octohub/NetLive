package com.richardlucasapps.netlive;

import android.net.TrafficStats;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;

public class AppDataUsage {
	
	private String appName;
	private int uId;
	private long previousTotalData;

    private static final String uidStatPath = "/proc/uid_stat/";
    private static final String uidRcv = "tcp_rcv";
    private static final String uidSnd = "tcp_snd";


	public AppDataUsage(String appName1, int uid1) {
		this.appName = appName1;
		this.uId = uid1;
		this.previousTotalData = getStatsWithTrafficStatsAPI();

	}

    public int getUid(){
        return this.uId;
    }



   public Long getStatsWithTrafficStatsAPI(){
        return TrafficStats.getUidRxBytes(this.uId) + TrafficStats.getUidTxBytes(this.uId);

    }

    public Long getRateWithTrafficStatsAPI(){
		long currentTotalData = getStatsWithTrafficStatsAPI();
		long rate = currentTotalData - previousTotalData;
		previousTotalData = currentTotalData;
		return rate;
	}


	public String getAppName() {
		return appName;
	}

	@Override
	public String toString(){
		return String.valueOf(uId) + String.valueOf(previousTotalData);
		
	}
}
