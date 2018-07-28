package com.gmail.woodyc40.savedposts;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

public class Json {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .setLenient()
            .create();

    private Json() {
    }

    public static JsonObject read(URL url) throws IOException {
        return GSON.fromJson(new InputStreamReader(url.openStream()), JsonObject.class);
    }

    public static JsonObject read(Path path) throws IOException {
        return GSON.fromJson(new InputStreamReader(Files.newInputStream(path)), JsonObject.class);
    }

    public static JsonObject read(String string) {
        return GSON.fromJson(string, JsonObject.class);
    }

    public static void write(Writer writer, JsonElement element) throws IOException {
        writer.write(GSON.toJson(element));
    }
}
