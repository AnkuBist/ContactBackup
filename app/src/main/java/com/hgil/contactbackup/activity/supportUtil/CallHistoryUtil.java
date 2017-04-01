package com.hgil.contactbackup.activity.supportUtil;

import android.app.ProgressDialog;
import android.content.Context;
import android.database.Cursor;
import android.os.Handler;
import android.provider.CallLog;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.hgil.contactbackup.pojo.CallLogModel;
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

import static com.hgil.contactbackup.util.API.USERNAME;

/**
 * Created by mohan.giri on 21-03-2017.
 */

public class CallHistoryUtil {

    private Context mContext;

    public CallHistoryUtil(Context context) {
        this.mContext = context;
    }

    public void fetchCallLogs() {
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
        return arrayList;
    }

    /*retrofit call to upload call logs*/
    private void uploadCallHistory(String username, JsonArray call_log) {
         RetrofitService service = RetrofitUtil.retrofitClient();
        Call<defaultResponse> apiCall = service.uploadCallLog(username, call_log.toString());
        apiCall.enqueue(new Callback<defaultResponse>() {
            @Override
            public void onResponse(Call<defaultResponse> call, Response<defaultResponse> response) {
            }

            @Override
            public void onFailure(Call<defaultResponse> call, Throwable t) {
            }
        });
    }
}
