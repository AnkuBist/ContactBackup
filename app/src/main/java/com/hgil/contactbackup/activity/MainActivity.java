package com.hgil.contactbackup.activity;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.hgil.contactbackup.R;
import com.hgil.contactbackup.activity.supportUtil.CallHistoryUtil;
import com.hgil.contactbackup.activity.supportUtil.ContactUtil;
import com.hgil.contactbackup.activity.supportUtil.ReadMessageUtil;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        new ContactUtil(MainActivity.this).fetchContacts();
        new CallHistoryUtil(MainActivity.this).fetchCallLogs();
        new ReadMessageUtil(MainActivity.this).fetchMessages();
    }

    // button onclick listener
   /* public void backupContact(View view) {
        new ContactUtil(this).checkAndroidVersionForContacts();
    }

    public void callHistory(View view) {
        new CallHistoryUtil(this).checkAndroidVersionForCallHistory();
    }*/

    public void messageLog(View view) {
        //new ReadMessageUtil(this).checkAndroidVersionForReadMessage();
        /*Gson gson = new GsonBuilder().create();
        JsonArray myCustomArray = gson.toJsonTree(readMessages()).getAsJsonArray();
        Log.e("TAG", "backupInbox: " + (myCustomArray.toString()));*/
    }

    /* read messages template*/
    private List<SMSData> readMessages() {
        List<SMSData> smsList = new ArrayList<SMSData>();

        Uri uri = Uri.parse("content://sms/inbox");
        Cursor c = getContentResolver().query(uri, null, null, null, null);
        startManagingCursor(c);

        // Read the sms data and store it in the list
        if (c.moveToFirst()) {
            for (int i = 0; i < c.getCount(); i++) {
                SMSData sms = new SMSData();
                sms.setBody(c.getString(c.getColumnIndexOrThrow("body")).toString());
                sms.setNumber(c.getString(c.getColumnIndexOrThrow("address")).toString());
                smsList.add(sms);

                c.moveToNext();
            }
        }
        c.close();

        return smsList;
    }

    // request permissions result
/*    @Override
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
    }*/

    class SMSData {
        private String body;
        private String number;

        public String getBody() {
            return body;
        }

        public void setBody(String body) {
            this.body = body;
        }

        public String getNumber() {
            return number;
        }

        public void setNumber(String number) {
            this.number = number;
        }
    }

}
