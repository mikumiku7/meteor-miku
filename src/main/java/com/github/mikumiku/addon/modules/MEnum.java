package com.github.mikumiku.addon.modules;

public interface MEnum {


    /**
     * 过滤模式枚举
     */
    enum ListMode {
        Whitelist("白名单"),
        Blacklist("黑名单");

        private final String displayName;

        ListMode(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }


    enum WalkMode {
        Simple("简单"),
        Smart("智能");

        private final String displayName;

        WalkMode(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }


    public enum WalkDirection {
        Forwards("前进"),
        Backwards("后退"),
        Left("左"),
        Right("右");

        private final String displayName;

        WalkDirection(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    enum LandingMode {
        Chorus("恰紫颂果"),
        Light("温柔降落"),
        HEAD("头朝下暴力降落");


        private final String displayName;

        LandingMode(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }
}
