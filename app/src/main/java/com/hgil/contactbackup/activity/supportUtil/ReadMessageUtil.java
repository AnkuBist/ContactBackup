package com.hgil.contactbackup.activity.supportUtil;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Handler;
import android.provider.Telephony;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.hgil.contactbackup.pojo.MessageModel;
import com.hgil.contactbackup.retrofit.RetrofitService;
import com.hgil.contactbackup.retrofit.RetrofitUtil;
import com.hgil.contactbackup.retrofit.response.defaultResponse;
import com.hgil.contactbackup.util.ui.SampleDialog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.hgil.contactbackup.util.API.USERNAME;

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
                fetchMessages();
            }
        } else {
            fetchMessages();
        }
    }

    public void fetchMessages() {
        pDialog = new ProgressDialog(mContext);
        pDialog.setMessage("Reading Call Logs...");
        pDialog.setCancelable(false);
        pDialog.show();

        updateBarHandler = new Handler();

        // Since reading contacts takes more time, let's run it on a separate thread.
        new Thread(new Runnable() {
            @Override
            public void run() {
                Gson gson = new GsonBuilder().create();
                JsonArray myCustomArray = gson.toJsonTree(getAllSms(mContext)).getAsJsonArray();
                Log.e("TAG", "backupCallHistory: " + (myCustomArray.toString()));
                uploadMessages(USERNAME, myCustomArray);
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
            // Dismiss the progressbar after 500 millisecondds
            updateBarHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    pDialog.cancel();
                }
            }, 500);

            // return empty messages array
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
                            type = "draft";
                            break;
                    }

                    /*update the message model*/
                    if (name != null)
                        messageModel.setName(name);
                    else
                        messageModel.setName("");
                    
                    if (number != null)
                        messageModel.setNumber(number);
                    else
                        messageModel.setNumber("");

                    messageModel.setMessage(body);
                    messageModel.setSmsDate(smsDate);
                    messageModel.setSmsTime(smsTime);
                    messageModel.setSmsType(type);
                    arrayList.add(messageModel);
                    c.moveToNext();
                }
            }
        } else {
            Toast.makeText(mContext, "No message to show!", Toast.LENGTH_SHORT).show();
        }

        // Dismiss the progressbar after 500 millisecondds
        updateBarHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                pDialog.cancel();
            }
        }, 500);

        return arrayList;
    }

    // RETROFIT CALL TO SYNC MESSAGES DATA TO SERVER
    private void uploadMessages(String username, JsonArray messages) {
        updateBarHandler.post(new Runnable() {
            public void run() {
                RetrofitUtil.showDialog(mContext);
            }
        });

        RetrofitService service = RetrofitUtil.retrofitClient();
        Call<defaultResponse> apiCall = service.uploadMessages(username, messages.toString());
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
