package com.github.mikumiku.addon.util;


import java.io.PrintWriter;
import java.io.StringWriter;

public class ExceptionToStringUtils {

    // 方法1：使用StringWriter和PrintWriter（推荐）
    public static String getStackTraceAsString(Throwable throwable) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        throwable.printStackTrace(printWriter);
        return stringWriter.toString();
    }

    // 方法2：使用Apache Commons Lang（如果项目中有这个依赖）
    // public static String getStackTraceAsString(Throwable throwable) {
    //     return org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace(throwable);
    // }

    // 方法3：手动构建堆栈信息
    public static String getStackTraceManually(Throwable throwable) {
        StringBuilder sb = new StringBuilder();
        sb.append(throwable.getClass().getName()).append(": ").append(throwable.getMessage()).append("\n");

        for (StackTraceElement element : throwable.getStackTrace()) {
            sb.append("\tat ").append(element.toString()).append("\n");
        }

        // 处理caused by
        Throwable cause = throwable.getCause();
        while (cause != null) {
            sb.append("Caused by: ").append(cause.getClass().getName())
                .append(": ").append(cause.getMessage()).append("\n");
            for (StackTraceElement element : cause.getStackTrace()) {
                sb.append("\tat ").append(element.toString()).append("\n");
            }
            cause = cause.getCause();
        }

        return sb.toString();
    }

    // 方法4：只获取异常信息和类名（简化版）
    public static String getSimpleExceptionInfo(Throwable throwable) {
        return throwable.getClass().getSimpleName() + ": " + throwable.getMessage();
    }

    // 方法5：获取前N行堆栈信息
    public static String getStackTraceLines(Throwable throwable, int maxLines) {
        String fullStackTrace = getStackTraceAsString(throwable);
        String[] lines = fullStackTrace.split("\n");

        if (lines.length <= maxLines) {
            return fullStackTrace;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < maxLines; i++) {
            sb.append(lines[i]).append("\n");
        }
        sb.append("... (").append(lines.length - maxLines).append(" more lines)");

        return sb.toString();
    }

}
