package com.gmail.woodyc40.savedposts.client;

import com.gmail.woodyc40.savedposts.Config;
import okhttp3.*;

import java.io.IOException;

public class TokenClient extends Client {
    private static final MediaType UNENCODED
            = MediaType.parse("application/x-www-form-urlencoded; charset=utf-8");

    public TokenClient(Config config) {
        super(Credentials.basic(config.getClientId(), config.getClientSecret()));
    }

    public String post(String url, String data) throws IOException {
        RequestBody body = RequestBody.create(UNENCODED, data);
        Request request = new Request.Builder()
                .header("Authorization", this.authString)
                .url(url)
                .post(body)
                .build();
        try (Response response = this.http.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return null;
            }

            return response.body().string();
        }
    }
}
