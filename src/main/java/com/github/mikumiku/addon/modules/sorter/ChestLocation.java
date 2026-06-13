package com.github.mikumiku.addon.modules.sorter;

import com.github.mikumiku.addon.util.ChatUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.util.math.BlockPos;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

/**
 * 箱子位置类
 */
public class ChestLocation implements Serializable {
    public final int x, y, z;
    public final boolean isDoubleChest;

    public ChestLocation(int x, int y, int z) {
        this(x, y, z, false, null);
    }

    public ChestLocation(int x, int y, int z, boolean isDoubleChest, ChestLocation pairedChest) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.isDoubleChest = isDoubleChest;
    }

    public static ChestLocation fromPos(BlockPos pos) {
        return new ChestLocation(pos.getX(), pos.getY(), pos.getZ());
    }

    public BlockPos toBlockPos() {
        return new BlockPos(x, y, z);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ChestLocation that = (ChestLocation) obj;
        return x == that.x && y == that.y && z == that.z;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z);
    }

    @Override
    public String toString() {

        return "(" + x + ", " + y + ", " + z + ")";
    }

    public String toJson() {
        // 使用Map作为中转，避免混淆后的反射问题
        Map<String, Object> map = Map.of(
            "x", x,
            "y", y,
            "z", z,
            "isDoubleChest", isDoubleChest
        );
        return new Gson().toJson(map);
    }

    public static ChestLocation fromJson(String json) {
        // 使用Map作为中转，避免混淆后的反射问题
        if (json == null || json.trim().isEmpty()) {
            return null;
        }

        try {
            TypeToken<Map<String, Object>> typeToken = new TypeToken<Map<String, Object>>() {
            };
            Map<String, Object> map = new Gson().fromJson(json, typeToken.getType());

            if (map == null) {
                return null;
            }

            int x = ((Number) map.get("x")).intValue();
            int y = ((Number) map.get("y")).intValue();
            int z = ((Number) map.get("z")).intValue();
            boolean isDoubleChest = Boolean.TRUE.equals(map.getOrDefault("isDoubleChest", false));

            return new ChestLocation(x, y, z, isDoubleChest, null);
        } catch (Exception e) {
            ChatUtils.sendMsg("Failed to parse ChestLocation from JSON: " + json);
        }

        return null;
    }

    public double distanceTo(int px, int py, int pz) {
        return Math.sqrt(Math.pow(x - px, 2) + Math.pow(y - py, 2) + Math.pow(z - pz, 2));
    }

    /**
     * 获取规范化的箱子位置（对于大箱子，总是返回较小坐标的那一半）
     */
    public ChestLocation getNormalizedLocation(ChestLocation pairedChest) {
        if (!isDoubleChest || pairedChest == null) {
            return this;
        }

        // 比较坐标，返回较小的那个作为主箱子
        if (x < pairedChest.x || (x == pairedChest.x && z < pairedChest.z)) {
            return this;
        } else {
            return pairedChest;
        }
    }
}
