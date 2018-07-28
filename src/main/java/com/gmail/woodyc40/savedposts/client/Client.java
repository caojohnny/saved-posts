package com.gmail.woodyc40.savedposts.client;

import okhttp3.OkHttpClient;

public class Client {
    protected final OkHttpClient http;
    protected final String authString;

    public Client(String authString) {
        this.http = new OkHttpClient();
        this.authString = authString;
    }
}
