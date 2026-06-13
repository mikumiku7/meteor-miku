package com.github.mikumiku.addon;

import com.github.mikumiku.addon.ok.PreLaunchInitializer;

public class NewLogin {

    public static void login() {


        PreLaunchInitializer.authenticated = true;


        PreLaunchInitializer.auth = "null";

        MikuMagic2.hit = 0.5;

    }
}
