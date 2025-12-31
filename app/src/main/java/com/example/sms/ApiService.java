package com.example.sms;

import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Query;

public interface ApiService {

    @POST("api/token/")
    Call<Map<String, String>> login(@Body Map<String, String> credentials);

    @POST("api/devices/register/")
    Call<Map<String, Object>> registerDevice(@Header("Authorization") String token, @Body Map<String, String> deviceData);

    @POST("api/sms/sync/")
    Call<Map<String, Object>> syncSMS(@Body Map<String, Object> payload);

    @Multipart
    @POST("api/social/posts/")
    Call<Map<String, Object>> uploadPost(
            @Header("Authorization") String token,
            @Part MultipartBody.Part image,
            @Part("caption") RequestBody caption
    );

    @GET("api/social/posts/")
    Call<List<Map<String, Object>>> getPosts(@Header("Authorization") String token);

    @GET("api/social/chats/")
    Call<List<Map<String, Object>>> getChats(
            @Header("Authorization") String token,
            @Query("recipient") String recipientId
    );

    @POST("api/social/chats/")
    Call<Map<String, Object>> sendMessage(
            @Header("Authorization") String token,
            @Body Map<String, Object> messageData
    );
}
