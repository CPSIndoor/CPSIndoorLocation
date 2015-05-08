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

import android.os.Build;
import android.os.SystemClock;

/**
 * Class for handling environmental data e.g. temperature and pressure.
 */
public class EnvironmentalHandler {

    private static int pressure = 0;
    private static long pressureTimestamp = 0;

    /**
     * Use this function to set the current barometric pressure.
     * @param pres The pressure in hPa (millibar)
     */
    public static void setCurrentBarometricPressure(int pres) {
        pressure = pres;
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR1) {
            pressureTimestamp = SystemClock.elapsedRealtime();
        } else {
            pressureTimestamp = System.currentTimeMillis();
        }
    }

    /**
     * Returns a string with all environmental data.
     * @return String with environmental data
     */
    String getEnvironmentalString() {
        return getBarometerString();
    }

    /**
     * Called by service to get barometric data.
      * @return String with barometric data.
     */
    String getBarometerString() {

        if (pressure > 0 && pressureTimestamp > 0) {

            long age;
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR1) {
                age = Math.round((SystemClock.elapsedRealtime() - pressureTimestamp) / 1000);
            } else {
                age = Math.round((System.currentTimeMillis()-pressureTimestamp)/1000);
            }
            return "BA,"+pressure+","+age;

        } else {

            return "";

        }
    }

}
