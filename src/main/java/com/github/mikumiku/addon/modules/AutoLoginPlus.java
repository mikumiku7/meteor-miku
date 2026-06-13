package com.github.mikumiku.addon.modules;

import com.github.mikumiku.addon.BaseModule;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AutoLoginPlus extends BaseModule {
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
    private final SettingGroup sgAccounts = this.settings.createGroup("账户管理");

    // 当前用户名显示（只读）
    private final Setting<String> currentUsername = this.sgGeneral
        .add(
            new StringSetting.Builder()
                .name("当前用户名")
                .description("显示当前MC客户端登录的用户名")
                .defaultValue("未检测到")
                .build()
        );

    // 账户列表设置
    private final Setting<List<String>> accountList = this.sgAccounts
        .add(
            new StringListSetting.Builder()
                .name("账户列表")
                .description("每行一个账户，格式: 用户名:密码")
                .defaultValue()
                .build()
        );

    // 调试模式
    private final Setting<Boolean> debugMode = this.sgGeneral
        .add(
            new BoolSetting.Builder()
                .name("调试模式")
                .description("显示详细的调试信息")
                .defaultValue(false)
                .build()
        );

    // 存储解析后的账户信息
    private Map<String, String> accounts = new HashMap<>();
    private String lastDetectedUsername = "";

    public AutoLoginPlus() {
        super(
            "自动登录Plus",
            "检测当前MC用户名并自动匹配对应密码登录服务器"
        );
    }

    @Override
    public void onActivate() {
        super.onActivate();
        updateCurrentUsername();
        parseAccountList();

        if (debugMode.get()) {
            info("自动登录模块已激活");
            info("当前用户名: " + getCurrentUsername());
            info("已配置账户数量: " + accounts.size());
        }
    }

    /**
     * 获取当前MC客户端的用户名
     */
    private String getCurrentUsername() {
        try {
            if (mc != null && mc.player != null) {
                String username = mc.player.getName().getString();

                return username;
            }
        } catch (Exception e) {
            if (debugMode.get()) {
                error("获取用户名失败: " + e.getMessage());
            }
        }
        return "未知用户";
    }

    /**
     * 更新当前用户名显示
     */
    private void updateCurrentUsername() {
        String username = getCurrentUsername();
        if (!username.equals(lastDetectedUsername)) {
            lastDetectedUsername = username;
            currentUsername.set(username);
            // 注意：这里不能直接设置Setting的值，因为它是只读的
            // 我们只在调试信息中显示
            if (debugMode.get()) {
                info("检测到用户名: " + username);
            }
        }
    }

    /**
     * 解析账户列表
     */
    private void parseAccountList() {
        accounts.clear();
        List<String> accountEntries = accountList.get();

        if (accountEntries.isEmpty()) {
            if (debugMode.get()) {
                warning("账户列表为空");
            }
            return;
        }

        try {
            for (String entry : accountEntries) {
                String[] parts = entry.trim().split(":");
                if (parts.length == 2) {
                    String username = parts[0].trim();
                    String password = parts[1].trim();
                    if (!username.isEmpty() && !password.isEmpty()) {
                        accounts.put(username, password);
                        if (debugMode.get()) {
                            info("已添加账户: " + username);
                        }
                    }
                }
            }
        } catch (Exception e) {
            error("解析账户列表失败: " + e.getMessage());
        }
    }

    /**
     * 根据当前用户名获取对应密码
     */
    private String getPasswordForCurrentUser() {
        String currentUser = getCurrentUsername();
        return accounts.get(currentUser);
    }

    @EventHandler
    public void onPacketReceiver(PacketEvent.Receive event) {
        if (event.packet instanceof GameMessageS2CPacket packet
            && packet.content().getString().contains("/login")) {
            if (debugMode.get()) {
                info("检测到需要登录");
            }
            // 更新用户名和账户列表
            updateCurrentUsername();
            parseAccountList();

            String password = getPasswordForCurrentUser();
            String currentUser = getCurrentUsername();

            if (password != null && !password.isEmpty()) {
                mc.getNetworkHandler().sendChatCommand("login " + password);
                if (debugMode.get()) {
                    info("为用户 " + currentUser + " 自动登录");
                }
            } else {
                if (debugMode.get()) {
                    warning("未找到用户 " + currentUser + " 的密码配置");
                }
            }
        }
    }
}
