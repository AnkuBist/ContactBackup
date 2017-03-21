package com.hgil.contactbackup.util;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

/**
 * Created by mohan.giri on 21-03-2017.
 */

public class ReadMessageUtil {

    public static final int READ_SMS = 103;

    private Context mContext;
    private ProgressDialog pDialog;
    private Handler updateBarHandler;

    public ReadMessageUtil(Context context) {
        this.mContext = context;
    }

    // simple trick to check and ask permission
    public void checkAndroidVersionForReadMessage() {
        if (Build.VERSION.SDK_INT >= 23) {
            int result_READ_SMS = ContextCompat.checkSelfPermission(mContext, Manifest.permission.READ_SMS);
            if (result_READ_SMS != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions((Activity) mContext, new String[]{Manifest.permission.READ_SMS}, READ_SMS);
                return;
            } else {
                //fetchCallLogs();
            }
        } else {
            //fetchCallLogs();
        }
    }
}
