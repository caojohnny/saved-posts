package com.gmail.woodyc40.savedposts;

import com.google.gson.JsonObject;

import java.io.IOException;
import java.nio.file.Path;

public class Config {
    private final String clientId;
    private final String clientSecret;
    private final String redditUser;
    private final String redditPass;

    public Config(Path path) throws IOException {
        JsonObject root = Json.read(path);

        this.clientId = root.get("client-id").getAsString();
        this.clientSecret = root.get("client-secret").getAsString();
        this.redditUser = root.get("reddit-user").getAsString();
        this.redditPass = root.get("reddit-pass").getAsString();
    }

    public String getClientId() {
        return this.clientId;
    }

    public String getClientSecret() {
        return this.clientSecret;
    }

    public String getRedditUser() {
        return this.redditUser;
    }

    public String getRedditPass() {
        return this.redditPass;
    }
}
