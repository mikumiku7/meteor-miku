package com.github.mikumiku.addon.auth.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * AES工具类 - 提供加密和哈希功能
 *
 * @author mikumiku7
 * @version 1.0.0
 */
public class AESUtil2 {

    /**
     * 计算字符串的SHA-256哈希值
     *
     * @param input 输入字符串
     * @return SHA-256哈希值（十六进制字符串）
     */
    public static String sha256(String input) {
        if (input == null) {
            return null;
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256算法不可用", e);
        }
    }

    /**
     * 计算字符串的MD5哈希值
     *
     * @param input 输入字符串
     * @return MD5哈希值（十六进制字符串）
     */
    public static String md5(String input) {
        if (input == null) {
            return null;
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5算法不可用", e);
        }
    }

    /**
     * 验证哈希值是否匹配
     *
     * @param input 原始输入
     * @param expectedHash 期望的哈希值
     * @return 是否匹配
     */
    public static boolean verifySha256(String input, String expectedHash) {
        if (input == null || expectedHash == null) {
            return false;
        }

        String actualHash = sha256(input);
        return expectedHash.equalsIgnoreCase(actualHash);
    }

    /**
     * 生成随机盐值
     *
     * @param length 盐值长度
     * @return 随机盐值
     */
    public static String generateSalt(int length) {
        StringBuilder salt = new StringBuilder();
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

        for (int i = 0; i < length; i++) {
            int index = (int) (Math.random() * chars.length());
            salt.append(chars.charAt(index));
        }

        return salt.toString();
    }

    /**
     * 带盐值的SHA-256哈希
     *
     * @param input 输入字符串
     * @param salt 盐值
     * @return 哈希值
     */
    public static String sha256WithSalt(String input, String salt) {
        if (input == null || salt == null) {
            return null;
        }

        return sha256(input + salt);
    }
}
