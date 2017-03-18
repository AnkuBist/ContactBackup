package com.hgil.contactbackup.activity;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.hgil.contactbackup.R;
import com.hgil.contactbackup.pojo.CallLogModel;
import com.hgil.contactbackup.pojo.ContactModel;
import com.hgil.contactbackup.pojo.MessageModel;
import com.hgil.contactbackup.retrofit.RetrofitService;
import com.hgil.contactbackup.retrofit.RetrofitUtil;
import com.hgil.contactbackup.retrofit.response.defaultResponse;
import com.hgil.contactbackup.util.Utility;
import com.hgil.contactbackup.util.ui.SampleDialog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    // button onclick listener
    public void backupContact(View view) {
        int MyVersion = Build.VERSION.SDK_INT;
        if (MyVersion > Build.VERSION_CODES.LOLLIPOP_MR1) {
            if (!checkIfAlreadyHavePermission()) {
                requestForSpecificPermission();
            } else {
                // fetch contacts and call log history
                fetchContacts();
            }
        }
        // for pre lollipop devices run this directly
        else {
            // you can directly pick contacts and call history
            fetchContacts();
        }
    }

    private void fetchContacts() {
        pDialog = new ProgressDialog(this);
        pDialog.setMessage("Reading contacts...");
        pDialog.setCancelable(false);
        pDialog.show();

        updateBarHandler = new Handler();

        // Since reading contacts takes more time, let's run it on a separate thread.
        new Thread(new Runnable() {
            @Override
            public void run() {
                ArrayList<ContactModel> arrList = getContacts();
                Gson gson = new GsonBuilder().create();
                JsonArray myCustomArray = gson.toJsonTree(arrList).getAsJsonArray();
                Log.e("TAG", "backupContact: " + (myCustomArray.toString()));
                uploadContacts(username, myCustomArray);
            }
        }).start();
    }

    private String username = "ankush";

    // RETROFIT CALL TO SYNC CONTACT DATA TO SERVER
    public void uploadContacts(String username, JsonArray contacts) {
        updateBarHandler.post(new Runnable() {
            public void run() {
                RetrofitUtil.showDialog(MainActivity.this);
            }
        });

        RetrofitService service = RetrofitUtil.retrofitClient();
        Call<defaultResponse> apiCall = service.uploadContacts(username, contacts.toString());
        apiCall.enqueue(new Callback<defaultResponse>() {
            @Override
            public void onResponse(Call<defaultResponse> call, Response<defaultResponse> response) {
                updateBarHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        RetrofitUtil.hideDialog();
                    }
                }, 500);

                defaultResponse syncResult = response.body();

                // rest call to read data from api service
                if (syncResult.getReturnCode()) {
                    new SampleDialog("", syncResult.getStrMessage(), MainActivity.this);

                } else {

                    //RetrofitUtil.showToast(LoginActivity.this, loginResult.getStrMessage());
                    new SampleDialog("", syncResult.getStrMessage(), MainActivity.this);
                }
            }

            @Override
            public void onFailure(Call<defaultResponse> call, Throwable t) {
                updateBarHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        RetrofitUtil.hideDialog();
                    }
                }, 500);

                // show some error toast or message to display the api call issue
                //RetrofitUtil.showToast(LoginActivity.this, "Unable to access API");
                new SampleDialog("", "Unable to access API", MainActivity.this);
            }
        });
    }


    /*permission check to read contacts and call logs*/
    // check permission
    private boolean checkIfAlreadyHavePermission() {
        int result_READ_CONTACTS = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS);
        int result_READ_CALL_LOG = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG);
        if ((result_READ_CONTACTS == PackageManager.PERMISSION_GRANTED) && (result_READ_CALL_LOG == PackageManager.PERMISSION_GRANTED)) {
            return true;
        } else {
            return false;
        }
    }

    // request permission
    private void requestForSpecificPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS, Manifest.permission.READ_CALL_LOG}, 101);
    }

    // request permissions result
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    fetchContacts();
                } else {
                    Toast.makeText(this, "Permission denied to read phone contacts", Toast.LENGTH_SHORT).show();
                }
                return;
            }
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private ProgressDialog pDialog;
    private Handler updateBarHandler;
    int counter;
    Cursor cursor;

    /*read contact logs here*/
    public ArrayList<ContactModel> getContacts() {
        ArrayList<ContactModel> contactList = new ArrayList<>();
        String phoneNumber = "", email = "";
        Uri CONTENT_URI = ContactsContract.Contacts.CONTENT_URI;
        String _ID = ContactsContract.Contacts._ID;
        String DISPLAY_NAME = ContactsContract.Contacts.DISPLAY_NAME;
        String HAS_PHONE_NUMBER = ContactsContract.Contacts.HAS_PHONE_NUMBER;
        Uri PhoneCONTENT_URI = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
        String Phone_CONTACT_ID = ContactsContract.CommonDataKinds.Phone.CONTACT_ID;
        String NUMBER = ContactsContract.CommonDataKinds.Phone.NUMBER;
        Uri EmailCONTENT_URI = ContactsContract.CommonDataKinds.Email.CONTENT_URI;
        String EmailCONTACT_ID = ContactsContract.CommonDataKinds.Email.CONTACT_ID;
        String DATA = ContactsContract.CommonDataKinds.Email.DATA;
        //StringBuffer output;
        ContentResolver contentResolver = getContentResolver();
        cursor = contentResolver.query(CONTENT_URI, null, null, null, null);

        // Iterate every contact in the phone
        if (cursor.getCount() > 0) {
            counter = 0;
            while (cursor.moveToNext()) {
                ContactModel contactModel = new ContactModel();
                //output = new StringBuffer();
                // Update the progress message
                updateBarHandler.post(new Runnable() {
                    public void run() {
                        pDialog.setMessage("Reading contacts : " + counter++ + "/" + cursor.getCount());
                    }
                });
                String contact_id = cursor.getString(cursor.getColumnIndex(_ID));
                String name = cursor.getString(cursor.getColumnIndex(DISPLAY_NAME));
                int hasPhoneNumber = Integer.parseInt(cursor.getString(cursor.getColumnIndex(HAS_PHONE_NUMBER)));
                contactModel.setContactName(name);

                ArrayList<String> arrNumb = new ArrayList<>();
                if (hasPhoneNumber > 0) {
                    //output.append("\n First Name:" + name);
                    //This is to read multiple phone numbers associated with the same contact
                    Cursor phoneCursor = contentResolver.query(PhoneCONTENT_URI, null, Phone_CONTACT_ID + " = ?", new String[]{contact_id}, null);
                    while (phoneCursor.moveToNext()) {
                        phoneNumber = phoneCursor.getString(phoneCursor.getColumnIndex(NUMBER));
                        //output.append("\n Phone number:" + phoneNumber);
                        arrNumb.add(phoneNumber);
                    }
                    phoneCursor.close();
                }
                contactModel.setContactNumbers(arrNumb.toString());

                // Read every email id associated with the contact
                Cursor emailCursor = contentResolver.query(EmailCONTENT_URI, null, EmailCONTACT_ID + " = ?", new String[]{contact_id}, null);
                ArrayList<String> arrEmails = new ArrayList<>();
                if (emailCursor.getCount() > 0) {
                    while (emailCursor.moveToNext()) {
                        email = emailCursor.getString(emailCursor.getColumnIndex(DATA));
                        //output.append("\n Email:" + email);
                        arrEmails.add(email);
                    }
                    emailCursor.close();
                }
                contactModel.setEmail(arrEmails.toString());

                // Add the contact to the ArrayList
                contactList.add(contactModel);
            }
           /* // ListView has to be updated using a ui thread
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(), R.layout.list_item, R.id.text1, contactList);
                    mListView.setAdapter(adapter);
                }
            });*/
            // Dismiss the progressbar after 500 millisecondds
            updateBarHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    pDialog.cancel();
                }
            }, 500);
        }

        return contactList;
    }

    public void callHistory(View view) {
        int MyVersion = Build.VERSION.SDK_INT;
        if (MyVersion > Build.VERSION_CODES.LOLLIPOP_MR1) {
            if (!checkIfAlreadyHavePermission()) {
                requestForSpecificPermission();
            } else {
                // fetch contacts and call log history
                fetchCallLogs();
            }
        }
        // for pre lollipop devices run this directly
        else {
            // you can directly pick contacts and call history
            fetchCallLogs();
        }
    }

    private void fetchCallLogs() {
        pDialog = new ProgressDialog(this);
        pDialog.setMessage("Reading contacts...");
        pDialog.setCancelable(false);
        pDialog.show();

        updateBarHandler = new Handler();

        // Since reading contacts takes more time, let's run it on a separate thread.
        new Thread(new Runnable() {
            @Override
            public void run() {
                Gson gson = new GsonBuilder().create();
                JsonArray myCustomArray = gson.toJsonTree(getCallDetails(MainActivity.this)).getAsJsonArray();
                Log.e("TAG", "backupCallHistory: " + (myCustomArray.toString()));
                uploadCallHistory(username, myCustomArray);
            }
        }).start();
    }

    /*read phone call log history and upate to server*/
    private ArrayList<CallLogModel> getCallDetails(Context context) {
        ArrayList<CallLogModel> arrayList = new ArrayList<>();

        //StringBuffer stringBuffer = new StringBuffer();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            // no permission assigned return empty array in result
            return arrayList;
        }
        Cursor cursor = context.getContentResolver().query(CallLog.Calls.CONTENT_URI,
                null, null, null, CallLog.Calls.DATE + " DESC");
        int name = cursor.getColumnIndex(CallLog.Calls.CACHED_NAME);
        int number = cursor.getColumnIndex(CallLog.Calls.NUMBER);
        int type = cursor.getColumnIndex(CallLog.Calls.TYPE);
        int date = cursor.getColumnIndex(CallLog.Calls.DATE);
        int duration = cursor.getColumnIndex(CallLog.Calls.DURATION);
        while (cursor.moveToNext()) {
            CallLogModel callLogModel = new CallLogModel();

            String cName = cursor.getString(name);
            String phNumber = cursor.getString(number);
            String callType = cursor.getString(type);
            String callDate = cursor.getString(date);
            Date callDayTime = new Date(Long.valueOf(callDate));
            String callDuration = cursor.getString(duration);
            String dir = "";
            int dircode = Integer.parseInt(callType);
            switch (dircode) {
                case CallLog.Calls.OUTGOING_TYPE:
                    dir = "OUTGOING";
                    break;
                case CallLog.Calls.INCOMING_TYPE:
                    dir = "INCOMING";
                    break;
                case CallLog.Calls.MISSED_TYPE:
                    dir = "MISSED";
                    break;
            }

            if (cName != null)
                callLogModel.setcName(cName);
            else
                callLogModel.setcName("");

            callLogModel.setcNumber(phNumber);

            // get call date and time in different strings
            SimpleDateFormat dateFormatter = new SimpleDateFormat("dd-MM-yyyy");
            String cDate = dateFormatter.format(callDayTime);

            SimpleDateFormat timeFormatter = new SimpleDateFormat("hh:mm:ss_a");
            String cTime = timeFormatter.format(callDayTime);

            callLogModel.setcDate(cDate);
            callLogModel.setcTime(cTime);
            callLogModel.setcType(dir);
            callLogModel.setcDuration(Utility.timeDuration(callDuration));
            arrayList.add(callLogModel);
        }
        cursor.close();
        // Dismiss the progressbar after 500 milliseconds
        updateBarHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                pDialog.cancel();
            }
        }, 500);
        return arrayList;
    }


    /*retrofit call to upload call logs*/
    public void uploadCallHistory(String username, JsonArray call_log) {
        updateBarHandler.post(new Runnable() {
            public void run() {
                RetrofitUtil.showDialog(MainActivity.this);
            }
        });

        RetrofitService service = RetrofitUtil.retrofitClient();
        Call<defaultResponse> apiCall = service.uploadCallLog(username, call_log.toString());
        apiCall.enqueue(new Callback<defaultResponse>() {
            @Override
            public void onResponse(Call<defaultResponse> call, Response<defaultResponse> response) {
                updateBarHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        RetrofitUtil.hideDialog();
                    }
                }, 500);

                defaultResponse syncResult = response.body();

                // rest call to read data from api service
                if (syncResult.getReturnCode()) {
                    new SampleDialog("", syncResult.getStrMessage(), MainActivity.this);

                } else {

                    //RetrofitUtil.showToast(LoginActivity.this, loginResult.getStrMessage());
                    new SampleDialog("", syncResult.getStrMessage(), MainActivity.this);
                }
            }

            @Override
            public void onFailure(Call<defaultResponse> call, Throwable t) {
                updateBarHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        RetrofitUtil.hideDialog();
                    }
                }, 500);

                // show some error toast or message to display the api call issue
                //RetrofitUtil.showToast(LoginActivity.this, "Unable to access API");
                new SampleDialog("", "Unable to access API", MainActivity.this);
            }
        });
    }

    public void messageLog(View view) {
        // Since reading contacts takes more time, let's run it on a separate thread.
        new Thread(new Runnable() {
            @Override
            public void run() {
                Gson gson = new GsonBuilder().create();
                JsonArray myCustomArray = gson.toJsonTree(getAllSms(MainActivity.this)).getAsJsonArray();
                Log.e("TAG", "backupCallHistory: " + (myCustomArray.toString()));
                uploadCallHistory(username, myCustomArray);
            }
        }).start();
    }


    /*READ PHONE MESSAGES*/
    public ArrayList<MessageModel> getAllSms(Context context) {
        ArrayList<MessageModel> arrayList = new ArrayList<>();

        ContentResolver cr = context.getContentResolver();
        Cursor c = cr.query(Telephony.Sms.CONTENT_URI, null, null, null, null);
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
