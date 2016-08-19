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

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.telephony.CellIdentityCdma;
import android.telephony.CellIdentityGsm;
import android.telephony.CellIdentityLte;
import android.telephony.CellIdentityWcdma;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.CellLocation;
import android.telephony.CellSignalStrengthCdma;
import android.telephony.CellSignalStrengthGsm;
import android.telephony.CellSignalStrengthLte;
import android.telephony.CellSignalStrengthWcdma;
import android.telephony.NeighboringCellInfo;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;

public class CellHandler {

    private TelephonyManager mTM;

    public CellHandler(ILService s) {
        mTM = (TelephonyManager) s.getSystemService(Context.TELEPHONY_SERVICE);
    }

    public String buildDataString() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            List<CellInfo> cellInfoList = mTM.getAllCellInfo();
            if (cellInfoList != null && cellInfoList.size() > 0) {
                StringBuilder sb = new StringBuilder();
                boolean first = true;
                for (CellInfo cellInfo : cellInfoList) {
                    if (first) {
                        first = false;
                    } else {
                        sb.append(";");
                    }
                    sb.append((cellInfo.isRegistered() ? "S" : "N")).append(",").append(buildCellString(cellInfo, getRAT(mTM.getNetworkType())));
                }
                return sb.toString();
            }
        }
        return buildCellString_old();
    }

    @SuppressLint("NewApi")
    public static String buildCellString(android.telephony.CellInfo cellInfo, String rat) {
        if (cellInfo instanceof CellInfoGsm) {
            return getCellString((CellInfoGsm) cellInfo, rat);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 && cellInfo instanceof CellInfoWcdma) {
            return getCellString((CellInfoWcdma) cellInfo);
        } else if (cellInfo instanceof CellInfoCdma) {
            return getCellString((CellInfoCdma) cellInfo);
        } else if (cellInfo instanceof CellInfoLte) {
            return getCellString((CellInfoLte) cellInfo);
        }
        return "";
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public static String getCellString(CellInfoGsm cellInfoGsm, String rat) {
        CellIdentityGsm cigsm = cellInfoGsm.getCellIdentity();
        int dbm = getDbm(cellInfoGsm);
        int lac = cigsm.getLac();
        int cid = cigsm.getCid();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 || rat.equals("G") || cid <= 0xFFFF) {
            int mcc = cigsm.getMcc();
            int mnc = cigsm.getMnc();
            int ta = 2147483647;
            return "G," + (mcc != 2147483647 ? mcc : "") + "," + (mnc != 2147483647 ? mnc : "")
                    + "," + (lac != 2147483647 ? Integer.toHexString(lac) : "") + "," + (cid != 2147483647 ? Integer.toHexString(cid) : "") + "," + dbm + "," + (ta != 2147483647 ? ta : "");
        } else {
            // Prior to JELLY_BEAN_MR2 CellInfoGsm was also containing Wcdma cells.
            @SuppressWarnings("deprecation")
            int psc = cigsm.getPsc();
            int mcc = cigsm.getMcc();
            int mnc = cigsm.getMnc();
            return "W," + (mcc != 2147483647 ? mcc : "") + "," + (mnc != 2147483647 ? mnc : "")
                    + "," + (lac != 2147483647 ? Integer.toHexString(lac) : "") + "," + (cid != 2147483647 ? Integer.toHexString(cid) : "") + "," + dbm + "," + (psc != 2147483647 ? psc : "");
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public static String getCellString(CellInfoWcdma cellInfoWcdma) {
        CellIdentityWcdma ciwcdma = cellInfoWcdma.getCellIdentity();
        int dbm = getDbm(cellInfoWcdma);
        int lac = ciwcdma.getLac();
        int cid = ciwcdma.getCid();
        int psc = ciwcdma.getPsc();
        int mcc = ciwcdma.getMcc();
        int mnc = ciwcdma.getMnc();
        return "W," + (mcc != 2147483647 ? mcc : "") + "," + (mnc != 2147483647 ? mnc : "")
                + "," + (lac != 2147483647 ? Integer.toHexString(lac) : "") + "," + (cid != 2147483647 ? Integer.toHexString(cid) : "") + "," + dbm + "," + (psc != 2147483647 ? psc : "");
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public static String getCellString(CellInfoCdma cellInfoCdma) {
        CellIdentityCdma cicdma = cellInfoCdma.getCellIdentity();
        int dbm = getDbm(cellInfoCdma);
        return ",C," + cicdma.getSystemId() + "," + cicdma.getNetworkId() + "," + cicdma.getBasestationId() + "," + cicdma.getLatitude() + "," + cicdma.getLongitude() + "," + dbm + "\n";
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public static String getCellString(CellInfoLte cellInfoLte) {
        CellIdentityLte cilte = cellInfoLte.getCellIdentity();
        int dbm = getDbm(cellInfoLte);
        int lac = cilte.getTac();
        int cid = cilte.getCi();
        int mcc = cilte.getMcc();
        int mnc = cilte.getMnc();
        return "L," + (mcc != 2147483647 ? mcc : "") + "," + (mnc != 2147483647 ? mnc : "")
                + "," + (lac != 2147483647 ? Integer.toHexString(lac) : "") + "," + (cid != 2147483647 ? Integer.toHexString(cid) : "") + "," + dbm + "," + cilte.getPci();
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public static int getDbm(CellInfoGsm cellInfo) {
        CellSignalStrengthGsm ss = cellInfo.getCellSignalStrength();
        int dbm = ss.getDbm();
        if (dbm > 0) {
            int asu = ss.getAsuLevel();
            if (asu >= 0 && asu <= 97) dbm = asu - 140;
        }
        return (dbm != 2147483647 ? dbm : 0);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public static int getDbm(CellInfoWcdma cellInfo) {
        CellSignalStrengthWcdma ss = cellInfo.getCellSignalStrength();
        int dbm = ss.getDbm();
        if (dbm > 0) {
            int asu = ss.getAsuLevel();
            if (asu >= 0 && asu <= 97) dbm = asu - 140;
        }
        return dbm;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public static int getDbm(CellInfoCdma cellInfo) {
        CellSignalStrengthCdma ss = cellInfo.getCellSignalStrength();
        int dbm = ss.getDbm();
        if (dbm > 0) {
            int asu = ss.getAsuLevel();
            if (asu >= 0 && asu <= 97) dbm = asu - 140;
        }
        return dbm;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public static int getDbm(CellInfoLte cellInfo) {
        CellSignalStrengthLte ss = cellInfo.getCellSignalStrength();
        int dbm = ss.getDbm();
        if (dbm > -30 || dbm < -140) {
            int asu = ss.getAsuLevel();
            if (asu >= 0 && asu <= 97) dbm = asu - 140;
        }
        return dbm;
    }

    private static String getRAT(int type) {
        String rat = "";
        switch (type) {
            case TelephonyManager.NETWORK_TYPE_GPRS:
            case TelephonyManager.NETWORK_TYPE_EDGE:
                rat = "G";
                break;
            case TelephonyManager.NETWORK_TYPE_UMTS:
            case TelephonyManager.NETWORK_TYPE_HSPA:
            case TelephonyManager.NETWORK_TYPE_HSDPA:
            case TelephonyManager.NETWORK_TYPE_HSUPA:
            case TelephonyManager.NETWORK_TYPE_HSPAP:
                rat = "W";
                break;
            case TelephonyManager.NETWORK_TYPE_LTE:
                rat = "L";
                break;
            default:
                rat = "U";
                break;
        }
        return rat;
    }

    private String buildCellString_old() {
        TelephonyManager tm = mTM;
        if (tm != null) {
            StringBuilder sb = new StringBuilder();
            String rat = getRAT(tm.getNetworkType());
            String op = tm.getNetworkOperator();
            int mcc = -1;
            int mnc = -1;
            if (op != null && op.length() == 5) {
                mcc = Integer.parseInt(op.substring(0, 3));
                mnc = Integer.parseInt(op.substring(3));
            }

            List<NeighboringCellInfo> neighbors = tm
                    .getNeighboringCellInfo();
            CellLocation cl = tm.getCellLocation();
            if (cl != null) {
                sb.append("S,").append(buildServingCellData(rat, mcc, mnc, cl));
            }
            for (NeighboringCellInfo neighbor : neighbors) {
                sb.append(";").append("N,").append(buildData(mcc, mnc, neighbor));
            }
            return sb.toString();
        }
        return "";
    }

    private static String buildServingCellData(String rat, int mcc, int mnc, CellLocation cellLocation) {
        if (cellLocation instanceof GsmCellLocation) {
            GsmCellLocation gsmCellLocation = (GsmCellLocation) cellLocation;
            if (gsmCellLocation.getCid() != -1
                    && gsmCellLocation.getLac() != -1) {
                String str = rat + "," + mcc
                        + "," + mnc
                        + "," + Integer.toHexString(gsmCellLocation.getLac())
                        + "," + Integer.toHexString(gsmCellLocation.getCid())
                        + ",";
                if (Build.VERSION.SDK_INT >= 9) {
                    str += "," + gsmCellLocation.getPsc();
                }
                return str;

            }
        } else if (cellLocation instanceof CdmaCellLocation) {
            CdmaCellLocation cdmaCellLocation = (CdmaCellLocation) cellLocation;
            if (cdmaCellLocation.getSystemId() != -1 && cdmaCellLocation.getNetworkId() != -1 && cdmaCellLocation.getBaseStationId() != -1) {
                return rat + "," + cdmaCellLocation.getSystemId()
                        + "," + cdmaCellLocation.getNetworkId()
                        + "," + cdmaCellLocation.getBaseStationId()
                        + "," + cdmaCellLocation.getBaseStationLatitude()
                        + "," + cdmaCellLocation.getBaseStationLongitude();
            }

        }
        return "";
    }

    private static String buildData(int mcc, int mnc, NeighboringCellInfo neighbour) {
        int type = neighbour.getNetworkType();
        int cid = neighbour.getCid();
        int lac = neighbour.getLac();
        int psc = neighbour.getPsc();
        int rssi = neighbour.getRssi();
        return getRAT(type) + "," + mcc + "," + mnc + "," + (lac != -1 ? lac : "") + "," + (cid != NeighboringCellInfo.UNKNOWN_CID ? cid : "") + "," + (psc != -1 ? psc : "") + "," + (rssi != NeighboringCellInfo.UNKNOWN_RSSI ? (2 * rssi - 113) : "");
    }

}
