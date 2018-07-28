package com.gmail.woodyc40.savedposts;

import com.google.gson.JsonObject;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MediaDownloader {
    private static final Set<String> MEDIA_EXT =
            new HashSet<>(Arrays.asList(".jpg", ".jpeg", ".png", ".gif", ".mp4"));
    private static final String IMGUR_BASE = "https://imgur.com/a";
    private static final String IMGUR2_BASE = "https://imgur.com/download";
    private static final String GFYCAT_BASE = "https://gfycat.com/cajax/get";

    private MediaDownloader() {
    }

    public static boolean isMedia(String url) {
        int extDot = url.lastIndexOf('.');
        if (extDot == -1) {
            return false;
        }

        return MEDIA_EXT.contains(url.substring(extDot));
    }

    public static void imgur(Path folder, String fileName, String url) throws IOException {
        String urlTranform = IMGUR_BASE + url.substring(url.lastIndexOf('/')) + "/zip";

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(urlTranform)
                .build();
        try (Response zipResp = client.newCall(request).execute()) {
            if (!zipResp.isSuccessful()) {
                imgur2(folder, fileName, url);
                return;
            }

            imgurDl(folder, fileName, zipResp);
        }
    }

    // Try again just in case imgur is being ridiculous
    // i.e. gifv links and edge cases (?)
    private static void imgur2(Path folder, String fileName, String url) throws IOException {
        String urlTranform = IMGUR2_BASE + url.replace(".gifv", "").substring(url.lastIndexOf('/'));

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(urlTranform)
                .build();
        try (Response zipResp = client.newCall(request).execute()) {
            if (!zipResp.isSuccessful()) {
                System.out.println("Failed to retrieve image " + url);
                return;
            }

            String type = zipResp.header("Content-Type");
            if (type == null) {
                throw new RuntimeException("Null content type");
            }

            imgurDl(folder, fileName, zipResp);
        }
    }

    private static void imgurDl(Path folder, String fileName, Response zipResp) throws IOException {
        String type = zipResp.header("Content-Type");
        if (type == null) {
            throw new RuntimeException("Null content type");
        }

        if (type.endsWith("x-zip")) { // album
            try (ZipInputStream zis = new ZipInputStream(zipResp.body().byteStream())) {
                ZipEntry zentry;
                int idx = 0;
                while ((zentry = zis.getNextEntry()) != null) {
                    String entryName = zentry.getName();
                    String unzippedName = fileName + "-" + idx + entryName.substring(entryName.lastIndexOf('.'));
                    Path filePath = verify(folder.resolve(unzippedName));
                    if (!zentry.isDirectory()) { // flatten
                        Main.copy(zis, filePath);
                    }
                    zis.closeEntry();
                    idx++;
                }
            }
        } else { // single image
            Path filePath = verify(folder.resolve(fileName + type.substring(type.lastIndexOf('/') + 1)));
            Main.copy(zipResp.body().byteStream(), filePath);
        }
    }

    public static void gfycat(Path folder, String fileName, String url) throws IOException {
        Path filePath = verify(folder.resolve(fileName + "mp4"));

        String urlTransform = GFYCAT_BASE + url.substring(url.lastIndexOf('/'));
        JsonObject links = Json.read(new URL(urlTransform));
        String actualUrl = links.getAsJsonObject("gfyItem").get("mp4Url").getAsString();
        download(filePath, actualUrl);
    }

    public static void vreddit(Path folder, String fileName, String url) throws IOException {
        Path filePath = verify(folder.resolve(fileName + "mp4"));
        download(filePath, url);
    }

    public static void download(Path out, String url) throws IOException {
        out = verify(out);

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .build();
        try (Response resp = client.newCall(request).execute()) {
            Main.copy(resp.body().byteStream(), out);
        }
    }

    private static Path verify(Path path) {
        while (Files.exists(path)) {
            path = path.getParent().resolve(path.getFileName().toString() + "-next");
        }

        return path;
    }
}
