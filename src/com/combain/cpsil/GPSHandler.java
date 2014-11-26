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

import java.util.Locale;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.SystemClock;

public class GPSHandler {

	private LocationManager mLM;

	public GPSHandler(ILService s) {
		mLM = (LocationManager) s.getSystemService(Context.LOCATION_SERVICE);
	}

	public String buildDataString() {
		Locale locale = null;
		String data = "";
		Location loc = mLM.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		if (loc != null) {
			long age = getAge(loc);
			if (age > 3600) return "";
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
//	
//	public boolean isGPSValid() {
//		Location loc = mLM.getLastKnownLocation(LocationManager.GPS_PROVIDER);
//		if (loc == null || getAge(loc) > 120
//				|| (loc.getLatitude() == 0 && loc.getLongitude() == 0)
//				|| loc.getAccuracy() > 150 || loc.getSpeed() * 3.6 > 250
//				|| loc.getAltitude() < -420) {
//			return false;
//		}
//		return true;
//	}

	@SuppressLint("NewApi")
	public static long getAge(Location loc) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
			return (SystemClock.elapsedRealtimeNanos()-loc.getElapsedRealtimeNanos())/1000000000;
		} else {
			return (System.currentTimeMillis() - loc.getTime())/1000;
		}
	}
	
}
