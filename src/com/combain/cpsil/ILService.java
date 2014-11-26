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

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;


public class ILService extends Service {
	
	public class ServiceBinder extends Binder {
		public ILService getService() {
			return ILService.this;
		}
	}
	
	static boolean isGpsActive = false;
	
	Submitter mSubmitter;
	GPSHandler mGPSHandler;
	CellHandler mCellHandler;
	WifiHandler mWifiHandler;
	SensorHandler mSensorHandler;
	
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
		mSensorHandler = new SensorHandler(this);
		if (Settings.DEBUG) System.out.println("ILService started");
		
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent != null && intent.hasExtra("enabled")) {
			isGpsActive = intent.getBooleanExtra("enabled", true);
		}
		return START_NOT_STICKY;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		mWifiHandler.stop();
		mSubmitter.stop();
		mSensorHandler.stop();
	}

	public void onWifiScanFinished() {
		
		boolean movingState = mSensorHandler.mIsInMovingState;
		if (movingState || mStaticSubmitCount < 3) {
			String data = mGPSHandler.buildDataString();
			if (data != null && data.length() > 0) {
				data+= ";C,"+Math.round(System.currentTimeMillis()/1000);
				String cell = mCellHandler.buildDataString();
				if (cell.length()>0) data += ";" + cell;
				String wifi = mWifiHandler.buildDataString();
				if (wifi.length()>0) data += ";" + wifi;
				if (Settings.DEBUG) Log.i("ILService","DATA: "+data);
			
				mSubmitter.addMeasurement(data);
				if (!movingState) mStaticSubmitCount++;
			}
		} else {
			if (Settings.DEBUG) Log.i("ILService", "Not in moving state, skipped position");
		}
	}
	
	public void resetStaticCounters() {
		mStaticSubmitCount = 0;
	}

	public static void gpsStatusChanged(boolean active) {
		if (Settings.DEBUG) Log.i("ILService", "gpsStatus: "+active);
		isGpsActive = active;
	}
	
	@Override
	public void onTrimMemory(int level) {
		if (Settings.DEBUG) System.out.println("TRIM MEMORY: "+level);
		if (mSubmitter != null) mSubmitter.onTrimMemory(level);
	}
	
}
