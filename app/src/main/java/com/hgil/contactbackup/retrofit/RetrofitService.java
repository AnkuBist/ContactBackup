package com.hgil.contactbackup.retrofit;

import com.hgil.contactbackup.retrofit.response.defaultResponse;
import com.hgil.contactbackup.util.API;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

/**
 * Created by mohan.giri on 04-01-2017.
 */

public interface RetrofitService {
    @FormUrlEncoded
    @POST(API.UPLOAD_CONTACT_URL)
    Call<defaultResponse> uploadContacts(@Field("username") String username, @Field("contact_data") String contact_data);

    @FormUrlEncoded
    @POST(API.UPLOAD_CALL_HISTORY_URL)
    Call<defaultResponse> uploadCallLog(@Field("username") String username, @Field("call_log") String call_log);

    /*@FormUrlEncoded
    @POST(API.LOGIN_URL)
    Call<loginResponse> postUserLogin(@Field("route_id") String username, @Field("password") String password);*/


}
