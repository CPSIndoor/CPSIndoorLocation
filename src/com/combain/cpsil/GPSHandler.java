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

import java.util.Locale;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;

public class GPSHandler implements LocationListener {
	
	private ILService mILS;
	private LocationManager mLM;
	private static Location mLatestLocation = null;

	private LocationListener mSingleUpdateListener = new LocationListener() {

		@Override
		public void onLocationChanged(Location loc) {
			if (Settings.DEBUG) System.out.println("onLocationChangedSingle");
			mLatestLocation = loc;
			if (mILS != null && mILS.mWifiHandler != null) mILS.mWifiHandler.scan();
		}

		@Override
		public void onProviderDisabled(String provider) {	}

		@Override
		public void onProviderEnabled(String provider) {	}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {	}
		
	};
	
	public GPSHandler(ILService s) {
		mILS = s;
		mLM = (LocationManager) s.getSystemService(Context.LOCATION_SERVICE);
		mLatestLocation = mLM.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		mLM.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, 0, 0, this);
		
		mLM.requestSingleUpdate(LocationManager.GPS_PROVIDER, mSingleUpdateListener, null);
		
		s.handler.postDelayed(new Runnable() {
			public void run() {
				mLM.removeUpdates(mSingleUpdateListener);
			}
		}, 30000);
		// For backup if first stop fails
		s.handler.postDelayed(new Runnable() {
			public void run() {
				mLM.removeUpdates(mSingleUpdateListener);
			}
		}, 40000);
		
	}
	
	public static Location getLastGPSLocation() {
		return mLatestLocation;
	}

	public String buildDataString() {
		Locale locale = null;
		String data = "";
		Location loc = getLastGPSLocation();
		if (loc != null) {
			long age = getAge(loc);
//			if (age > 3600) return ""; // Always send 
			data = "G," + String.format(locale, "%.5f", loc.getLatitude()) + ","
					+ String.format(locale, "%.5f", loc.getLongitude()) + ","
					+ formatInteger(loc.getAccuracy()) + "," + formatInteger(loc.getAltitude()) + ","
					+ formatInteger(loc.getSpeed()) + "," + formatInteger(loc.getBearing()) + ","
					+ formatInteger(age) + "," + Math.round(loc.getTime()/1000);
		}
		return data;
	}
	
	public static String formatInteger(double number) {
		if (number==0) return "";
		return ""+Math.round(number);
	}

	public static long getAge(Location loc) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return (SystemClock.elapsedRealtimeNanos() - loc.getElapsedRealtimeNanos()) / 1000000000;
        } else {
            return (System.currentTimeMillis() - loc.getTime()) / 1000;
        }
	}

	@Override
	public void onLocationChanged(Location loc) {
		if (Settings.DEBUG) System.out.println("onLocationChanged: "+(loc!=null?loc.getProvider():""));
		if (loc != null && LocationManager.GPS_PROVIDER.equals(loc.getProvider())) {
			mLatestLocation = loc;
		}
	}

	@Override
	public void onProviderDisabled(String arg0) {	}

	@Override
	public void onProviderEnabled(String arg0) {	}

	@Override
	public void onStatusChanged(String arg0, int arg1, Bundle arg2) {	}
	
}
