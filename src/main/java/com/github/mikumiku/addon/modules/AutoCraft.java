package com.github.mikumiku.addon.modules;

import com.github.mikumiku.addon.BaseModule;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.text.Text;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class AutoCraft extends BaseModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgSearch = settings.createGroup("搜索设置");
    private final SettingGroup sgResults = settings.createGroup("结果设置");

    // 通用设置
    private final Setting<Boolean> autoSearch = sgGeneral.add(new BoolSetting.Builder()
        .name("自动搜索")
        .description("启用时自动执行搜索")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> searchInterval = sgGeneral.add(new IntSetting.Builder()
        .name("搜索间隔")
        .description("自动搜索的间隔时间（秒）")
        .defaultValue(300)
        .min(60)
        .max(3600)
        .visible(() -> autoSearch.get())
        .build()
    );

    // 搜索设置
    private final Setting<String> searchKeyword = sgSearch.add(new StringSetting.Builder()
        .name("搜索关键词")
        .description("要搜索的关键词")
        .defaultValue("AutoCraft")
        .build()
    );

    private final Setting<Integer> minStars = sgSearch.add(new IntSetting.Builder()
        .name("最小星标数")
        .description("仓库最少需要的星标数")
        .defaultValue(10)
        .min(0)
        .max(10000)
        .build()
    );

    private final Setting<String> language = sgSearch.add(new StringSetting.Builder()
        .name("编程语言")
        .description("搜索特定编程语言的仓库（留空表示所有语言）")
        .defaultValue("Java")
        .build()
    );

    private final Setting<Boolean> includeForked = sgSearch.add(new BoolSetting.Builder()
        .name("包含分支仓库")
        .description("是否包含分支（Fork）仓库")
        .defaultValue(false)
        .build()
    );

    // 结果设置
    private final Setting<Integer> maxResults = sgResults.add(new IntSetting.Builder()
        .name("最大结果数")
        .description("最多显示的搜索结果数量")
        .defaultValue(10)
        .min(1)
        .max(100)
        .build()
    );

    private final Setting<Boolean> showDetails = sgResults.add(new BoolSetting.Builder()
        .name("显示详细信息")
        .description("显示仓库的详细信息（星标数、语言等）")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> copyToClipboard = sgResults.add(new BoolSetting.Builder()
        .name("复制到剪贴板")
        .description("将搜索结果复制到剪贴板")
        .defaultValue(false)
        .build()
    );

    private int tickCounter = 0;
    private final AtomicBoolean isSearching = new AtomicBoolean(false);
    private final AtomicInteger foundRepositories = new AtomicInteger(0);

    public AutoCraft() {
        super("AutoCraft仓库搜索", "搜索包含AutoCraft文件且星标数大于指定值的GitHub仓库");
    }

    @Override
    public void onActivate() {
        tickCounter = 0;
        foundRepositories.set(0);
        info("AutoCraft仓库搜索已启动");
        
        if (!autoSearch.get()) {
            // 手动启动时立即执行一次搜索
            executeSearch();
        }
    }

    @Override
    public void onDeactivate() {
        info("AutoCraft仓库搜索已停止");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!Utils.canUpdate()) return;

        tickCounter++;

        if (autoSearch.get()) {
            // 每秒20个tick，转换为搜索间隔
            int intervalTicks = searchInterval.get() * 20;
            if (tickCounter >= intervalTicks) {
                tickCounter = 0;
                if (!isSearching.get()) {
                    executeSearch();
                }
            }
        }
    }

    private void executeSearch() {
        if (isSearching.get()) {
            warning("搜索正在进行中，请稍候...");
            return;
        }

        CompletableFuture.runAsync(() -> {
            if (!isSearching.compareAndSet(false, true)) {
                return;
            }

            try {
                info("开始搜索包含 " + searchKeyword.get() + " 的仓库...");
                searchGitHubRepositories();
            } catch (Exception e) {
                error("搜索时发生错误: " + e.getMessage());
            } finally {
                isSearching.set(false);
            }
        });
    }

    private void searchGitHubRepositories() {
        try {
            StringBuilder queryBuilder = new StringBuilder();
            
            // 构建搜索查询
            queryBuilder.append(URLEncoder.encode(searchKeyword.get(), StandardCharsets.UTF_8));
            queryBuilder.append("+in:file");
            queryBuilder.append("+stars:>").append(minStars.get());

            if (!language.get().trim().isEmpty()) {
                queryBuilder.append("+language:").append(URLEncoder.encode(language.get(), StandardCharsets.UTF_8));
            }

            if (!includeForked.get()) {
                queryBuilder.append("+fork:false");
            }

            String apiUrl = "https://api.github.com/search/repositories?q=" + queryBuilder.toString() + 
                           "&sort=stars&order=desc&per_page=" + maxResults.get();

            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json");
            connection.setRequestProperty("User-Agent", "Meteor-AutoCraft-Search");

            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                
                parseAndDisplayResults(response.toString());
            } else {
                error("GitHub API 请求失败，响应码: " + responseCode);
                if (responseCode == 403) {
                    error("API 限制已达到，请稍后重试");
                }
            }
        } catch (IOException e) {
            error("网络请求失败: " + e.getMessage());
        }
    }

    private void parseAndDisplayResults(String jsonResponse) {
        try {
            // 简单的JSON解析（不使用外部库）
            int totalCount = extractValue(jsonResponse, "\"total_count\":", ",");
            info("找到 " + totalCount + " 个匹配的仓库");

            String itemsSection = jsonResponse.substring(jsonResponse.indexOf("\"items\":[") + 9);
            itemsSection = itemsSection.substring(0, itemsSection.lastIndexOf("]"));

            String[] repositories = itemsSection.split("\\},\\s*\\{");
            
            StringBuilder resultBuilder = new StringBuilder();
            resultBuilder.append("=== AutoCraft 搜索结果 ===\n");
            resultBuilder.append("关键词: ").append(searchKeyword.get()).append("\n");
            resultBuilder.append("最小星标: ").append(minStars.get()).append("\n");
            resultBuilder.append("找到 ").append(Math.min(repositories.length, maxResults.get())).append(" 个仓库:\n\n");

            int count = 0;
            for (String repo : repositories) {
                if (count >= maxResults.get()) break;
                
                String name = extractJsonValue(repo, "\"full_name\":");
                String description = extractJsonValue(repo, "\"description\":");
                String htmlUrl = extractJsonValue(repo, "\"html_url\":");
                int stars = extractValue(repo, "\"stargazers_count\":", ",");
                String lang = extractJsonValue(repo, "\"language\":");

                count++;
                resultBuilder.append(count).append(". ").append(name).append("\n");
                resultBuilder.append("   URL: ").append(htmlUrl).append("\n");
                
                if (showDetails.get()) {
                    resultBuilder.append("   星标: ").append(stars).append("\n");
                    if (lang != null && !lang.equals("null")) {
                        resultBuilder.append("   语言: ").append(lang).append("\n");
                    }
                    if (description != null && !description.equals("null")) {
                        resultBuilder.append("   描述: ").append(description).append("\n");
                    }
                }
                resultBuilder.append("\n");
            }

            String results = resultBuilder.toString();
            info(results);

            if (copyToClipboard.get()) {
                try {
                    mc.keyboard.setClipboard(results);
                    info("搜索结果已复制到剪贴板");
                } catch (Exception e) {
                    warning("复制到剪贴板失败: " + e.getMessage());
                }
            }

            foundRepositories.set(count);
            info("搜索完成，共找到 " + count + " 个符合条件的仓库");

        } catch (Exception e) {
            error("解析搜索结果时发生错误: " + e.getMessage());
        }
    }

    private String extractJsonValue(String json, String key) {
        try {
            int start = json.indexOf(key);
            if (start == -1) return null;
            
            // Check if the value is null
            int nullStart = json.indexOf("null", start + key.length());
            int quoteStart = json.indexOf("\"", start + key.length());
            
            if (nullStart != -1 && (quoteStart == -1 || nullStart < quoteStart)) {
                return null;
            }
            
            start = quoteStart + 1;
            int end = json.indexOf("\"", start);
            
            return json.substring(start, end);
        } catch (Exception e) {
            return null;
        }
    }

    private int extractValue(String json, String key, String delimiter) {
        try {
            int start = json.indexOf(key);
            if (start == -1) return 0;
            
            start += key.length();
            int end = json.indexOf(delimiter, start);
            if (end == -1) end = json.length();
            
            String value = json.substring(start, end).trim();
            return Integer.parseInt(value);
        } catch (Exception e) {
            return 0;
        }
    }

    public int getFoundRepositoriesCount() {
        return foundRepositories.get();
    }

    public boolean isCurrentlySearching() {
        return isSearching.get();
    }
}