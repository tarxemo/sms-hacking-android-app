package com.example.sms;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface ApiService {

    @POST("api/token/")
    Call<Map<String, String>> login(@Body Map<String, String> credentials);

    @POST("api/devices/register/")
    Call<Map<String, Object>> registerDevice(@Header("Authorization") String token, @Body Map<String, String> deviceData);

    @POST("api/sms/sync/")
    Call<Map<String, Object>> syncSMS(@Body Map<String, Object> payload);

    @POST("api/social/posts/")
    Call<Map<String, Object>> uploadPost(@Header("Authorization") String token, @Body Map<String, Object> postData);

    @GET("api/social/posts/")
    Call<List<Map<String, Object>>> getPosts(@Header("Authorization") String token);

    @GET("api/social/chats/")
    Call<List<Map<String, Object>>> getChats(@Header("Authorization") String token);
}
