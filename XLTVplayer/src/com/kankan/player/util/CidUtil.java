package com.kankan.player.util;

import android.text.TextUtils;
import com.kankan.player.app.AppConfig;

import java.io.*;

public class CidUtil {

    static {
        System.loadLibrary("xunleicid");
    }

    public native static String nativeQueryCidByPath(String path);

    public native static String nativeQueryCidByData(byte[] data);

    public static String queryCid(final String path) {
        if (TextUtils.isEmpty(path)) {
            return null;
        }

        final File file = new File(path);
        long length = file.length();
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(path);
            return queryCid(fis, length);
        } catch (FileNotFoundException e) {
            AppConfig.LOGD("[[CidUtil]] queryCid FileNotFoundException:" + e.getMessage());
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                }
            }
        }

        return null;
    }

    public static String queryCid(InputStream is, long fileLen) {
        return nativeQueryCidByData(getData(is, fileLen));
    }

    /**
     * 不需要关闭is，因为InputStream的available方法返回int，所以当文件大小超出int型范围时会出现错误，因此要传入文件大小
     *
     * @param is
     * @param fileLen
     * @return
     */
    private static byte[] getData(InputStream is, long fileLen) {
        if (is == null) {
            return null;
        }

        byte[] buffer = null;
        final int CID_PART_SIZE = 20 << 10;

        try {
            int len = 0;

            if (fileLen < CID_PART_SIZE) {
                int bufferLen = (int) fileLen;
                buffer = new byte[bufferLen];
                len = is.read(buffer, 0, bufferLen);
                if (len != bufferLen) {
                    return null;
                }
            } else {
                buffer = new byte[3 * CID_PART_SIZE];
                len = is.read(buffer, 0, CID_PART_SIZE);
                if (len != CID_PART_SIZE) {
                    return null;
                }

                is.skip(fileLen / 3 - CID_PART_SIZE);
                len = is.read(buffer, CID_PART_SIZE, CID_PART_SIZE);
                if (len != CID_PART_SIZE) {
                    return null;
                }

                is.skip(fileLen - CID_PART_SIZE - (fileLen / 3 + CID_PART_SIZE));
                len = is.read(buffer, 2 * CID_PART_SIZE, CID_PART_SIZE);
                if (len != CID_PART_SIZE) {
                    return null;
                }
            }
        } catch (IOException e) {
            AppConfig.LOGD("[[CidUtil]] getData IOException:" + e.getMessage());
        }

        return buffer;
    }
}
