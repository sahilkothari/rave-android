package com.flutterwave.raveandroid;

import android.annotation.SuppressLint;
import android.content.Context;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import javax.inject.Inject;

public class DeviceIdGetter {

    Context context;
    TelephonyManager mTelephonyManager;

    @Inject
    public DeviceIdGetter(Context context, TelephonyManager mTelephonyManager) {
        this.context = context;
    }

    @SuppressLint("MissingPermission")
    public String getDeviceImei() {
        String ip = mTelephonyManager.getDeviceId();

        if (ip == null) {
            ip = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        }

        return ip;

    }
}
