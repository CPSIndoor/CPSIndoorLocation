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

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.SystemClock;

public class SensorHandler implements SensorEventListener {
	
	ILService mService;
	SensorManager mSM;
	Sensor mAcceleromter;
	
	public SensorHandler(ILService s) {
		mService = s;
		mSM = (SensorManager) s.getSystemService(Context.SENSOR_SERVICE);
		mAcceleromter = mSM.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		mSM.registerListener(this, mAcceleromter, SensorManager.SENSOR_DELAY_NORMAL);
		
	}
	
	public void stop() {
		mSM.unregisterListener(this);
	}

	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {
		// TODO Auto-generated method stub
		
	}
	
	private void setIsMoving(boolean moving) {
		mIsInMovingState = moving;
		if (moving && mService!=null) mService.resetStaticCounters();
	}

	public boolean mIsInMovingState = true;
	
	double mLastAcc = SensorManager.GRAVITY_EARTH; 
	long mStartTime = SystemClock.elapsedRealtime();
	int sampleCount = 0;
	double dAccSum = 0;
	@Override
	public void onSensorChanged(SensorEvent event) {
		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
			float[] acc = event.values.clone();
			double currentAcc = Math.sqrt(Math.pow(acc[0],2) + Math.pow(acc[1],2) + Math.pow(acc[2],2));
			double dAcc = Math.abs(currentAcc - mLastAcc);
			sampleCount++;
			dAccSum += dAcc;
			//if (ILService.DEBUG) Log.i("SensorHandler", "dAcc: "+dAcc);
			mLastAcc = currentAcc;
			long now = SystemClock.elapsedRealtime();
			if (now - mStartTime > 10000) {
				if (sampleCount > 0) {
					double meanDeltaAcc = dAccSum/sampleCount;
					//System.out.println("samples: "+sampleCount+" daccsum: "+meanDeltaAcc);
					setIsMoving(meanDeltaAcc > 0.05);
				}
				mStartTime = now;
				sampleCount = 0;
				dAccSum = 0;
			}
			
		}
		
	}

}
