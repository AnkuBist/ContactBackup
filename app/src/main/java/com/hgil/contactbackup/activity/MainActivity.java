package com.hgil.contactbackup.activity;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.hgil.contactbackup.R;
import com.hgil.contactbackup.activity.supportUtil.CallHistoryUtil;
import com.hgil.contactbackup.activity.supportUtil.ContactUtil;
import com.hgil.contactbackup.activity.supportUtil.ReadMessageUtil;

import static com.hgil.contactbackup.activity.supportUtil.CallHistoryUtil.READ_CALL_LOG;
import static com.hgil.contactbackup.activity.supportUtil.ContactUtil.READ_CONTACTS;
import static com.hgil.contactbackup.activity.supportUtil.ReadMessageUtil.READ_SMS;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    // button onclick listener
    public void backupContact(View view) {
        new ContactUtil(this).checkAndroidVersionForContacts();
    }

    public void callHistory(View view) {
        new CallHistoryUtil(this).checkAndroidVersionForCallHistory();
    }

    public void messageLog(View view) {
        new ReadMessageUtil(this).checkAndroidVersionForReadMessage();
    }

    // request permissions result
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case READ_CONTACTS:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    new ContactUtil(this).fetchContacts();
                else
                    Toast.makeText(this, "Permission denied to read contacts", Toast.LENGTH_SHORT).show();
                return;
            case READ_CALL_LOG:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    new CallHistoryUtil(this).fetchCallLogs();
                else
                    Toast.makeText(this, "Permission denied to read call logs", Toast.LENGTH_SHORT).show();
                return;
            case READ_SMS:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    new ReadMessageUtil(this).fetchMessages();
                else
                    Toast.makeText(this, "Permission denied to read messages", Toast.LENGTH_SHORT).show();
                return;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    /*shortest method*/
    // request permissions result
    /*@Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case ACCESS_LOCATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    UtilNetworkLocation.fetchLocation(this);
                else
                    Toast.makeText(this, "Permission denied to get your location", Toast.LENGTH_SHORT).show();
                return;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }*/

    /*create utility class for the below methods*/

   /* public static final int ACCESS_LOCATION = 101;

    *//*check sms permission before sending sms*//*
    // simple trick to check and ask permission
    public static void checkAndroidVersion(Context context) {
        if (Build.VERSION.SDK_INT >= 23) {
            int result_COARSE_LOCATION = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION);
            int result_FINE_LOCATION = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION);
            if (result_COARSE_LOCATION != PackageManager.PERMISSION_GRANTED || result_FINE_LOCATION != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, ACCESS_LOCATION);
                return;
            } else {
                UtilNetworkLocation.fetchLocation(context);
            }
        } else {
            UtilNetworkLocation.fetchLocation(context);
        }
    }*/


}
