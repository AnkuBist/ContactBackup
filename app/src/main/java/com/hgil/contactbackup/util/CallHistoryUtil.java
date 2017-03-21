package com.hgil.contactbackup.util;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Handler;
import android.provider.CallLog;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.hgil.contactbackup.pojo.CallLogModel;
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

public class CallHistoryUtil {

    public static final int READ_CALL_LOG = 102;

    private Context mContext;
    private ProgressDialog pDialog;
    private Handler updateBarHandler;

    public CallHistoryUtil(Context context) {
        this.mContext = context;
    }

    // simple trick to check and ask permission
    public void checkAndroidVersionForCallHistory() {
        if (Build.VERSION.SDK_INT >= 23) {
            int result_READ_CALL_LOG = ContextCompat.checkSelfPermission(mContext, Manifest.permission.READ_CALL_LOG);
            if (result_READ_CALL_LOG != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions((Activity) mContext, new String[]{Manifest.permission.READ_CALL_LOG}, READ_CALL_LOG);
                return;
            } else {
                fetchCallLogs();
            }
        } else {
            fetchCallLogs();
        }
    }

    public void fetchCallLogs() {
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
                JsonArray myCustomArray = gson.toJsonTree(getCallDetails(mContext)).getAsJsonArray();
                Log.e("TAG", "backupCallHistory: " + (myCustomArray.toString()));
                uploadCallHistory(USERNAME, myCustomArray);
            }
        }).start();
    }

    /*read phone call log history and upate to server*/
    private ArrayList<CallLogModel> getCallDetails(Context context) {
        ArrayList<CallLogModel> arrayList = new ArrayList<>();

        //StringBuffer stringBuffer = new StringBuffer();
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
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
    private void uploadCallHistory(String username, JsonArray call_log) {
        updateBarHandler.post(new Runnable() {
            public void run() {
                RetrofitUtil.showDialog(mContext);
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
