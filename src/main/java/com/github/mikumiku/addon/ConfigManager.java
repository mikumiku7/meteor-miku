package com.github.mikumiku.addon;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

/**
 * 保存自动login模块的配置， 因为彗星的自动 保存配置有问题， 升级版本有可能会丢失配置
 * 当前未使用
 */
public class ConfigManager {
    private static final String SECRET_KEY = "MySecretKey12345"; // 16字符密钥
    private static final String CONFIG_DIR = "config";
    private static final String CONFIG_FILE = "user.dat";

    // 保存用户信息
    public static void saveCredentials(String username, String password, boolean remember) {
        try {
            File configDir = new File(CONFIG_DIR);
            if (!configDir.exists()) {
                configDir.mkdirs();
            }

            JsonObject json = new JsonObject();
            json.addProperty("username", username);
            json.addProperty("password", remember ? password : "");
            json.addProperty("remember", remember);

            String jsonString = new Gson().toJson(json);
            String encrypted = encrypt(jsonString);

            Files.write(Paths.get(CONFIG_DIR, CONFIG_FILE), encrypted.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 读取用户信息
    public static JsonObject loadCredentials() {
        try {
            File configFile = new File(CONFIG_DIR, CONFIG_FILE);
            if (!configFile.exists()) {
                return null;
            }

            String encrypted = new String(Files.readAllBytes(Paths.get(CONFIG_DIR, CONFIG_FILE)));
            String decrypted = decrypt(encrypted);
            return new Gson().fromJson(decrypted, JsonObject.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // AES加密
    private static String encrypt(String data) throws Exception {
        SecretKeySpec key = new SecretKeySpec(SECRET_KEY.getBytes("UTF-8"), "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encrypted = cipher.doFinal(data.getBytes("UTF-8"));
        return Base64.getEncoder().encodeToString(encrypted);
    }

    // AES解密
    private static String decrypt(String encryptedData) throws Exception {
        SecretKeySpec key = new SecretKeySpec(SECRET_KEY.getBytes("UTF-8"), "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedData));
        return new String(decrypted, "UTF-8");
    }
}
