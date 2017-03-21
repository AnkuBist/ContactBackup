package com.hgil.contactbackup.activity;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.Telephony;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.hgil.contactbackup.R;
import com.hgil.contactbackup.pojo.MessageModel;
import com.hgil.contactbackup.util.CallHistoryUtil;
import com.hgil.contactbackup.util.ContactUtil;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import static com.hgil.contactbackup.util.CallHistoryUtil.READ_CALL_LOG;
import static com.hgil.contactbackup.util.ContactUtil.READ_CONTACTS;
import static com.hgil.contactbackup.util.ReadMessageUtil.READ_SMS;

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
                //new ReadMessageUtil(this).fetchCallLogs();
                //else
                Toast.makeText(this, "Permission denied to read call logs", Toast.LENGTH_SHORT).show();
                return;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    public void messageLog(View view) {
        // Since reading contacts takes more time, let's run it on a separate thread.
        new Thread(new Runnable() {
            @Override
            public void run() {
                Gson gson = new GsonBuilder().create();
                JsonArray myCustomArray = gson.toJsonTree(getAllSms(MainActivity.this)).getAsJsonArray();
                Log.e("TAG", "backupCallHistory: " + (myCustomArray.toString()));
                //uploadCallHistory(USERNAME, myCustomArray);
            }
        }).start();
    }


    /*READ PHONE MESSAGES*/
    public ArrayList<MessageModel> getAllSms(Context context) {
        ArrayList<MessageModel> arrayList = new ArrayList<>();

        ContentResolver cr = context.getContentResolver();
        Cursor c = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            c = cr.query(Telephony.Sms.CONTENT_URI, null, null, null, null);
        } else {
            return arrayList;
        }
        int totalSMS = 0;
        if (c != null) {
            totalSMS = c.getCount();
            if (c.moveToFirst()) {
                for (int j = 0; j < totalSMS; j++) {
                    MessageModel messageModel = new MessageModel();

                    String name = c.getString(c.getColumnIndex(Telephony.Sms.PERSON));
                    String messageDate = c.getString(c.getColumnIndexOrThrow(Telephony.Sms.DATE));
                    String number = c.getString(c.getColumnIndexOrThrow(Telephony.Sms.ADDRESS));
                    String body = c.getString(c.getColumnIndexOrThrow(Telephony.Sms.BODY));
                    Date dateFormat = new Date(Long.valueOf(messageDate));

                    // get call date and time in different strings
                    SimpleDateFormat dateFormatter = new SimpleDateFormat("dd-MM-yyyy");
                    String smsDate = dateFormatter.format(dateFormat);

                    SimpleDateFormat timeFormatter = new SimpleDateFormat("hh:mm:ss_a");
                    String smsTime = timeFormatter.format(dateFormat);

                    String type = "";
                    switch (Integer.parseInt(c.getString(c.getColumnIndexOrThrow(Telephony.Sms.TYPE)))) {
                        case Telephony.Sms.MESSAGE_TYPE_INBOX:
                            type = "inbox";
                            break;
                        case Telephony.Sms.MESSAGE_TYPE_SENT:
                            type = "sent";
                            break;
                        case Telephony.Sms.MESSAGE_TYPE_OUTBOX:
                            type = "outbox";
                            break;
                        default:
                            break;
                    }

                    /*update the message model*/
                    messageModel.setName(name);
                    messageModel.setNumber(number);
                    messageModel.setMessage(body);
                    messageModel.setSmsDate(smsDate);
                    messageModel.setSmsTime(smsTime);
                    messageModel.setSmsType(type);
                    arrayList.add(messageModel);
                    c.moveToNext();
                }
            }
        } else {
            Toast.makeText(this, "No message to show!", Toast.LENGTH_SHORT).show();
        }
        return arrayList;
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
