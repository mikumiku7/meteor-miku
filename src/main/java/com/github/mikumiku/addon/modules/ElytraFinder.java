package com.github.mikumiku.addon.modules;

import com.github.mikumiku.addon.MikuMikuAddon;
import com.github.mikumiku.addon.util.WaypointUtils;
import com.github.mikumiku.addon.util.seeds.Seed;
import com.github.mikumiku.addon.util.seeds.Seeds;
import com.seedfinding.mcbiome.source.BiomeSource;
import com.seedfinding.mccore.rand.ChunkRand;
import com.seedfinding.mccore.state.Dimension;
import com.seedfinding.mccore.util.pos.BPos;
import com.seedfinding.mccore.util.pos.CPos;
import com.seedfinding.mccore.version.MCVersion;
import com.seedfinding.mcfeature.structure.EndCity;
import com.seedfinding.mcfeature.structure.generator.structure.EndCityGenerator;
import com.seedfinding.mcterrain.TerrainGenerator;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.BlockPos;
import xaero.hud.minimap.waypoint.set.WaypointSet;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static meteordevelopment.meteorclient.utils.world.Dimension.End;

public class ElytraFinder extends Module {

    // 设置组
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgSeed = settings.createGroup("种子设置");

    // 通用设置
    private final Setting<Integer> searchRadius = sgGeneral.add(new IntSetting.Builder()
        .name("搜索半径")
        .description("以玩家为中心的搜索半径（区块）")
        .defaultValue(100)
        .min(10)
        .max(2000)
        .sliderMin(10)
        .sliderMax(1000)
        .build()
    );

    private final Setting<Integer> maxResults = sgGeneral.add(new IntSetting.Builder()
        .name("最大结果数")
        .description("显示的最大鞘翅位置数量")
        .defaultValue(50)
        .min(1)
        .max(200)
        .sliderMin(1)
        .sliderMax(200)
        .build()
    );

    private final Setting<Boolean> includeDistance = sgGeneral.add(new BoolSetting.Builder()
        .name("路径点包含距离")
        .description("在路径点名称中包含距离信息")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> autoSearch = sgGeneral.add(new BoolSetting.Builder()
        .name("自动搜索")
        .description("启用模块时自动开始搜索")
        .defaultValue(true)
        .build()
    );

    // 种子设置
    private final Setting<String> seedInput = sgSeed.add(new StringSetting.Builder()
        .name("种子")
        .description("输入服务器种子")
        .defaultValue("")
        .build()
    );

    private final Setting<MCVersion> mcVersion = sgSeed.add(new EnumSetting.Builder<MCVersion>()
        .name("MC版本")
        .description("选择Minecraft版本")
        .defaultValue(MCVersion.latest())
        .build()
    );

    private final Setting<Boolean> applySeed = sgSeed.add(new BoolSetting.Builder()
        .name("应用种子")
        .description("应用手动输入的种子设置")
        .defaultValue(false)
        .onChanged(this::onApplySeedChanged)
        .build()
    );

    private final Setting<Boolean> startSearch = sgGeneral.add(new BoolSetting.Builder()
        .name("开始搜索")
        .description("开始搜索鞘翅位置")
        .defaultValue(false)
        .onChanged(this::onStartSearchChanged)
        .build()
    );

    // 内部变量
    private boolean isSearching = false;
    private CompletableFuture<Void> searchTask = null;

    public ElytraFinder() {
        super(MikuMikuAddon.CATEGORY, "鞘翅搜索", "自动搜索带末地船的末地城结构（包含鞘翅）");
    }

    @Override
    public void onActivate() {
        startElytraSearch();
        info("鞘翅搜索模块已启用");
    }

    @Override
    public void onDeactivate() {
        if (searchTask != null && !searchTask.isDone()) {
            searchTask.cancel(true);
            isSearching = false;
            info("搜索已取消");
        }
        info("鞘翅搜索模块已禁用");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        // 检查搜索任务状态
        if (searchTask != null && searchTask.isDone()) {
            isSearching = false;
            searchTask = null;
            startSearch.set(false);
        }
    }

    private void onApplySeedChanged(boolean value) {
        if (value) {
            String seed = seedInput.get();
            if (!seed.isEmpty()) {
                Seeds.get().setSeed(seed, mcVersion.get());
                info("已设置种子: " + seed + " 版本: " + mcVersion.get().name);
            } else {
                error("请先输入种子");
            }
            applySeed.set(false);
        }
    }

    private void onStartSearchChanged(boolean value) {
        if (value && !isSearching) {
            startElytraSearch();
        }
    }

    private void startElytraSearch() {
        if (isSearching) {
            warning("搜索正在进行中...");
            return;
        }

        if (mc.player == null) {
            error("玩家不存在");
            return;
        }

        // 获取种子
        Seed worldSeed = Seeds.get().getSeed();
        if (worldSeed == null) {
            error("无法获取世界种子，请手动设置种子");
            return;
        }

        // 检查是否在末地
        if (PlayerUtils.getDimension() != End) {
            warning("建议在末地使用此功能以获得最准确的结果");
        }

        isSearching = true;
        BlockPos playerPos = mc.player.getBlockPos();

        info("开始搜索鞘翅位置... 半径: " + searchRadius.get() + " 区块");

        // 异步搜索
        searchTask = CompletableFuture.runAsync(() -> {
            try {
                searchElytraLocations(worldSeed, playerPos);
            } catch (Exception e) {
                error("搜索过程中发生错误: " + e.getMessage());
                isSearching = false;
            }
        });
    }

    private void searchElytraLocations(Seed worldSeed, BlockPos playerPos) {
        List<ElytraLocation> elytraLocations = new ArrayList<>();

        try {
            long seed = worldSeed.seed;
            MCVersion version = worldSeed.version;

            BiomeSource biomeSource = BiomeSource.of(Dimension.END, version, seed);
            TerrainGenerator generator = TerrainGenerator.of(Dimension.END, biomeSource);
            EndCity endCity = new EndCity(version);
            EndCityGenerator endCityGenerator = new EndCityGenerator(version);
            ChunkRand rand = new ChunkRand();

            int radius = searchRadius.get();
            int playerChunkX = playerPos.getX() >> 4;
            int playerChunkZ = playerPos.getZ() >> 4;

            int searchCount = 0;
            int totalChunks = (radius * 2 + 1) * (radius * 2 + 1);

            // 搜索指定半径内的区块
            for (int regionX = (playerChunkX - radius) / endCity.getSpacing();
                 regionX <= (playerChunkX + radius) / endCity.getSpacing(); regionX++) {
                for (int regionZ = (playerChunkZ - radius) / endCity.getSpacing();
                     regionZ <= (playerChunkZ + radius) / endCity.getSpacing(); regionZ++) {

                    if (!isSearching) return; // 检查是否被取消

                    CPos pos = endCity.getInRegion(seed, regionX, regionZ, rand);

                    // 检查是否在搜索范围内
                    double chunkDistance = Math.sqrt(Math.pow(pos.getX() - playerChunkX, 2) +
                        Math.pow(pos.getZ() - playerChunkZ, 2));
                    if (chunkDistance > radius) continue;

                    if (endCity.canSpawn(pos, biomeSource)) {
                        if (endCity.canGenerate(pos, generator)) {
                            endCityGenerator.generate(generator, pos, rand);

                            // 检查是否有末地船（包含鞘翅）
                            if (endCityGenerator.hasShip()) {
                                BPos blockPos = new BPos(pos.getX() * 16, 0, pos.getZ() * 16);
                                double distance = Math.sqrt(Math.pow(blockPos.getX() - playerPos.getX(), 2) +
                                    Math.pow(blockPos.getZ() - playerPos.getZ(), 2));

                                elytraLocations.add(new ElytraLocation(blockPos, distance));

                                if (elytraLocations.size() >= maxResults.get()) {
                                    break;
                                }
                            }

                            endCityGenerator.reset();
                        }
                    }

                    searchCount++;
                    if (searchCount % 100 == 0) {
                        // 定期更新进度
                        double progress = (double) searchCount / totalChunks * 100;
                        info(String.format("搜索进度: %.1f%% (%d/%d)", progress, searchCount, totalChunks));
                    }
                }

                if (elytraLocations.size() >= maxResults.get()) {
                    break;
                }
            }

        } catch (Exception e) {
            error("搜索过程中发生错误: " + e.getMessage());
            return;
        }

        // 显示结果
        displayResults(elytraLocations, playerPos);
        isSearching = false;
    }

    private void displayResults(List<ElytraLocation> locations, BlockPos playerPos) {
        if (locations.isEmpty()) {
            warning("在指定范围内未找到带鞘翅的末地城");
            return;
        }

        // 按距离排序
        locations.sort((a, b) -> Double.compare(a.distance, b.distance));

        // 获取Xaero路径点集合
        WaypointSet waypointSet = WaypointUtils.getWaypointSet();
        if (waypointSet == null) {
            warning("无法获取Xaero路径点集合，请确保已安装Xaero地图模组");
            return;
        }

        int addedCount = 0;

        // 添加路径点到Xaero地图
        for (int i = 0; i < locations.size(); i++) {
            ElytraLocation loc = locations.get(i);
            BPos pos = loc.position;

            // 创建路径点名称，包含距离信息
            String waypointName;
            if (includeDistance.get()) {
                waypointName = String.format("鞘翅 #%d (%.0fm)", i + 1, loc.distance);
            } else {
                waypointName = String.format("鞘翅 #%d", i + 1);
            }

            // 添加到Xaero路径点（Y坐标设为100，一般末地城高度）
            WaypointUtils.addToWaypoints(pos.getX(), 100, pos.getZ(), waypointName);
            addedCount++;
        }

        info(String.format("搜索完成！找到 %d 个鞘翅位置，已添加到Xaero路径点", addedCount));
    }

    // 内部类存储鞘翅位置信息
    private static class ElytraLocation {
        final BPos position;
        final double distance;

        ElytraLocation(BPos position, double distance) {
            this.position = position;
            this.distance = distance;
        }
    }
}
