/*
 * Copyright (c) 2014, Combain Mobile AB
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
		// TODO Auto-generated method stub
		mService.onWifiScanFinished();
	}
	
	private List<ScanResult> mLastBuiltScanResult = null;
	public String buildDataString() {
		String data = "";
		List<ScanResult> srs = mWifiManager.getScanResults();
		mLastBuiltScanResult = srs;
		if (srs != null && srs.size()>0) {
			for (ScanResult sr : srs) {
				if (sr!=null && sr.BSSID!=null) data += (data.length()>0?";":"") + "W," + sr.BSSID.replace(":", "") + ",\"" + sr.SSID + "\"," + sr.level + "," + getAuth(sr.capabilities) + "," + sr.frequency;
			}
		}
		return data;
	}

	public boolean hasStrongestWifisChanged() {
		return !hasSameStrongestWifis(mLastBuiltScanResult, mWifiManager.getScanResults());
	}
	
	private boolean hasSameStrongestWifis(List<ScanResult> srs1, List<ScanResult> srs2) {
		if (srs1 == null || srs2 == null) return false;
		int max = Math.min(2, Math.min(srs1.size(), srs2.size()));
		for (int i = 0; i < max; i++) {
			if (!areSame(srs1.get(i), srs2.get(i))) return false;
		}
		return true;
	}
	
	private boolean areSame(ScanResult sr1, ScanResult sr2) {
		if (sr1 == null || sr2 == null) return false;
		if (sr1.BSSID == null || sr2.BSSID == null || !sr1.BSSID.equals(sr2.BSSID)) return false;
		if (sr1.SSID == null || sr2.SSID == null || !sr1.SSID.equals(sr2.SSID)) return false;
		return true;
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
