package com.gmail.woodyc40.savedposts;

import com.gmail.woodyc40.savedposts.client.RedditClient;
import com.gmail.woodyc40.savedposts.client.TokenClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {
        Path folder = Paths.get(System.getProperty("user.dir"));
        p("Using " + folder.toAbsolutePath() + " as the working dir");

        Path img = folder.resolve("img");
        if (!Files.exists(img)) {
            p("Image dir not found, creating one for you...");
            Files.createDirectories(img);
        }
        p("Found image directory");

        Path text = folder.resolve("text.txt");
        if (!Files.exists(text)) {
            p("Text file not found, creating one for you...");
            Files.createFile(text);
        }
        p("Found text file");

        Path configPath = Paths.get(System.getProperty("user.dir"), "config.json");
        if (!Files.exists(configPath)) {
            copy(Main.class.getResourceAsStream("/config.json"), configPath);
            p("Config file created, exiting for you to edit!");
            return;
        }
        p("Config file detected, parsing...");

        Config config = new Config(configPath);
        TokenClient tokenClient = new TokenClient(config);
        p("Parsed config and initialized client");

        p("Getting auth token...");
        String data = f("grant_type=password&username=%s&password=%s", config.getRedditUser(), config.getRedditPass());
        String tokenResp = tokenClient.post("https://www.reddit.com/api/v1/access_token", data);
        if (tokenResp == null) {
            p("Error occurred getting response from server");
            p("Exiting...");
            return;
        }

        p("Parsing auth token response...");
        JsonObject tokenRespObj = Json.read(tokenResp);
        JsonElement tokenElement = tokenRespObj.get("access_token");

        if (tokenElement == null) {
            p("An error occurred retrieving the token");
            p("This was the response: " + tokenResp);
            p("Exiting...");
            return;
        }

        RedditClient client = new RedditClient(tokenElement.getAsString());
        p("Auth token = " + tokenElement.getAsString());

        String savedUrl = f("https://oauth.reddit.com/user/%s/saved", config.getRedditUser());
        p("Fetching " + savedUrl + "...");
        String savedResp = client.get(savedUrl);

        if (savedResp == null) {
            p("Error getting response from server");
            p("Exiting...");
            return;
        }

        JsonObject savedParsed = Json.read(savedResp);
        JsonObject savedData = savedParsed.getAsJsonObject("data");
        JsonArray entries = savedData.getAsJsonArray("children");

        JsonArray textArray = new JsonArray();
        int parsed = 0;
        int media = 0;
        int comments = 0;

        ExecutorService service = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors());

        while (true) {
            for (JsonElement entry : entries) {
                JsonObject entryObj = entry.getAsJsonObject();
                JsonObject dataObj = entryObj.getAsJsonObject("data");
                String kind = entryObj.get("kind").getAsString();

                String permalink = dataObj.get("permalink").getAsString();
                if ("t1".equalsIgnoreCase(kind)) { // Comment - store to text
                    JsonObject comment = new JsonObject();
                    comment.add("url", dataObj.get("link_permalink"));
                    comment.add("body", dataObj.get("body"));

                    p("Downloading " + permalink);

                    textArray.add(comment);
                    comments++;
                } else if ("t3".equalsIgnoreCase(kind)) { // Post
                    String fileName = sanitize(permalink);
                    String url = dataObj.get("url").getAsString();
                    String strippedUrl = stripOpts(url);

                    p("Downloading " + permalink);

                    if (MediaDownloader.isMedia(strippedUrl)) {
                        service.submit(() -> {
                            MediaDownloader.download(img.resolve(fileName + url.substring(url.lastIndexOf('.') + 1)), url);
                            return null;
                        });
                    } else if (url.contains("imgur.com")) {
                        service.submit(() -> {
                            MediaDownloader.imgur(img, fileName, strippedUrl);
                            return null;
                        });
                    } else if (url.contains("gfycat.com")) {
                        service.submit(() -> {
                            MediaDownloader.gfycat(img, fileName, strippedUrl);
                            return null;
                        });
                    } else if (url.contains("v.redd.it")) {
                        JsonElement mediaObj = dataObj.get("media");

                        // for xposts, the media is embedded in the original
                        // post instead of the actual one grabbed
                        if (mediaObj.isJsonNull()) {
                            for (JsonElement xpost : dataObj.getAsJsonArray("crosspost_parent_list")) {
                                mediaObj = xpost.getAsJsonObject().get("media");

                                if (!mediaObj.isJsonNull()) {
                                    break;
                                }
                            }
                        }

                        JsonObject videoObj = mediaObj
                                .getAsJsonObject()
                                .getAsJsonObject("reddit_video");
                        String vidUrl = videoObj.get("fallback_url").getAsString();
                        service.submit(() -> {
                            MediaDownloader.vreddit(img, fileName, stripOpts(vidUrl));
                            return null;
                        });
                    } else {
                        JsonObject unknown = new JsonObject();
                        if (!url.contains(permalink)) { // usually the AskReddit posts
                            unknown.addProperty("reddit-url", permalink);
                        }
                        unknown.addProperty("media-url", url);

                        textArray.add(unknown);
                    }
                    media++;
                }

                parsed++;
            }

            JsonElement after = savedData.get("after");
            if (after.isJsonNull()) {
                break;
            }

            savedUrl = f("https://oauth.reddit.com/user/%s/saved/?count=%d&after=%s",
                    config.getRedditUser(), parsed, after.getAsString());
            p("Fetching " + savedUrl + "...");
            savedResp = client.get(savedUrl);

            if (savedResp == null) {
                p("Error getting response from server");
                p("Exiting...");
                return;
            }

            savedParsed = Json.read(savedResp);
            savedData = savedParsed.getAsJsonObject("data");
            entries = savedData.getAsJsonArray("children");
        }

        try (BufferedWriter writer =
                     Files.newBufferedWriter(text, StandardOpenOption.APPEND)) {
            Json.write(writer, textArray);
        }

        p("Awaiting for queued task completion...");
        service.shutdown();
        service.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);

        p(f("Saved %d total (%d) posts and (%d) comments", parsed, media, comments));
    }

    private static void p(String string) {
        System.out.println(string);
    }

    private static String f(String string, Object... objects) {
        return String.format(string, objects);
    }

    private static String sanitize(String string) {
        return string.replace('/', '.');
    }

    private static String stripOpts(String string) {
        int qidx = string.lastIndexOf('?');
        if (qidx == -1) {
            return string;
        }
        return string.substring(0, qidx);
    }

    public static void copy(InputStream in, Path out) throws IOException {
        ReadableByteChannel rbc = Channels.newChannel(in);
        FileOutputStream fos = new FileOutputStream(out.toFile());
        fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        fos.close();
        in.close();
    }
}
