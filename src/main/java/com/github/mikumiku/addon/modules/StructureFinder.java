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
import com.seedfinding.mcfeature.structure.*;
import lombok.extern.slf4j.Slf4j;
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

import static meteordevelopment.meteorclient.utils.world.Dimension.Overworld;

@Slf4j
public class StructureFinder extends Module {

    // 设置组
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgSeed = settings.createGroup("种子设置");
    private final SettingGroup sgStructures = settings.createGroup("结构设置");
    // 通用设置
    private final Setting<Integer> searchRadius = sgGeneral.add(new IntSetting.Builder()
        .name("搜索半径")
        .description("以玩家为中心的搜索半径（区块）。")
        .defaultValue(100)
        .min(10)
        .max(20000)
        .sliderMin(10)
        .sliderMax(20000)
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
        .description("开始搜索结构位置")
        .defaultValue(false)
        .onChanged(this::onStartSearchChanged)
        .build()
    );

    // 结构设置
    private final Setting<Boolean> findVillage = sgStructures.add(new BoolSetting.Builder()
        .name("查找村庄")
        .description("查找村庄结构")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> findShipwreck = sgStructures.add(new BoolSetting.Builder()
        .name("查找沉船")
        .description("查找沉船结构")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> findBuriedTreasure = sgStructures.add(new BoolSetting.Builder()
        .name("查找埋藏的宝藏")
        .description("查找埋藏的宝藏结构")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> findOceanRuin = sgStructures.add(new BoolSetting.Builder()
        .name("查找海底废墟")
        .description("查找海底废墟结构")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> findPillagerOutpost = sgStructures.add(new BoolSetting.Builder()
        .name("查找掠夺者前哨站")
        .description("查找掠夺者前哨站结构")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> findNetherFortress = sgStructures.add(new BoolSetting.Builder()
        .name("查找下界要塞")
        .description("查找下界要塞结构")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> findBastionRemnant = sgStructures.add(new BoolSetting.Builder()
        .name("查找堡垒遗迹")
        .description("查找堡垒遗迹结构")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> findEndCity = sgStructures.add(new BoolSetting.Builder()
        .name("查找末地城")
        .description("查找末地城结构")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> findDesertPyramid = sgStructures.add(new BoolSetting.Builder()
        .name("查找沙漠神殿")
        .description("查找沙漠神殿结构")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> findIgloo = sgStructures.add(new BoolSetting.Builder()
        .name("查找雪屋")
        .description("查找雪屋结构")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> findJunglePyramid = sgStructures.add(new BoolSetting.Builder()
        .name("查找丛林神庙")
        .description("查找丛林神庙结构")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> findMansion = sgStructures.add(new BoolSetting.Builder()
        .name("查找林地府邸")
        .description("查找林地府邸结构")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> findMineshaft = sgStructures.add(new BoolSetting.Builder()
        .name("查找废弃矿井")
        .description("查找废弃矿井结构")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> findMonument = sgStructures.add(new BoolSetting.Builder()
        .name("查找海底神殿")
        .description("查找海底神殿结构")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> findNetherFossil = sgStructures.add(new BoolSetting.Builder()
        .name("查找下界化石")
        .description("查找下界化石结构")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> findRuinedPortal = sgStructures.add(new BoolSetting.Builder()
        .name("查找废弃传送门")
        .description("查找废弃传送门结构")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> findStronghold = sgStructures.add(new BoolSetting.Builder()
        .name("查找要塞")
        .description("查找要塞结构")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> findSwampHut = sgStructures.add(new BoolSetting.Builder()
        .name("查找沼泽小屋")
        .description("查找沼泽小屋结构")
        .defaultValue(false)
        .build()
    );

    // 内部变量
    private boolean isSearching = false;
    private CompletableFuture<Void> searchTask = null;

    public StructureFinder() {
        super(MikuMikuAddon.CATEGORY, "结构搜索", "自动搜索各种结构位置。此模块需要 xaero 小地图mod。");
    }

    @Override
    public void onActivate() {
        if (autoSearch.get()) {
            startStructureSearch();
        }
        info("结构搜索模块已启用");
    }

    @Override
    public void onDeactivate() {
        if (searchTask != null && !searchTask.isDone()) {
            searchTask.cancel(true);
            isSearching = false;
            info("搜索已取消");
        }
        info("结构搜索模块已禁用");
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
            String seed = seedInput.get().trim();
            if (!seed.isEmpty()) {
                try {
                    Seeds.get().setSeed(seed, mcVersion.get());
                    info("已设置种子: " + seed + " 版本: " + mcVersion.get().name);
                } catch (Exception e) {
                    error("设置种子失败: " + e.getMessage());
                }
            } else {
                error("请先输入种子");
            }
            applySeed.set(false);
        }
    }

    private void onStartSearchChanged(boolean value) {
        if (value && !isSearching) {
            startStructureSearch();
        }
    }

    private void startStructureSearch() {
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

        // 验证种子数据
        if (worldSeed.seed == null || worldSeed.version == null) {
            error("种子数据无效，请重新设置种子");
            return;
        }

        // 检查是否在主世界
        if (PlayerUtils.getDimension() != Overworld) {
            warning("建议在主世界使用此功能以获得最准确的结果");
        }

        isSearching = true;
        BlockPos playerPos = mc.player.getBlockPos();

        info("开始搜索结构位置... 半径: " + searchRadius.get() + " 区块");

        // 异步搜索
        searchTask = CompletableFuture.runAsync(() -> {
            try {
                searchStructureLocations(worldSeed, playerPos);
            } catch (Exception e) {
                error("搜索过程中发生错误: " + e.getMessage());
                log.error("搜索过程中发生错误: ", e);  // 在开发环境中启用
                isSearching = false;
            }
        });
    }

    private void searchStructureLocations(Seed worldSeed, BlockPos playerPos) {
        List<StructureLocation> structureLocations = new ArrayList<>();

        try {
            // 验证种子数据
            if (worldSeed == null || worldSeed.seed == null || worldSeed.version == null) {
                error("种子数据无效");
                return;
            }

            long seed = worldSeed.seed;
            MCVersion version = worldSeed.version;

            info("使用种子: " + seed + ", 版本: " + version.name);

            BiomeSource overworldBiomeSource = BiomeSource.of(Dimension.OVERWORLD, version, seed);
            BiomeSource netherBiomeSource = BiomeSource.of(Dimension.NETHER, version, seed);
            ChunkRand rand = new ChunkRand();

            int radius = searchRadius.get();

            // 安全检查搜索半径
            if (radius > 500) {
                warning("搜索半径过大 (" + radius + ")，可能导致性能问题或错误。 ");
            }

            int playerChunkX = playerPos.getX() >> 4;
            int playerChunkZ = playerPos.getZ() >> 4;

            // 搜索主世界结构
            if (findVillage.get()) {
                searchRegionStructure(structureLocations, new Village(version), seed, overworldBiomeSource, rand, playerChunkX, playerChunkZ, radius, "村庄");
            }

            if (findShipwreck.get()) {
                searchRegionStructure(structureLocations, new Shipwreck(version), seed, overworldBiomeSource, rand, playerChunkX, playerChunkZ, radius, "沉船");
            }

            if (findBuriedTreasure.get()) {
                searchRegionStructure(structureLocations, new BuriedTreasure(version), seed, overworldBiomeSource, rand, playerChunkX, playerChunkZ, radius, "埋藏的宝藏");
            }

            if (findOceanRuin.get()) {
                searchRegionStructure(structureLocations, new OceanRuin(version), seed, overworldBiomeSource, rand, playerChunkX, playerChunkZ, radius, "海底废墟");
            }

            if (findPillagerOutpost.get()) {
                searchRegionStructure(structureLocations, new PillagerOutpost(version), seed, overworldBiomeSource, rand, playerChunkX, playerChunkZ, radius, "掠夺者前哨站");
            }

            if (findDesertPyramid.get()) {
                searchRegionStructure(structureLocations, new DesertPyramid(version), seed, overworldBiomeSource, rand, playerChunkX, playerChunkZ, radius, "沙漠神殿");
            }

            if (findIgloo.get()) {
                searchRegionStructure(structureLocations, new Igloo(version), seed, overworldBiomeSource, rand, playerChunkX, playerChunkZ, radius, "雪屋");
            }

            if (findJunglePyramid.get()) {
                searchRegionStructure(structureLocations, new JunglePyramid(version), seed, overworldBiomeSource, rand, playerChunkX, playerChunkZ, radius, "丛林神庙");
            }

            if (findMansion.get()) {
                searchRegionStructure(structureLocations, new Mansion(version), seed, overworldBiomeSource, rand, playerChunkX, playerChunkZ, radius, "林地府邸");
            }

            if (findMineshaft.get()) {
                searchStructure(structureLocations, new Mineshaft(version), seed, overworldBiomeSource, rand, playerChunkX, playerChunkZ, radius, "废弃矿井");
            }

            if (findMonument.get()) {
                searchRegionStructure(structureLocations, new Monument(version), seed, overworldBiomeSource, rand, playerChunkX, playerChunkZ, radius, "海底神殿");
            }

            if (findRuinedPortal.get()) {
                searchRegionStructure(structureLocations, new RuinedPortal(Dimension.OVERWORLD, version), seed, overworldBiomeSource, rand, playerChunkX, playerChunkZ, radius, "废弃传送门");
            }

            if (findStronghold.get()) {
                searchStructure(structureLocations, new Stronghold(version), seed, overworldBiomeSource, rand, playerChunkX, playerChunkZ, radius, "要塞");
            }

            if (findSwampHut.get()) {
                searchRegionStructure(structureLocations, new SwampHut(version), seed, overworldBiomeSource, rand, playerChunkX, playerChunkZ, radius, "沼泽小屋");
            }

            // 搜索下界结构
            if (findNetherFortress.get()) {
                // 下界要塞搜索
                searchNetherRegionStructure(structureLocations, new Fortress(version), seed, version, playerChunkX, playerChunkZ, radius, "下界要塞");
            }

            if (findBastionRemnant.get()) {
                // 堡垒遗迹搜索
                searchNetherRegionStructure(structureLocations, new BastionRemnant(version), seed, version, playerChunkX, playerChunkZ, radius, "堡垒遗迹");
            }

            if (findNetherFossil.get()) {
                // 下界化石搜索
                searchNetherRegionStructure(structureLocations, new NetherFossil(version), seed, version, playerChunkX, playerChunkZ, radius, "下界化石");
            }

            // 搜索末地结构
            if (findEndCity.get()) {
                searchEndRegionStructure(structureLocations, new EndCity(version), seed, version, playerChunkX, playerChunkZ, radius, "末地城");
            }

        } catch (Exception e) {
            error("搜索过程中发生错误, 建议调小搜索半径: " + e.getMessage());
            return;
        }

        // 显示结果
        displayResults(structureLocations, playerPos);
        isSearching = false;
    }

    private void searchStructure(List<StructureLocation> locations, Structure structure, long seed, BiomeSource biomeSource, ChunkRand rand, int playerChunkX, int playerChunkZ, int radius, String structureName) {
        try {
            int minChunkX = Math.max(playerChunkX - radius, Integer.MIN_VALUE / 2);
            int maxChunkX = Math.min(playerChunkX + radius, Integer.MAX_VALUE / 2);
            int minChunkZ = Math.max(playerChunkZ - radius, Integer.MIN_VALUE / 2);
            int maxChunkZ = Math.min(playerChunkZ + radius, Integer.MAX_VALUE / 2);

            for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
                for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                    if (!isSearching) return; // 检查是否被取消

                    try {
                        CPos pos = new CPos(chunkX, chunkZ);

                        // 检查是否在搜索范围内
                        double chunkDistance = Math.sqrt(Math.pow(pos.getX() - playerChunkX, 2) +
                            Math.pow(pos.getZ() - playerChunkZ, 2));
                        if (chunkDistance > radius) continue;

                        if (structure.canSpawn(pos, biomeSource)) {
                            BPos blockPos = new BPos(pos.getX() * 16, 0, pos.getZ() * 16);
                            double distance = Math.sqrt(Math.pow(blockPos.getX() - playerChunkX * 16, 2) +
                                Math.pow(blockPos.getZ() - playerChunkZ * 16, 2));

                            locations.add(new StructureLocation(blockPos, distance, structureName));
                        }
                    } catch (Exception e) {
                        warning(String.format("处理区块 (%d,%d) 时出错: %s", chunkX, chunkZ, e.getMessage()));
                        continue;
                    }
                }
            }
        } catch (Exception e) {
            warning("搜索结构 " + structureName + " 时出错: " + e.getMessage());
        }
    }

    private void searchRegionStructure(List<StructureLocation> locations, RegionStructure<?, ?> structure, long seed, BiomeSource biomeSource, ChunkRand rand, int playerChunkX, int playerChunkZ, int radius, String structureName) {
        try {
            int spacing = structure.getSpacing();
            if (spacing <= 0) {
                warning("结构 " + structureName + " 间距无效: " + spacing);
                return;
            }

            int minChunkX = Math.max(playerChunkX - radius, Integer.MIN_VALUE / 2);
            int maxChunkX = Math.min(playerChunkX + radius, Integer.MAX_VALUE / 2);
            int minChunkZ = Math.max(playerChunkZ - radius, Integer.MIN_VALUE / 2);
            int maxChunkZ = Math.min(playerChunkZ + radius, Integer.MAX_VALUE / 2);

            int minRegionX = minChunkX / spacing;
            int maxRegionX = maxChunkX / spacing;
            int minRegionZ = minChunkZ / spacing;
            int maxRegionZ = maxChunkZ / spacing;

            for (int regionX = minRegionX; regionX <= maxRegionX; regionX++) {
                for (int regionZ = minRegionZ; regionZ <= maxRegionZ; regionZ++) {
                    if (!isSearching) return; // 检查是否被取消

                    try {
                        CPos pos = structure.getInRegion(seed, regionX, regionZ, rand);
                        if (pos == null) {
                            continue;
                        }
                        // 检查是否在搜索范围内
                        double chunkDistance = Math.sqrt(Math.pow(pos.getX() - playerChunkX, 2) +
                            Math.pow(pos.getZ() - playerChunkZ, 2));
                        if (chunkDistance > radius) continue;

                        if (structure.canSpawn(pos, biomeSource)) {
                            BPos blockPos = new BPos(pos.getX() * 16, 0, pos.getZ() * 16);

                            double distance = Math.sqrt(Math.pow(blockPos.getX() - playerChunkX * 16, 2) +
                                Math.pow(blockPos.getZ() - playerChunkZ * 16, 2));

                            locations.add(new StructureLocation(blockPos, distance, structureName));
                        }
                    } catch (Exception e) {
                        warning(String.format("处理区域 (%d,%d) 时出错: %s", regionX, regionZ, e.getMessage()));
                        continue;
                    }
                }
            }
        } catch (Exception e) {
            warning("搜索结构 " + structureName + " 时出错: " + e.getMessage());
        }
    }

    private void searchNetherRegionStructure(List<StructureLocation> locations, RegionStructure<?, ?> structure, long seed, MCVersion version, int playerChunkX, int playerChunkZ, int radius, String structureName) {
        try {
            // 下界结构需要使用下界维度
            BiomeSource netherBiomeSource = BiomeSource.of(Dimension.NETHER, version, seed);

            int spacing = structure.getSpacing();
            if (spacing <= 0) {
                warning("结构 " + structureName + " 间距无效: " + spacing);
                return;
            }

            int minChunkX = Math.max(playerChunkX - radius, Integer.MIN_VALUE / 2);
            int maxChunkX = Math.min(playerChunkX + radius, Integer.MAX_VALUE / 2);
            int minChunkZ = Math.max(playerChunkZ - radius, Integer.MIN_VALUE / 2);
            int maxChunkZ = Math.min(playerChunkZ + radius, Integer.MAX_VALUE / 2);

            int minRegionX = minChunkX / spacing;
            int maxRegionX = maxChunkX / spacing;
            int minRegionZ = minChunkZ / spacing;
            int maxRegionZ = maxChunkZ / spacing;

            ChunkRand rand = new ChunkRand();

            for (int regionX = minRegionX; regionX <= maxRegionX; regionX++) {
                for (int regionZ = minRegionZ; regionZ <= maxRegionZ; regionZ++) {
                    if (!isSearching) return; // 检查是否被取消

                    try {
                        CPos pos = structure.getInRegion(seed, regionX, regionZ, rand);

                        // 检查是否在搜索范围内
                        double chunkDistance = Math.sqrt(Math.pow(pos.getX() - playerChunkX, 2) +
                            Math.pow(pos.getZ() - playerChunkZ, 2));
                        if (chunkDistance > radius) continue;

                        if (structure.canSpawn(pos, netherBiomeSource)) {
                            BPos blockPos = new BPos(pos.getX() * 16, 0, pos.getZ() * 16);
                            double distance = Math.sqrt(Math.pow(blockPos.getX() - playerChunkX * 16, 2) +
                                Math.pow(blockPos.getZ() - playerChunkZ * 16, 2));

                            locations.add(new StructureLocation(blockPos, distance, structureName));
                        }
                    } catch (Exception e) {
                        warning(String.format("处理区域 (%d,%d) 时出错: %s", regionX, regionZ, e.getMessage()));
                        continue;
                    }
                }
            }
        } catch (Exception e) {
            warning("搜索下界结构 " + structureName + " 时出错: " + e.getMessage());
        }
    }

    private void searchEndRegionStructure(List<StructureLocation> locations, RegionStructure<?, ?> structure, long seed, MCVersion version, int playerChunkX, int playerChunkZ, int radius, String structureName) {
        try {
            // 末地结构需要使用末地维度
            BiomeSource endBiomeSource = BiomeSource.of(Dimension.END, version, seed);

            int spacing = structure.getSpacing();
            if (spacing <= 0) {
                warning("结构 " + structureName + " 间距无效: " + spacing);
                return;
            }

            int minChunkX = Math.max(playerChunkX - radius, Integer.MIN_VALUE / 2);
            int maxChunkX = Math.min(playerChunkX + radius, Integer.MAX_VALUE / 2);
            int minChunkZ = Math.max(playerChunkZ - radius, Integer.MIN_VALUE / 2);
            int maxChunkZ = Math.min(playerChunkZ + radius, Integer.MAX_VALUE / 2);

            int minRegionX = minChunkX / spacing;
            int maxRegionX = maxChunkX / spacing;
            int minRegionZ = minChunkZ / spacing;
            int maxRegionZ = maxChunkZ / spacing;

            ChunkRand rand = new ChunkRand();

            for (int regionX = minRegionX; regionX <= maxRegionX; regionX++) {
                for (int regionZ = minRegionZ; regionZ <= maxRegionZ; regionZ++) {
                    if (!isSearching) return; // 检查是否被取消

                    try {
                        CPos pos = structure.getInRegion(seed, regionX, regionZ, rand);

                        // 检查是否在搜索范围内
                        double chunkDistance = Math.sqrt(Math.pow(pos.getX() - playerChunkX, 2) +
                            Math.pow(pos.getZ() - playerChunkZ, 2));
                        if (chunkDistance > radius) continue;

                        if (structure.canSpawn(pos, endBiomeSource)) {
                            BPos blockPos = new BPos(pos.getX() * 16, 0, pos.getZ() * 16);
                            double distance = Math.sqrt(Math.pow(blockPos.getX() - playerChunkX * 16, 2) +
                                Math.pow(blockPos.getZ() - playerChunkZ * 16, 2));

                            locations.add(new StructureLocation(blockPos, distance, structureName));
                        }
                    } catch (Exception e) {
                        warning(String.format("处理区域 (%d,%d) 时出错: %s", regionX, regionZ, e.getMessage()));
                        continue;
                    }
                }
            }
        } catch (Exception e) {
            warning("搜索末地结构 " + structureName + " 时出错: " + e.getMessage());
        }
    }

    private void displayResults(List<StructureLocation> locations, BlockPos playerPos) {
        if (locations.isEmpty()) {
            warning("在指定范围内未找到任何结构");
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
            StructureLocation loc = locations.get(i);
            BPos pos = loc.position;

            // 创建路径点名称，包含距离信息
            String waypointName;
            if (includeDistance.get()) {
                waypointName = String.format("%s #%d (%.0fm)", loc.structureType, i + 1, loc.distance);
            } else {
                waypointName = String.format("%s #%d", loc.structureType, i + 1);
            }

            // 添加到Xaero路径点（Y坐标设为100）
            WaypointUtils.addToWaypoints(pos.getX(), 100, pos.getZ(), waypointName, "结构");
            addedCount++;
        }

        info(String.format("搜索完成！找到 %d 个结构，已添加到Xaero路径点", addedCount));
    }

    // 内部类存储结构位置信息
    private static class StructureLocation {
        final BPos position;
        final double distance;
        final String structureType;

        StructureLocation(BPos position, double distance, String structureType) {
            this.position = position;
            this.distance = distance;
            this.structureType = structureType;
        }
    }
}
