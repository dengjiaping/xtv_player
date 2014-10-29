package com.kankan.player.explorer;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.HashSet;

/**
 * Created by zhangdi on 14-3-27.
 */
public class FilenameExtFilter implements FilenameFilter {

    private HashSet<String> mExts = new HashSet<String>();

    public FilenameExtFilter(String[] exts) {
        if (exts != null) {
            for (int i = 0; i < exts.length; i++) {
                if (exts[i] != null)
                    exts[i] = exts[i].toLowerCase();
            }
            mExts.addAll(Arrays.asList(exts));
        }
    }

    public boolean contains(String ext) {
        return mExts.contains(ext.toLowerCase());
    }

    @Override
    public boolean accept(File dir, String filename) {
        File file = new File(dir + File.separator + filename);
        if (!FileUtils.isNormalFile(file.getAbsolutePath()) || !FileUtils.shouldShowFile(file)) {
            return false;
        }

        //创维不想显示DCIM
        if (file.isDirectory() && !filename.equals("DCIM")) {
//            String[] names = file.list();
//            if (names != null && names.length > 0) {
//                for (String name : names) {
//                    if (accept(file, name)) {
//                        return true;
//                    }
//                }
//            }
//            return false;
            return true;
        }

        int dotPosition = filename.lastIndexOf('.');
        if (dotPosition != -1) {
            String ext = (String) filename.subSequence(dotPosition + 1, filename.length());
            return contains(ext.toLowerCase());
        }

        return false;
    }
}
