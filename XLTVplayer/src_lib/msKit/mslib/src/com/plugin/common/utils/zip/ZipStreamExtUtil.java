package com.plugin.common.utils.zip;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.util.Log;

class ZipStreamExtUtil implements ExtUtil {
    private static final String TAG = "ZipStreamExtUtil";
    private static final String SYSTEM_PATH_SPILT = System.getProperty("file.separator");

    private static final boolean DEBUG = false;

    private String mZipFileName;
    private String mZipExtToPlace;

    private List<String> mZipFileList;
    private InputStream mIs;

    private ErrorType mErrorType = ErrorType.NONE;

    public ZipStreamExtUtil() {

    }

    public ErrorType getErrorType() {
        return mErrorType;
    }

    public Set<String> getChildDirs(String path) {
        LOGD("  In getChildDirs ");
        mErrorType = ErrorType.NONE;
        Set<String> ret = new HashSet<String>();
        for (String name : mZipFileList) {
            if (path == null && name.endsWith(SYSTEM_PATH_SPILT) == true
                    && name.substring(0, name.length() - 1).contains(SYSTEM_PATH_SPILT) == false) {
                ret.add(name.substring(0, name.length() - 1));
            } else if (path != null && name.startsWith(path) == true
                    && name.substring(path.length()).endsWith(SYSTEM_PATH_SPILT) == true
                    && name.substring(path.length(), name.length() - 1).contains(SYSTEM_PATH_SPILT) == false) {
                ret.add(name.substring(path.length(), name.length() - 1));
            }
        }
        if (ret.size() != 0) {
            return ret;
        }
        mErrorType = ErrorType.FILE_UNFOUND;
        return null;
    }

    public Set<String> getChildFiles(String path) {
        mErrorType = ErrorType.NONE;
        Set<String> ret = new HashSet<String>();
        for (String name : mZipFileList) {
            if (path == null && name.endsWith(SYSTEM_PATH_SPILT) == false) {
                ret.add(name);
            } else if (path != null && name.startsWith(path) == true && name.endsWith(SYSTEM_PATH_SPILT) == false) {
                ret.add(name);
            }
        }
        if (ret.size() != 0) {
            return ret;
        }
        mErrorType = ErrorType.FILE_UNFOUND;
        return null;
    }

    public boolean setExtFile(String fileToExt) {
        mErrorType = ErrorType.NONE;
        mZipFileName = fileToExt;
        try {
            setExtStream(new BufferedInputStream(new FileInputStream(new File(mZipFileName))));
        } catch (IOException e) {
            Log.d(TAG, "Exception" + e.toString());
            mErrorType = ErrorType.SOURCE_FILE_UNFOUND;
            return false;
        }
        return true;
    }

    public boolean setExtStream(InputStream is) {
        mErrorType = ErrorType.NONE;
        mIs = null;
        mIs = is;
        if (mIs.markSupported() == true) {
            LOGD(" In setExtStream, the is support mark");
            try {
                mIs.mark(mIs.available());
            } catch (IOException e) {
                e.printStackTrace();
                mErrorType = ErrorType.IS_MARKERROR;
                return false;
            }
        } else {
            mErrorType = ErrorType.IS_NOTMARKABLE;
            return false;
        }
        mZipFileList = ZipUtil.getZipFileList(is);
        // dump();
        return true;
    }

    public void setExtPlace(String extPlace) {
        mZipExtToPlace = extPlace;
    }

    public boolean extFile(String fileName, String place) {
        mErrorType = ErrorType.NONE;
        if (fileName == null || place == null) {
            mErrorType = ErrorType.ARGUMENTS_INVALIDED;
            return false;
        }
        LOGD(" In extFile : fileName = " + fileName + " place = " + place);
        if (mZipFileList.contains(fileName) == false) {
            mErrorType = ErrorType.FILE_UNFOUND;
            return false;
        }
        if (mIs.markSupported() == true) {
            try {
                mIs.reset();
            } catch (IOException e) {
                e.printStackTrace();
                mErrorType = ErrorType.IS_MARKERROR;
                return false;
            }
        } else {
            mErrorType = ErrorType.IS_NOTMARKABLE;
            return false;
        }
        int pos = fileName.lastIndexOf(SYSTEM_PATH_SPILT);
        String fileNameSubFix = fileName.substring(pos == -1 ? 0 : pos + 1);
        if (ZipUtil.outputSubFile(mIs, fileName, mZipExtToPlace + place, fileNameSubFix) == true) {
            mErrorType = ErrorType.UNZIP_SUCCESS;
            return true;
        }
        mErrorType = ErrorType.UNZIP_FAILED;
        return false;
    }

    public boolean extDir(String dirName, String place) {
        mErrorType = ErrorType.NONE;
        if ((dirName == null) || (place == null)) {
            mErrorType = ErrorType.ARGUMENTS_INVALIDED;
            return false;
        }
        LOGD("  In extDir : dirName = " + dirName + " place = " + place);
        if (mZipFileList.contains(dirName) == false) {
            mErrorType = ErrorType.FILE_UNFOUND;
            return false;
        }
        if (mIs.markSupported() == true) {
            LOGD(" In extDir, is support mark");
            try {
                mIs.reset();
            } catch (IOException e) {
                e.printStackTrace();
                mErrorType = ErrorType.IS_MARKERROR;
                return false;
            }
        } else {
            mErrorType = ErrorType.IS_NOTMARKABLE;
            return false;
        }
        if (ZipUtil.UnzipSubDir(mIs, dirName, mZipExtToPlace + place) == true) {
            mErrorType = ErrorType.UNZIP_SUCCESS;
            return true;
        }
        mErrorType = ErrorType.UNZIP_FAILED;
        return false;
    }

    public String findFirstFile(String fileName) {
        mErrorType = ErrorType.NONE;
        for (String file : mZipFileList) {
            if (file.endsWith(fileName) == true
                    && (file.substring(0, file.lastIndexOf(fileName)).length() == 0 || file.substring(0,
                            file.lastIndexOf(fileName)).endsWith(SYSTEM_PATH_SPILT) == true)) {
                return file;
            }
        }
        mErrorType = ErrorType.UNZIP_FAILED;
        return null;
    }

    public boolean containFile(String fileName) {
        mErrorType = ErrorType.NONE;
        if (mZipFileList.contains(fileName) == true) {
            return true;
        }
        mErrorType = ErrorType.FILE_UNFOUND;
        return false;
    }

    public boolean containDir(String dirName) {
        mErrorType = ErrorType.NONE;
        if (mZipFileList.contains(dirName) == true && dirName.endsWith(SYSTEM_PATH_SPILT) == true) {
            return true;
        }
        mErrorType = ErrorType.FILE_UNFOUND;
        return false;
    }

    public boolean containFileUnderDir(String dirName) {
        LOGD(" In containFileUnderDir, dirName = " + dirName);
        mErrorType = ErrorType.NONE;
        if ((mZipFileList.contains(dirName) == true) && (dirName.endsWith(SYSTEM_PATH_SPILT) == true)) {
            for (String name : mZipFileList) {
                if ((name.startsWith(dirName) == true)
                        && (name.substring(dirName.length()).contains(SYSTEM_PATH_SPILT) == false)) {
                    return true;
                }
            }
        }
        mErrorType = ErrorType.FILE_UNFOUND;
        return false;
    }

    public boolean extAllFiles() {
        return true;
    }

    public void recycle() {
        ZipUtil.recycle();
    }

    // =========================================================================
    private static void LOGD(String s) {
        if (DEBUG) {
            Log.d(TAG, s);
        }
    }

    private void dump() {
        LOGD(" ----------- begin dump file list --------------");
        for (String name : mZipFileList) {
            LOGD(" file name = " + name);
        }
        LOGD(" ----------- end dump file list --------------");
    }
}
