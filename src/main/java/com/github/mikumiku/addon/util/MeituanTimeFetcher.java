package com.github.mikumiku.addon.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class MeituanTimeFetcher {

    private static final String URL = "https://googlese.bbbbbbb.top/time";

    public static long getServerTimeSeconds() {
        HttpClient client = HttpClient.newHttpClient();
        int maxRetries = 3;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(URL))
                    .GET()
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36 Edg/142.0.0.0")
                    .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                    if (json.has("data")) {
                        long ms = json.get("data").getAsLong();
                        return ms ; // 转为秒级时间戳
                    }
                }
            } catch (Exception e) {
                // 打印重试信息
                System.err.println("请求失败，正在重试 (" + attempt + "/" + maxRetries + ")：" + e.getMessage());
                try {
                    Thread.sleep(500); // 简单等待 0.5s 再试
                } catch (InterruptedException ignored) {
                }
            }
        }

        throw new RuntimeException("请求美团时间失败，已重试3次");
    }


}
