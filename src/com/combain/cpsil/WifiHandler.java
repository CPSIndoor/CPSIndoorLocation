/*
 * Copyright (c) 2015, Combain Mobile AB
 * 
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.combain.cpsil;

import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.SystemClock;

public class WifiHandler extends BroadcastReceiver {

	private ILService mService;
	private WifiManager mWifiManager;
	
	public WifiHandler(ILService s) {
		mService = s;
		mWifiManager = (WifiManager) s.getSystemService(Context.WIFI_SERVICE);
		s.registerReceiver(this, new IntentFilter(
				WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
	}
	
	public void stop() {
		if (mService != null) {
			mService.unregisterReceiver(this);
		}
	}
	
	@Override
	public void onReceive(Context arg0, Intent arg1) {
		mService.onWifiScanFinished();
	}
	
	public void scan() {
		if (mWifiManager != null) mWifiManager.startScan();
	}
	
	public List<ScanResult> getScanResult() {
		return mWifiManager.getScanResults();
	}
	
	public boolean isStrongestWifiSimilarInLastScan(List<ScanResult> srs) {
		ScanResult strongest = null;
		for (ScanResult sr : srs) {
			if (strongest == null || sr.level > strongest.level) {
				strongest = sr;
			}
		}
		if (strongest != null && lastBuiltScanResult != null) {
			for (ScanResult sr : lastBuiltScanResult) {
				if (sr.BSSID.equals(strongest.BSSID) && sr.SSID.equals(strongest.SSID)) {
					if (Math.abs(strongest.level-sr.level) < Settings.requiredWifiLevelDiff) {
						return true;
					}
					break;
				}
			}
		}
		return false;
	}
	
	private static List<ScanResult> lastBuiltScanResult = null;
	public String buildDataString() {
		String data = "";
		List<ScanResult> srs = mWifiManager.getScanResults();
		lastBuiltScanResult = srs;
		if (srs != null && srs.size()>0) {
			for (ScanResult sr : srs) {
				if (sr!=null && sr.BSSID!=null) {
					data += (data.length()>0?";":"") + "W," + sr.BSSID.replace(":", "") + ",\"" + sr.SSID + "\"," + sr.level + "," + getAuth(sr.capabilities) + "," + sr.frequency;
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) data += "," + Math.round((SystemClock.elapsedRealtimeNanos()/1000-sr.timestamp)/1000000);
				}
			}
		}
		return data;
	}

	static String getAuth(String cap) {
		if (cap == null) {
			return "";
		}
		int len = cap.length();
		if (len < 3) {
			return "";
		}
		int end = cap.indexOf('-');
		if (end == -1) end = cap.indexOf("]");
		String auth;
		if (end != -1 && end < len)
			auth = cap.substring(1, end);
		else
			auth = cap.substring(1, len - 1);
		if (auth.equals("ESS")) return "";
		return auth;
	}
	
}
