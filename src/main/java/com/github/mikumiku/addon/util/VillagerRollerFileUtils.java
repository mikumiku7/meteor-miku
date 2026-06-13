package com.github.mikumiku.addon.util;

import com.github.mikumiku.addon.modules.VillagerRoller.RollingEnchantment;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.function.Consumer;

/**
 * 村民刷取器文件操作工具类
 * 用于保存和加载附魔搜索列表到文件
 */
public class VillagerRollerFileUtils {

    /**
     * 从文件加载搜索附魔列表
     *
     * @param file              要加载的文件
     * @param searchingEnchants 要填充的附魔列表
     * @param errorCallback     错误消息回调函数
     * @return 是否加载成功
     */
    public static boolean loadSearchingFromFile(File file, List<RollingEnchantment> searchingEnchants, Consumer<String> errorCallback) {
        if (!file.exists() || !file.canRead()) {
            errorCallback.accept("文件不存在或无法加载");
            return false;
        }

        NbtCompound nbtData = null;
        try {
            nbtData = NbtIo.read(file.toPath());
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (nbtData == null) {
            errorCallback.accept("从文件加载NBT失败");
            return false;
        }

        NbtList nbtList = nbtData.getList("rolling", NbtElement.COMPOUND_TYPE);
        searchingEnchants.clear();

        for (NbtElement element : nbtList) {
            if (element.getType() != NbtElement.COMPOUND_TYPE) {
                errorCallback.accept("无效的列表元素");
                return false;
            }
            searchingEnchants.add(new RollingEnchantment().fromTag((NbtCompound) element));
        }

        return true;
    }

    /**
     * 保存搜索附魔列表到文件
     *
     * @param file              要保存的文件
     * @param searchingEnchants 要保存的附魔列表
     * @param errorCallback     错误消息回调函数
     * @return 是否保存成功
     */
    public static boolean saveSearchingToFile(File file, List<RollingEnchantment> searchingEnchants, Consumer<String> errorCallback) {
        NbtList nbtList = new NbtList();
        for (RollingEnchantment enchantment : searchingEnchants) {
            nbtList.add(enchantment.toTag());
        }

        NbtCompound nbtCompound = new NbtCompound();
        nbtCompound.put("rolling", nbtList);

        if (Files.notExists(file.getParentFile().toPath()) && !file.getParentFile().mkdirs()) {
            errorCallback.accept("创建目录失败");
            return false;
        }

        try {
            NbtIo.write(nbtCompound, file.toPath());
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }
}
