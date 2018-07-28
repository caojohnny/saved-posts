package com.gmail.woodyc40.savedposts.client;

import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

public class RedditClient extends Client {
    public RedditClient(String token) {
        super("bearer " + token);
    }

    public String get(String url) throws IOException {
        Request request = new Request.Builder()
                .header("Authorization", this.authString)
                .url(url)
                .build();

        try (Response response = this.http.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return null;
            }

            return response.body().string();
        }
    }
}
