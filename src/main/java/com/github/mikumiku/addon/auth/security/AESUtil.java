package com.github.mikumiku.addon.auth.security;

import com.google.gson.Gson;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class AESUtil {
    // 使用 SHA-256 将任意长度 password 转为 256bit key
    private static SecretKeySpec getKey(String password) throws Exception {
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] keyBytes = sha.digest(password.getBytes("UTF-8"));
        return new SecretKeySpec(keyBytes, "AES");
    }

    // 使用前 16 字节 key 作为固定 IV (可改为随机 IV 并与密文一起保存以增强安全)
    private static IvParameterSpec getIv(byte[] keyBytes) {
        byte[] iv = Arrays.copyOfRange(keyBytes, 0, 16);
        return new IvParameterSpec(iv);
    }

    public static String encrypt(String plainText, String password) throws Exception {
        SecretKeySpec keySpec = getKey(password);
        IvParameterSpec ivSpec = getIv(keySpec.getEncoded());
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
        byte[] encrypted = cipher.doFinal(plainText.getBytes("UTF-8"));
        return Base64.getEncoder().encodeToString(encrypted);
    }

    public static String decrypt(String cipherText, String password) throws Exception {
        SecretKeySpec keySpec = getKey(password);
        IvParameterSpec ivSpec = getIv(keySpec.getEncoded());
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
        byte[] decoded = Base64.getDecoder().decode(cipherText);
        byte[] decrypted = cipher.doFinal(decoded);
        return new String(decrypted, "UTF-8");
    }

    /**
     * 计算 SHA-256 摘要，并返回 Base64 字符串
     */
    public static String sha256(String input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digestBytes = md.digest(input.getBytes("UTF-8"));
        return Base64.getEncoder().encodeToString(digestBytes);
    }

    /**
     * 计算 SHA-256 摘要，返回仅含小写字母和数字的十六进制字符串
     */
    public static String sha256Hex(String input)     {
        byte[] digestBytes = null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            digestBytes = md.digest(input.getBytes("UTF-8"));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        return bytesToHex(digestBytes);
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            // &0xff 避免负值，|0x100 保证长度，substring(1) 去掉多余前缀
            sb.append(Integer.toHexString((b & 0xff) | 0x100).substring(1));
        }
        return sb.toString();
    }

    /**
     * 加密账号密码
     * @param account 账号
     * @param password 密码
     * @return 加密后的Base64字符串
     */
    public static String encryptAccount(String account, String password) throws Exception {
        // 1. 将账号密码放入HashMap
        Map<String, String> data = new HashMap<>();
        data.put("account", account);
        data.put("password", password);

        // 2. 转换为JSON字符串
        Gson gson = new Gson();
        String jsonString = gson.toJson(data);

        // 3. 获取当前秒级时间戳除以10作为密钥
        long timestamp = System.currentTimeMillis() / 1000;
        String keyPassword = String.valueOf(timestamp / 10);

        // 4. 使用AES加密
        String encryptedData = encrypt(jsonString, keyPassword);

        return encryptedData;
    }



    public static void main(String[] args) throws Exception {
        String password = "fakePassword";
        String plain = "https://mc-auto-open-auth.workers.dev/";

        String cipher = encrypt(plain, password);
        System.out.println("Encrypted: " + cipher);

        String decrypted = decrypt(cipher, password);
        System.out.println("Decrypted: " + decrypted);


        String text = plain;
        String hash = sha256(text);
        System.out.println("SHA-256 digest: " + hash);

        hash = sha256Hex(text);

        System.out.println("SHA-256 digest (hex): " + hash);
    }
}
