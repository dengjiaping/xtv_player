package com.plugin.common.utils.zip;

import java.io.File;

public class FileOperator {

    public static final boolean deleteDir(File dir) {
        boolean bRet = false;
        if (dir != null && dir.isDirectory()) {
            File[] entries = dir.listFiles();
            int sz = entries.length;
            for (int i = 0; i < sz; i++) {
                if (entries[i].isDirectory()) {
                    deleteDir(entries[i]);
                } else {
                    entries[i].delete();
                }
            }
            dir.delete();
            bRet = true;
        }
        return bRet;
    }

    public static final long getDirectorySize(File dir) {
        long retSize = 0;
        if ((dir == null) || !dir.isDirectory()) {
            return retSize;
        }
        File[] entries = dir.listFiles();
        int count = entries.length;
        for (int i = 0; i < count; i++) {
            if (entries[i].isDirectory()) {
                retSize += getDirectorySize(entries[i]);
            } else {
                retSize += entries[i].length();
            }
        }
        return retSize;
    }

    public static final void createDirectory(String strDir) {
        File file = new File(strDir);
        if (!file.isDirectory()) {
            file.mkdir();
        }
    }

    public static final void moveFile(String strOriginal, String strDest) {
        try {
            File fileOriginal = new File(strOriginal);
            File fileDest = new File(strDest);
            fileOriginal.renameTo(fileDest);
        } catch (Exception e) {
        }
    }
}
