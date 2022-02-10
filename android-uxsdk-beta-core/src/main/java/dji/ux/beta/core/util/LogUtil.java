package dji.ux.beta.core.util;

import android.util.Log;

/**
 * Class Description
 *
 * @author Hoker
 * @date 2021/12/14
 * <p>
 * Copyright (c) 2021, DJI All Rights Reserved.
 */
public class LogUtil {

    public static void DjiLog(Object... args) {
        StringBuffer buffer = new StringBuffer();
        for (Object arg : args) {
            buffer.append(arg != null ? arg : "NULL").append(" ");
        }
        e("DjiLog", buffer.toString());
    }

    private static void e(String tag, String msg) {
        if (tag == null || tag.length() == 0
                || msg == null || msg.length() == 0)
            return;

        int segmentSize = 4 * 1024;
        long length = msg.length();
        // 打印剩余日志
        if (length > segmentSize) {// 长度小于等于限制直接打印
            while (msg.length() > segmentSize) {// 循环分段打印日志
                String logContent = msg.substring(0, segmentSize);
                msg = msg.replace(logContent, "");
                Log.e(tag, logContent);
            }
        }
        Log.e(tag, msg);
    }
}
