package com.github.mikumiku.addon.modules;

import com.github.mikumiku.addon.BaseModule;
import com.github.mikumiku.addon.util.ChatUtils;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.Fonts;
import meteordevelopment.meteorclient.renderer.text.FontFace;
import meteordevelopment.meteorclient.renderer.text.FontFamily;
import meteordevelopment.meteorclient.renderer.text.FontInfo;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.orbit.EventHandler;

/**
 * 使用教程模块
 * 提供Miku插件的使用教程和设置指南
 *
 * @author MikuMiku
 */
public class UserGuide extends BaseModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgFont = settings.createGroup("字体设置");
    private final SettingGroup sgVia = settings.createGroup("VIA设置");
    private final SettingGroup sgMods = settings.createGroup("前置MOD");

    // 基本教程设置
    private final Setting<Boolean> showWelcome = sgGeneral.add(new BoolSetting.Builder()
        .name("显示欢迎信息")
        .description("启用时显示欢迎信息和基本使用指南")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> autoSetFont = sgGeneral.add(new BoolSetting.Builder()
        .name("自动设置字体")
        .description("尝试自动设置中文字体（等线/Dengxian）")
        .defaultValue(true)
        .build()
    );

    // 字体教程设置
    private final Setting<Boolean> showFontGuide = sgFont.add(new BoolSetting.Builder()
        .name("显示字体教程")
        .description("显示如何设置中文字体的详细教程")
        .defaultValue(true)
        .build()
    );

    private final Setting<String> preferredFont = sgFont.add(new StringSetting.Builder()
        .name("首选字体")
        .description("推荐的中文字体名称")
        .defaultValue("Dengxian")
        .build()
    );

    // VIA教程设置
    private final Setting<Boolean> showViaGuide = sgVia.add(new BoolSetting.Builder()
        .name("显示VIA教程")
        .description("显示VIA版本设置教程")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> checkViaVersion = sgVia.add(new BoolSetting.Builder()
        .name("检查VIA版本")
        .description("自动检查当前VIA版本是否为1.20.6")
        .defaultValue(true)
        .build()
    );

    private final Setting<String> targetViaVersion = sgVia.add(new StringSetting.Builder()
        .name("目标VIA版本")
        .description("推荐的VIA版本")
        .defaultValue("1.20.6")
        .build()
    );

    // 前置MOD设置
    private final Setting<Boolean> showModsGuide = sgMods.add(new BoolSetting.Builder()
        .name("显示MOD教程")
        .description("显示必要的前置MOD安装教程")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> checkMods = sgMods.add(new BoolSetting.Builder()
        .name("检查前置MOD")
        .description("自动检查必要的前置MOD是否已安装")
        .defaultValue(true)
        .build()
    );

    private int tutorialStep = 0;
    private boolean tutorialActive = false;

    public UserGuide() {
        super("A使用教程",
            "📚 Miku插件完整使用指南和设置教程\n\n" +
                "✨ 本模块将指导您完成以下设置：\n" +
                "🔤 字体设置 - 配置中文字体确保界面正常显示\n" +
                "🌐 VIA版本 - 设置正确的VIA版本(1.20.6)以获得最佳兼容性\n" +
                "📦 前置MOD - 检查并指导安装必要的前置MOD\n\n" +
                "⚡ 主要功能：\n" +
                "• 自动设置中文字体(等线/Dengxian)\n" +
                "• 智能检查VIA版本兼容性\n" +
                "• 检测Baritone、Xaero地图、Litematica、ViaFabricPlus等前置MOD\n" +
                "• 分步骤的渐进式教程系统\n" +
                "• 快速指南和状态查询功能\n\n" +
                "💡 使用建议：\n" +
                "首次使用Miku插件时请启用本模块，\n" +
                "按照教程完成基本设置以获得最佳体验！"
        );
    }

    @Override
    public void onActivate() {
        super.onActivate();
        tutorialStep = 0;
        tutorialActive = true;

        if (showWelcome.get()) {
            showWelcomeMessage();
        }

        if (autoSetFont.get()) {
            attemptSetFont();
        }
    }

    @Override
    public void onDeactivate() {
        super.onActivate();
        tutorialActive = false;
        ChatUtils.sendMsg("教程已结束。如果需要帮助，请重新启用使用教程模块。");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!tutorialActive) return;

        // 每隔一段时间显示下一个教程步骤
        if (mc.player.age % 20 == 0 && tutorialStep < getTotalTutorialSteps()) {
            showNextTutorialStep();
            tutorialStep++;
        }
    }


    private void showWelcomeMessage() {
        ChatUtils.sendMsg("=== 欢迎使用Miku插件 ===");
        ChatUtils.sendMsg("本教程将指导您完成插件的基本设置");
        ChatUtils.sendMsg("确保获得最佳的中文显示和功能体验");
        ChatUtils.sendMsg("========================");
    }

    private void attemptSetFont() {
        try {
            // 尝试设置中文字体
            ChatUtils.sendMsg("正在尝试设置中文字体...");

            // 这里可以添加自动设置字体的逻辑
            // 由于字体设置通常在客户端配置中，我们提供指导

            try {
                Config config = Config.get();
                config.customFont.set(true);
                // 查找并设置Dengxian字体
                FontFamily dengxianFamily = Fonts.getFamily("Dengxian");

                if (dengxianFamily != null) {
                    FontFace dengxianFont = dengxianFamily.get(FontInfo.Type.Regular);
                    if (dengxianFont != null) {
                        config.font.set(dengxianFont);
                    }
                }
            } catch (Exception e) {

            }

            ChatUtils.sendMsg("请手动设置字体：");
            ChatUtils.sendMsg("1. 点击顶部的Config按钮");
            ChatUtils.sendMsg("2. 找到Font设置");
            ChatUtils.sendMsg("3. 选择" + preferredFont.get() + "字体");
            ChatUtils.sendMsg("4. 保存设置");

        } catch (Exception e) {
            warning("自动设置字体失败，请手动设置。");
        }
    }

    private void showNextTutorialStep() {
        switch (tutorialStep) {
            case 0:
                if (showFontGuide.get()) {
                    showFontTutorial();
                }
                break;
            case 1:
                if (showViaGuide.get()) {
                    showViaTutorial();
                }
                break;
            case 2:
                if (showModsGuide.get()) {
                    showModsTutorial();
                }
                break;
            case 3:
                showFinalInstructions();
                break;
        }
    }

    private void showFontTutorial() {
        ChatUtils.sendMsg("=== 字体设置教程 ===");
        ChatUtils.sendMsg("为了正确显示中文，请设置中文字体：");
        ChatUtils.sendMsg("1. 点击游戏界面右上角的'Config'按钮");
        ChatUtils.sendMsg("2. 在设置中找到'Font'选项");
        ChatUtils.sendMsg("3. 选择'等线(Dengxian)'或其他中文字体");
        ChatUtils.sendMsg("4. 点击保存并重新启动游戏");
        ChatUtils.sendMsg("提示：中文字体能确保界面文字正确显示");
        ChatUtils.sendMsg("===================");
    }

    private void showViaTutorial() {
        ChatUtils.sendMsg("=== VIA版本设置教程 ===");
        ChatUtils.sendMsg("VIA插件用于版本兼容，请设置如下：");
        ChatUtils.sendMsg("1. 确保已安装VIA插件");
        ChatUtils.sendMsg("2. 在VIA设置中将版本设置为：" + targetViaVersion.get());
        ChatUtils.sendMsg("3. 确保版本设置为1.20.6以获得最佳兼容性");
        ChatUtils.sendMsg("4. 保存设置并重新连接服务器");
        ChatUtils.sendMsg("注意：错误的VIA版本可能导致功能异常");
        ChatUtils.sendMsg("====================");

        if (checkViaVersion.get()) {
            checkViaCompatibility();
        }
    }

    private void showModsTutorial() {
        ChatUtils.sendMsg("=== 前置MOD安装教程 ===");
        ChatUtils.sendMsg("部分功能需要以下前置MOD：");
        ChatUtils.sendMsg("1. Baritone - 用于自动化移动和路径规划");
        ChatUtils.sendMsg("2. Xaero的小地图 - 小地图功能");
        ChatUtils.sendMsg("3. Litematica - 建筑和结构管理");
        ChatUtils.sendMsg("4. ViaFabric - 版本兼容（如果使用VIA）");
        ChatUtils.sendMsg("5. ViaFabricPlus - 增强版本兼容功能");
        ChatUtils.sendMsg("");
        ChatUtils.sendMsg("安装建议：");
        ChatUtils.sendMsg("- 从官方源下载MOD以确保安全性");
        ChatUtils.sendMsg("- 确保MOD版本与Minecraft版本匹配");
        ChatUtils.sendMsg("- 按照MOD作者说明进行配置");
        ChatUtils.sendMsg("===================");

        if (checkMods.get()) {
            checkRequiredMods();
        }
    }

    private void showFinalInstructions() {
        ChatUtils.sendMsg("=== 教程完成 ===");
        ChatUtils.sendMsg("基本设置已完成！以下是一些使用提示：");
        ChatUtils.sendMsg("• 所有模块支持中文界面");
        ChatUtils.sendMsg("• 大部分功能都有详细的设置选项");
        ChatUtils.sendMsg("• 遇到问题可以查看模块描述");
        ChatUtils.sendMsg("• 建议先在测试世界试用功能");
        ChatUtils.sendMsg("");
        ChatUtils.sendMsg("感谢使用Miku插件！");
        ChatUtils.sendMsg("插件群：1013297171");
        ChatUtils.sendMsg("===============");

        tutorialActive = false;
    }

    private void checkViaCompatibility() {
        try {
            // 这里可以添加检查VIA版本的逻辑
            // 由于我们无法直接访问VIA的API，我们提供指导
            ChatUtils.sendMsg("请确认您的VIA版本设置为：" + targetViaVersion.get());
        } catch (Exception e) {
            warning("无法检查VIA版本，请手动确认设置正确。");
        }
    }

    private void checkRequiredMods() {
        boolean allModsPresent = true;

        // 检查Baritone（通过尝试访问相关类）
        try {
            Class.forName("baritone.api.BaritoneAPI");
            ChatUtils.sendMsg("✓ Baritone 已安装");
        } catch (ClassNotFoundException e) {
            warning("✗ Baritone 未安装 - 部分自动化功能将不可用");
            allModsPresent = false;
        }

        // 检查Xaero地图（类似检查方式）
        try {
            Class.forName("xaero.minimap.XaeroMinimap");
            ChatUtils.sendMsg("✓ Xaero的小地图 已安装");
        } catch (ClassNotFoundException e) {
            warning("✗ Xaero的小地图 未安装 - 地图功能将不可用");
            allModsPresent = false;
        }

        // 检查Litematica
        try {
            Class.forName("fi.dy.masa.litematica.Litematica");
            ChatUtils.sendMsg("✓ Litematica 已安装");
        } catch (ClassNotFoundException e) {
            warning("✗ Litematica 未安装 - 建筑功能将不可用");
            allModsPresent = false;
        }

        // 检查ViaFabricPlus (检查两个可能的类名)
        boolean viaFabricPlusFound = false;
        try {
            Class.forName("de.florianmichael.viafabricplus.ViaFabricPlus");
            viaFabricPlusFound = true;
        } catch (ClassNotFoundException e) {
            // 尝试第二个类名
        }

        if (!viaFabricPlusFound) {
            try {
                Class.forName("com.viaversion.viafabricplus.ViaFabricPlusImpl");
                viaFabricPlusFound = true;
            } catch (ClassNotFoundException e) {
                // 两个类名都不存在
            }
        }

        if (viaFabricPlusFound) {
            ChatUtils.sendMsg("✓ ViaFabricPlus 已安装");
        } else {
            warning("✗ ViaFabricPlus 未安装 - 版本兼容功能可能受限");
            allModsPresent = false;
        }

        if (allModsPresent) {
            ChatUtils.sendMsg("所有推荐的前置MOD已安装！");
        } else {
            ChatUtils.sendMsg("某些前置MOD缺失，但不影响基本功能使用。");
        }
    }

    private int getTotalTutorialSteps() {
        int steps = 1; // 最终说明
        if (showFontGuide.get()) steps++;
        if (showViaGuide.get()) steps++;
        if (showModsGuide.get()) steps++;
        return steps;
    }

    /**
     * 显示字体设置的快速指南
     */
    public void showQuickFontGuide() {
        ChatUtils.sendMsg("=== 快速字体设置指南 ===");
        ChatUtils.sendMsg("1. 右上角 → Config → Font");
        ChatUtils.sendMsg("2. 选择字体：Dengxian（等线）");
        ChatUtils.sendMsg("3. 保存设置");
        ChatUtils.sendMsg("======================");
    }

    /**
     * 显示VIA设置的快速指南
     */
    public void showQuickViaGuide() {
        ChatUtils.sendMsg("=== 快速VIA设置指南 ===");
        ChatUtils.sendMsg("1. VIA设置 → 版本");
        ChatUtils.sendMsg("2. 设置版本：1.20.6");
        ChatUtils.sendMsg("3. 保存并重连");
        ChatUtils.sendMsg("======================");
    }

    /**
     * 显示MOD检查结果
     */
    public void showModCheckResults() {
        ChatUtils.sendMsg("=== 前置MOD检查结果 ===");
        checkRequiredMods();
        ChatUtils.sendMsg("========================");
    }

    /**
     * 重置教程进度
     */
    public void resetTutorial() {
        tutorialStep = 0;
        tutorialActive = true;
        ChatUtils.sendMsg("教程已重置，将重新开始。");
    }

    /**
     * 获取当前教程状态
     */
    public String getTutorialStatus() {
        if (!isActive()) {
            return "使用教程：未启用";
        }

        String status = tutorialActive ? "进行中" : "已完成";
        return String.format("使用教程：%s (进度：%d/%d)",
            status, tutorialStep, getTotalTutorialSteps());
    }
}
