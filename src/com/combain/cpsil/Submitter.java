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

import java.io.DataInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Vector;
import java.util.zip.GZIPOutputStream;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

public class Submitter extends PhoneStateListener {
	
	public class NetworkReceiver extends BroadcastReceiver {
		public void onReceive(Context c, Intent intent) {
			if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
				if (isConnectedToBTOrWifi()) {
					if (measurements.size()>=Settings.minPositionsToSend) sendData(3);
				}
			}
		}
	}

	public static final String TAG = "Submitter";

	static Vector<String> measurements = new Vector<String>();
	ILService mService;
	ConnectivityManager mCM;
	TelephonyManager mTM;
	ActivityManager mAM;
	NetworkReceiver mNR = new NetworkReceiver();
	
	public Submitter(ILService s) {
		mService = s;
		mCM = (ConnectivityManager) s.getSystemService(Context.CONNECTIVITY_SERVICE);
		mTM = (TelephonyManager) s.getSystemService(Context.TELEPHONY_SERVICE);
		if (Settings.useCellularNetwork) mTM.listen(this, PhoneStateListener.LISTEN_DATA_ACTIVITY);
		mAM = (ActivityManager) s.getSystemService(Context.ACTIVITY_SERVICE);
		s.registerReceiver(mNR, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
	}
	
	public void stop() {
		if (Settings.useCellularNetwork) mTM.listen(this, PhoneStateListener.LISTEN_NONE);
		if (mService != null) {
			mService.unregisterReceiver(mNR);
		}
	}
	
	public void addMeasurement(String str) {
		measurements.add(str);
        if (Settings.DEBUG) System.out.println("Queue Size: "+measurements.size());
		handleSendingToServer();
	}
	
	public void handleSendingToServer() {
		if (Settings.DEBUG) Log.i("Submitter", "Size: "+measurements.size() + " Min to send: " + Settings.minPositionsToSend);
		if (measurements.size()>=Settings.minPositionsToSend && isConnectedToBTOrWifi()) sendData(1);
	}
	
	public void onDataActivity(int direction) {
		if (Settings.useCellularNetwork && direction != TelephonyManager.DATA_ACTIVITY_NONE && direction != TelephonyManager.DATA_ACTIVITY_DORMANT) {
			if (measurements.size()>=Settings.minPositionsToSend && !mTM.isNetworkRoaming()) sendData(2);
		}
	}
	
	public void sendData(final int reason) {
		
		if (measurements.size() == 0) {
			return;
		}

        if (!Settings.useCellularNetwork && !isConnectedToBTOrWifi()) return;

		final Vector<String> measurementsInQueue = measurements;
		measurements = new Vector<String>();
		
		//System.out.println("Sending Data");
		new Thread() {
			public void run() {
				
				URL url;
				try {
					
					String data = Build.BRAND+","+Build.PRODUCT+","+Build.ID+","+reason+","+(ILService.isGpsActive?1:0)+","+ILService.versionName+","+ILService.versionCode;
					for (String measurement : measurementsInQueue) {
						data += "\n"+measurement;
					}
					
					String hmac = hmacSha1(data, Settings.privateKey);
					
					url = new URL("https://cpsil.combain.com/?key="+Settings.publicKey);
					HttpURLConnection hc = (HttpURLConnection) url.openConnection();
					hc.setRequestMethod("POST");
					hc.setDoInput(true);
					hc.setDoOutput(true);
					hc.setInstanceFollowRedirects(true);
					hc.setRequestProperty("Content-Type", "text/plain");
					hc.setRequestProperty("Content-HMAC", "sha256 "+hmac);
					hc.connect();
					
					GZIPOutputStream os = new GZIPOutputStream(hc.getOutputStream());
					os.write(data.getBytes("UTF-8"));
					
					os.finish();
					os.close();
					
					DataInputStream in;
					int responseCode = hc.getResponseCode();
					if (responseCode == 200) {
						in = new DataInputStream(hc.getInputStream());
					} else {
						in = new DataInputStream(hc.getErrorStream());
					}
					
					byte[] result = null;

					int chunkSize = 10240;
					int index = 0;
					int readLength = 0;
					result = new byte[chunkSize];
					while (true) {
						if (result.length < index + chunkSize) {
							byte[] newData = new byte[index + chunkSize];
							System.arraycopy(result, 0, newData, 0, result.length);
							result = newData;
						}
						readLength = in.read(result, index, chunkSize);
						if (readLength == -1)
							break;
						index += readLength;
					}

					int length = index;
					byte[] newData = new byte[length];
					System.arraycopy(result, 0, newData, 0, length);
					String response = new String(newData);
					
					if (Settings.DEBUG) {
						Log.i("Submitter", response);
					}
					
					if (response != null) {
						
						String[] responseParts = response.split(",");
						if (responseParts[0].equals("OK")) {
							
							if (responseParts.length > 1) {
								Settings.requiredGPSDistance = parseInt(responseParts[1]);
							}
							if (responseParts.length > 2) {
								Settings.requiredWifiLevelDiff = parseInt(responseParts[2]);
							}
							if (responseParts.length > 3) {
								Settings.minPositionsToSend = parseInt(responseParts[3]);
							}
						}
					}
					
					/*if (!ILService.isGpsActive && mService != null) {
						mService.stopSelf();
					}*/
					
				} catch (MalformedURLException e) {
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InvalidKeyException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (NoSuchAlgorithmException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}.start();
	}

	
	private boolean isConnectedToBTOrWifi() {
		NetworkInfo ni = mCM.getActiveNetworkInfo();
		if (ni != null && (ni.getType() == ConnectivityManager.TYPE_WIFI || ni.getType() == ConnectivityManager.TYPE_BLUETOOTH)) return true;
		return false;
	}
	
	public void onTrimMemory(int level) {
		int maxSize = -1;
		switch (level) {
		case ComponentCallbacks2.TRIM_MEMORY_MODERATE:
		case ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW:
			maxSize = 500;
			break;
		case ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL:
		case ComponentCallbacks2.TRIM_MEMORY_COMPLETE:
			maxSize = 100;
			break;
		}
		
		if (maxSize > 0) {
			while (measurements.size() > 500) {
				measurements.removeElementAt(0);
			}
		}
	}
	
	public static long parseLong(String s) {
		try {
			if (!isEmpty(s))
				return Long.parseLong(s);
		} catch (NumberFormatException e) {
		}
		return -1;
	}
	
	public static int parseInt(String s) {
		try {
			if (!isEmpty(s))
				return Integer.parseInt(s);
		} catch (NumberFormatException e) {
		}
		return -1;
	}
	
	static boolean isEmpty(String str) {
		if (str != null)
			return "".equals(str);
		else
			return true;
	}
	
	
	public static String hmacSha1(String value, String key)
	        throws UnsupportedEncodingException, NoSuchAlgorithmException,
	        InvalidKeyException {
	    String type = "HmacSHA256";
	    SecretKeySpec secret = new SecretKeySpec(key.getBytes(), type);
	    Mac mac = Mac.getInstance(type);
	    mac.init(secret);
	    byte[] bytes = mac.doFinal(value.getBytes());
	    return bytesToHex(bytes);
	}

	private final static char[] hexArray = "0123456789abcdef".toCharArray();

	private static String bytesToHex(byte[] bytes) {
	    char[] hexChars = new char[bytes.length * 2];
	    int v;
	    for (int j = 0; j < bytes.length; j++) {
	        v = bytes[j] & 0xFF;
	        hexChars[j * 2] = hexArray[v >>> 4];
	        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
	    }
	    return new String(hexChars);
	}
	
}
