package com.github.mikumiku.addon.dynamic;

import com.github.mikumiku.addon.util.RegistryUtil;
import org.reflections.Reflections;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class DV {
    private final static Map<Class<?>, Object> BEAN_MAP = new ConcurrentHashMap<>();

    public static <T> T of(Class<T> clazz) {
        if (BEAN_MAP.containsKey(clazz)) {
            Object bean = BEAN_MAP.get(clazz);
            return clazz.cast(bean);
        }
        List<Class<? extends T>> list = getAllAchieveClass(clazz);
        if (list.isEmpty()) {
            throw new NullPointerException("%s.%s is not achieve".formatted(clazz.getPackageName(), clazz.getName()));
        }
        Class<? extends T> achieveClass = list.getLast();
        try {
            T bean = achieveClass.getDeclaredConstructor().newInstance();
            BEAN_MAP.put(clazz, bean);
            return bean;
        } catch (InstantiationException | NoSuchMethodException | InvocationTargetException |
                 IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取所有接口的实现类
     */
    private static <T> List<Class<? extends T>> getAllAchieveClass(Class<T> clazz) {
        Reflections reflections = new Reflections("com.github.mikumiku.addon");
        Set<Class<? extends T>> subTypesOf = reflections.getSubTypesOf(clazz);
        return subTypesOf.stream()
            .filter(e -> !e.isInterface())
            .toList();
    }

    public static void main(String[] args) {
        System.out.println(DV.of(RegistryUtil.class).getClass());
    }

}
