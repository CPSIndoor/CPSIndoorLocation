/*
 * Copyright (c) 2016, Combain Mobile AB
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

import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.wifi.ScanResult;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.text.format.Time;
import android.util.Log;

public class ILService extends Service {

    public static final String TAG = "ILService";

	public class ServiceBinder extends Binder {
		public ILService getService() {
			return ILService.this;
		}
	}

	static boolean isGpsActive = false;

    public static String versionName = "";
    public static String versionCode = "";

	Submitter mSubmitter;
	GPSHandler mGPSHandler;
	CellHandler mCellHandler;
	WifiHandler mWifiHandler;
	BLEHandler mBLEHandler;
	EnvironmentalHandler mEnvironmentalHandler;
	final Handler handler = new Handler();

	int mStaticSubmitCount = 0;

	private final IBinder mBinder = new ServiceBinder();

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public void onCreate() {
		super.onCreate();

		mSubmitter = new Submitter(this);
		mGPSHandler = new GPSHandler(this);
		mCellHandler = new CellHandler(this);
		mWifiHandler = new WifiHandler(this);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			mBLEHandler = new BLEHandler_23(this);
		} else {
			mBLEHandler = new BLEHandler(this);
		}
		mEnvironmentalHandler = new EnvironmentalHandler(this);
		if (Settings.DEBUG)
			System.out.println("ILService started");

        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            versionName = pInfo.versionName;
            versionCode = pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {

        }
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent != null && intent.hasExtra("enabled")) {
			isGpsActive = intent.getBooleanExtra("enabled", true);
		}
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mWifiHandler.stop();
		mBLEHandler.stop();
		mSubmitter.stop();
	}

	private static Location lastSubmitLocation = null;

	private long lastGPSAge = 100000;
	public void onWifiScanFinished(List<ScanResult> srs) {

		if (Settings.DEBUG) Log.v(TAG, "onWifiScanFinished: "+(srs == null ? "null" : srs.size()));

		boolean shouldSend = false;
		Location loc = GPSHandler.getLastGPSLocation();

		if (loc != null && GPSHandler.getAge(loc) < 5) {

			if (lastSubmitLocation == null) {
				shouldSend = true;
			} else {
				float gpslimit = lastSubmitLocation.getAccuracy() + loc.getAccuracy();
				if (Settings.requiredGPSDistance >= 0) {
					gpslimit = Settings.requiredGPSDistance;
				}
				if (gpslimit == 0 || lastSubmitLocation.distanceTo(loc) > gpslimit) {
					shouldSend = true;
				} else {
					shouldSend = false;
					if (Settings.DEBUG) System.out.println("GPS has not moved enough!");
				}
			}

		} else {

			if (loc != null && GPSHandler.getAge(loc) < lastGPSAge) {

				if (srs != null && srs.size() > 0) shouldSend = true;

			} else {

                if (mBLEHandler.hasMoreBLEData()) {
                    if (Settings.DEBUG) Log.v(TAG, "BLE changes!");
                    shouldSend = true;
                } else if (mWifiHandler.isStrongestWifiSimilarInLastScan(srs)) {
					if (Settings.DEBUG) Log.v(TAG, "Strongest Wifi too similar!");
					shouldSend = false;
				} else {
                    if (Settings.DEBUG) Log.v(TAG, "Wifi has changed!");
					shouldSend = true;
				}

			}
		}

		if (shouldSend) handleNewMeasurement(loc, srs);
	}

	private void handleNewMeasurement(Location loc, List<ScanResult> srs) {

		if (Settings.DEBUG) Log.v(TAG, "handleNewMeasurement");

		lastGPSAge = (loc!=null?GPSHandler.getAge(loc):100000);

		lastSubmitLocation = loc;
		String time = "";
		try {
			Time t = new Time();
			t.setToNow();
			String dateStr = t.toString();
			if (dateStr != null) {
				String[] parts = dateStr.split(",");
				int ind = parts.length - 1;
				if (ind >= 0) {
					time = parts[ind];
					if (time.length() > 1)
						time = time.substring(0, time.length() - 1);
				}
			}
		} catch (Exception e) {
			time = ""+System.currentTimeMillis();
		}

		String gpsData = mGPSHandler.buildDataString();
		if (gpsData != null && gpsData.length() > 0) {
			StringBuilder sb = new StringBuilder(gpsData);
			sb.append(";C,").append(time);
			String cell = mCellHandler.buildDataString();
			if (cell.length() > 0)
				sb.append(";").append(cell);
			String wifi = mWifiHandler.buildDataString();
			if (wifi.length() > 0)
				sb.append(";").append(wifi);
			String ble = mBLEHandler.buildDataString();
			if (ble.length() > 0)
				sb.append(";").append(ble);
			String environmentalData = mEnvironmentalHandler.getEnvironmentalString();
			if (environmentalData != null && environmentalData.length() > 0) {
				sb.append(";").append(environmentalData);
			}
			if (Settings.DEBUG)
				Log.i(TAG, "DATA: " + sb.toString());

			mSubmitter.addMeasurement(sb.toString());
		} else {
			if (Settings.DEBUG) Log.v(TAG, "GPS DATA IS EMPTY");
		}

	}

	@Override
	public void onTrimMemory(int level) {
		if (Settings.DEBUG)
			System.out.println("TRIM MEMORY: " + level);
		if (mSubmitter != null)
			mSubmitter.onTrimMemory(level);
	}

}
