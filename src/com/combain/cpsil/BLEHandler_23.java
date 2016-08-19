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

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

@TargetApi(23)
public class BLEHandler_23 extends BLEHandler {

    public static final String TAG = "BLEHandler_23";

    final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive (Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                if(intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)
                        == BluetoothAdapter.STATE_ON) {
                    start();
                    // } else if () {

                }
            }

        }

    };

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public class BluetoothBeacon {
        public BluetoothDevice mDevice;
        public ScanRecord mScanRecord;
        public String mMAC,mName,mType;
        public String mDataType = "";
        public String mUUID = "";
        public int mRssi, mMajor, mMinor;
        public int mTxPower = 0;
        public long mTimestamp;
        public double mLatitude, mLongitude;
        public int mAccuracy;

        @TargetApi(21)
        public BluetoothBeacon(ScanResult sr) {
            this(sr.getScanRecord(), sr.getDevice(), sr.getRssi(), sr.getTimestampNanos());
        }

        public BluetoothBeacon(BluetoothDevice device, int rssi) {
            this(null, device, rssi, SystemClock.elapsedRealtimeNanos());
        }

        public BluetoothBeacon(ScanRecord scanRecord, BluetoothDevice device, int rssi, long timestamp) {
            mScanRecord = scanRecord;
            mDevice = device;
            if (mDevice != null) {
                switch (device.getType()) {
                    default:
                    case BluetoothDevice.DEVICE_TYPE_CLASSIC:
                        mType = "C";
                        break;
                    case BluetoothDevice.DEVICE_TYPE_DUAL:
                        mType = "D";
                        break;
                    case BluetoothDevice.DEVICE_TYPE_LE:
                        mType = "L";
                        break;
                    case BluetoothDevice.DEVICE_TYPE_UNKNOWN:
                        mType = "U";
                        break;
                }
                mMAC = device.getAddress();
                mName = getName();
            }
            mRssi = rssi;
            mTimestamp = timestamp;
        }

        public long getAge() {
            return (SystemClock.elapsedRealtimeNanos()-mTimestamp)/1000000000;
        }

        public String getName() {
            String name = null;
            if (mDevice != null) {
                name = mDevice.getName();
            }
            if (name == null && mScanRecord != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                name = mScanRecord.getDeviceName();
            }
            return name;
        }

        public String buildData() {
            int age = Math.round(getAge());
            if (age < 0) age = 0;
            String data = "B," + mType + "," + mMAC + ",\"" + mName + "\"," + mRssi + "," + age;
            if (mDevice != null && mDevice.getBluetoothClass() != null) {
                BluetoothClass bc = mDevice.getBluetoothClass();
                data += "," + bc.getMajorDeviceClass() + "," + bc.getDeviceClass();
            } else {
                data += ",,";
            }
            data += "," + mUUID + "," + mMajor + "," + mMinor + "," + mDataType + "," + mTxPower;
            return data;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj != null && obj instanceof BluetoothBeacon) {
                BluetoothBeacon bb = (BluetoothBeacon) obj;
                String addr1 = this.mDevice.getAddress();
                String addr2 = bb.mDevice.getAddress();
                String name1 = this.mDevice.getName();
                String name2 = bb.mDevice.getName();
                if (addr1 != null && addr2!= null && addr1.equals(addr2)) {
                    if (name1 == null && name2 == null) return true;
                    return (name1 != null && name2!=null  && name1.equals(name2));
                }
            }
            return false;
        }
    }

    private static Map<String,BluetoothBeacon> beaconsFound = new HashMap<String,BluetoothBeacon>();
    Context mContext;
    BluetoothAdapter mBTAdapter;
    BluetoothLeScanner mBLEScanner;
    ScanCallback mScanCallback;

    public BLEHandler_23(Context context) {
        super(context);

        mContext = context;
        BluetoothManager btManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        mBTAdapter = btManager.getAdapter();
        start();
        context.registerReceiver(mReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
    }

    @Override
    public void start() {
        if (mBTAdapter == null) return;
        mBLEScanner = mBTAdapter.getBluetoothLeScanner();
        ScanSettings scanSettings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_OPPORTUNISTIC)
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                .setReportDelay(1000)
                .setNumOfMatches(ScanSettings.MATCH_NUM_MAX_ADVERTISEMENT)
                .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE).build();
        mScanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
                //Log.v(TAG, "SCANCALLBACK: "+callbackType);
                onNewScanResult(result);
            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                super.onBatchScanResults(results);
                //Log.v(TAG, "SCANCALLBACK BATCH: "+(results!=null?results.size():0));
                for (ScanResult sr : results) {
                    onNewScanResult(sr);
                }
            }

            @Override
            public void onScanFailed(int errorCode) {
                super.onScanFailed(errorCode);
                if (Settings.DEBUG) Log.v(TAG, "SCANCALLBACK ERROR: "+errorCode);
            }
        };
        if (mBLEScanner != null) {
            if (Settings.DEBUG) Log.v(TAG, "STARTED OPPORTUNISTIC SCAN");
            mBLEScanner.startScan(null, scanSettings, mScanCallback);
        }
    }

    @Override
    public String buildDataString() {
        cleanBLEList();
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        synchronized (beaconsFound) {
            Set<String> keys = beaconsFound.keySet();
            for (String key : keys) {
                BluetoothBeacon bb = beaconsFound.get(key);
                String bbStr = bb.buildData();
                if (bbStr.length() > 0) {
                    if (first) {
                        first = false;
                    } else {
                        sb.append(";");
                    }
                }
                sb.append(bbStr);
            }
            lastSubmittedBeacons = new HashMap<String,BluetoothBeacon>(beaconsFound);
        }
        return sb.toString();
    }

    @Override
    public void stop() {
        if (mBLEScanner != null) {
            try {
                mBLEScanner.stopScan(mScanCallback);
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
        }
        mContext.unregisterReceiver(mReceiver);
    }

    void onNewScanResult(ScanResult sr) {
        BluetoothBeacon newBB = new BluetoothBeacon(sr);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) parseScanRecordBytes(sr.getScanRecord().getBytes(), newBB);
        synchronized (beaconsFound) {
            beaconsFound.put(newBB.mMAC, newBB);
            //if (Settings.DEBUG) Log.v(TAG, "BLE: " + newBB.mMAC + " " + newBB.mName+" "+newBB.mUUID+" "+newBB.mMajor+" "+newBB.mMinor);
        }
    }

    private void parseScanRecordBytes(byte[] scanRecord, BluetoothBeacon bb) {
        //System.out.println("SCANRECORD: "+new String(scanRecord));
        try {
            int startByte = 2;
            boolean patternFound = false;
            while (startByte <= 5) {
                if (((int) scanRecord[startByte + 2] & 0xff) == 0x02 && //Identifies an iBeacon
                        ((int) scanRecord[startByte + 3] & 0xff) == 0x15) { //Identifies correct data length
                    patternFound = true;
                    break;
                }
                startByte++;
            }

            if (patternFound) {
                //Convert to hex String
                byte[] uuidBytes = new byte[16];
                System.arraycopy(scanRecord, startByte + 4, uuidBytes, 0, 16);
                String hexString = Utils.bytesToHex(uuidBytes);

                //Here is your UUID
                String uuid = hexString.substring(0, 8) + "-" +
                        hexString.substring(8, 12) + "-" +
                        hexString.substring(12, 16) + "-" +
                        hexString.substring(16, 20) + "-" +
                        hexString.substring(20, 32);

                //Here is your Major value
                int major = (scanRecord[startByte + 20] & 0xff) * 0x100 + (scanRecord[startByte + 21] & 0xff);

                //Here is your Minor value
                int minor = (scanRecord[startByte + 22] & 0xff) * 0x100 + (scanRecord[startByte + 23] & 0xff);

                Byte txPower = scanRecord[startByte + 24];

                //System.out.println("UUID: " + uuid + ", major: " + major + ", minor: " + minor + ", txPower: " + txPower);
                bb.mUUID = uuid;
                bb.mMajor = major;
                bb.mMinor = minor;
                bb.mDataType = "iBeacon";
                bb.mTxPower = txPower.intValue();
            }
        } catch (Exception e) {

        }
    }

    public static void cleanBLEList() {
        synchronized (beaconsFound) {
            int count = 0;
            for (int i = 0; i < beaconsFound.size(); i++) {
                BluetoothBeacon bb = beaconsFound.get(i);
                while (bb != null && bb.getAge() >= 30 && count < 1000) {
                    beaconsFound.remove(bb);
                    count++;
                    if (beaconsFound.size() >= i) break;
                    bb = beaconsFound.get(i);
                }
            }
            if (beaconsFound.size() > 100) beaconsFound.clear();
        }
    }

    private Map<String,BluetoothBeacon> lastSubmittedBeacons = new HashMap<String,BluetoothBeacon>();

    @Override
    public boolean hasMoreBLEData() {
        if (beaconsFound.size() == 0) return false;
        synchronized (beaconsFound) {
            if (lastSubmittedBeacons.size() == 0) {
                if (Settings.DEBUG) Log.v(TAG, "lastSubmittedBeacons is empty");
                return true;
            } else if (beaconsFound.size()/lastSubmittedBeacons.size() > 2) {
                if (Settings.DEBUG) Log.v(TAG, "beaconsFound size increased");
                return true;
            } else {
                int nbrNew = 0;
                Set<String> keys = beaconsFound.keySet();
                for (String key : keys) {
                    if (!lastSubmittedBeacons.containsKey(key)) {
                        nbrNew++;
                        if (nbrNew > 0.5*lastSubmittedBeacons.size()) {
                            return true;
                        }
                    }
                }
                if (Settings.DEBUG) Log.v(TAG, "BLE new: "+nbrNew+" prev: "+lastSubmittedBeacons.size());
            }
        }
        return false;
    }

}
