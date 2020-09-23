package com.wangjc.clickhouse.base.util;

/**
 * clickHouse的工具类
 * @author wangjc
 * @title: ClickHouseBaseUtil
 * @projectName wangjc-clickhouse
 * @description: TODO
 * @date 2020/9/2310:45
 */
public class ClickHouseBaseUtil {

    /**
     * 将首字母变成大写：一般用于通过属性名称反射操作getter，setter方法
     * @param str
     * @return
     */
    public static String capitalize(final String str) {
        int strLen;
        if (str == null || (strLen = str.length()) == 0) {
            return str;
        }

        final int firstCodePoint = str.codePointAt(0);
        final int newCodePoint = Character.toTitleCase(firstCodePoint);
        if (firstCodePoint == newCodePoint) {
            // already capitalized
            return str;
        }

        final int newCodePoints[] = new int[strLen]; // cannot be longer than the char array
        int outOffset = 0;
        newCodePoints[outOffset++] = newCodePoint; // copy the first codepoint
        for (int inOffset = Character.charCount(firstCodePoint); inOffset < strLen; ) {
            final int codePoint = str.codePointAt(inOffset);
            newCodePoints[outOffset++] = codePoint; // copy the remaining ones
            inOffset += Character.charCount(codePoint);
        }
        return new String(newCodePoints, 0, outOffset);
    }

}
