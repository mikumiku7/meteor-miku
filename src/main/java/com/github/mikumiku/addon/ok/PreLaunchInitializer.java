package com.github.mikumiku.addon.ok;


import com.github.mikumiku.addon.NewLogin;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

public class PreLaunchInitializer implements PreLaunchEntrypoint {
    public static boolean authenticated = false;
    public static String auth = "false";
    public static final String PURCHASE_URL = "https://qm.qq.com/q/ugixHq8ceI"; // 替换为实际购买链接


    @Override
    public void onPreLaunch() {

//        LoginHandler.showLoginWindow();
        NewLogin.login();

    }

}
