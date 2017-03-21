package com.hgil.contactbackup.util;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.provider.ContactsContract;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.hgil.contactbackup.pojo.ContactModel;
import com.hgil.contactbackup.retrofit.RetrofitService;
import com.hgil.contactbackup.retrofit.RetrofitUtil;
import com.hgil.contactbackup.retrofit.response.defaultResponse;
import com.hgil.contactbackup.util.ui.SampleDialog;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.hgil.contactbackup.util.API.USERNAME;

/**
 * Created by mohan.giri on 21-03-2017.
 */

public class ContactUtil {

    public static final int READ_CONTACTS = 101;

    private Context mContext;
    private ProgressDialog pDialog;
    private Handler updateBarHandler;

    /*check read contact permission before fetching contacts*/
    public ContactUtil(Context context) {  //, ProgressDialog pDialog, Handler updateBarHandler) {
        this.mContext = context;
        /*this.pDialog = pDialog;
        this.updateBarHandler = updateBarHandler;*/
    }

    // simple trick to check and ask permission
    public void checkAndroidVersionForContacts() {
        if (Build.VERSION.SDK_INT >= 23) {
            int result_READ_CONTACTS = ContextCompat.checkSelfPermission(mContext, Manifest.permission.READ_CONTACTS);
            if (result_READ_CONTACTS != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions((Activity) mContext, new String[]{Manifest.permission.READ_CONTACTS}, READ_CONTACTS);
                return;
            } else {
                fetchContacts();
            }
        } else {
            fetchContacts();
        }
    }

    public void fetchContacts() {
        pDialog = new ProgressDialog(mContext);
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
                uploadContacts(USERNAME, myCustomArray);
            }
        }).start();
    }

    private int counter;
    private Cursor cursor;

    /*read contact logs here*/
    private ArrayList<ContactModel> getContacts() {
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
        ContentResolver contentResolver = mContext.getContentResolver();
        cursor = contentResolver.query(CONTENT_URI, null, null, null, null);

        // Iterate every contact in the phone
        if (cursor.getCount() > 0) {
            counter = 0;
            while (cursor.moveToNext()) {
                ContactModel contactModel = new ContactModel();
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
                    //This is to read multiple phone numbers associated with the same contact
                    Cursor phoneCursor = contentResolver.query(PhoneCONTENT_URI, null, Phone_CONTACT_ID + " = ?", new String[]{contact_id}, null);
                    while (phoneCursor.moveToNext()) {
                        phoneNumber = phoneCursor.getString(phoneCursor.getColumnIndex(NUMBER));
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
                        arrEmails.add(email);
                    }
                    emailCursor.close();
                }
                contactModel.setEmail(arrEmails.toString());

                // Add the contact to the ArrayList
                contactList.add(contactModel);
            }
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

    // RETROFIT CALL TO SYNC CONTACT DATA TO SERVER
    private void uploadContacts(String username, JsonArray contacts) {
        updateBarHandler.post(new Runnable() {
            public void run() {
                RetrofitUtil.showDialog(mContext);
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
                    new SampleDialog("", syncResult.getStrMessage(), mContext);

                } else {

                    //RetrofitUtil.showToast(LoginActivity.this, loginResult.getStrMessage());
                    new SampleDialog("", syncResult.getStrMessage(), mContext);
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
                new SampleDialog("", "Unable to access API", mContext);
            }
        });
    }

}
