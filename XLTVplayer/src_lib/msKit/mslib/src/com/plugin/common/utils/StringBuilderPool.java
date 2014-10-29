
package com.plugin.common.utils;

import java.util.ArrayList;
import java.util.List;

public class StringBuilderPool {

    private static final boolean DEBUG = false;
    private static final String TAG = "StringBuilderPool";
    private static final int MAX_LENGTH = 256;

    private List<CharSequence> mStringPool;
    private int mTotal;

    public StringBuilderPool() {
        mStringPool = new ArrayList<CharSequence>();
        mTotal = 0;
    }

    public StringBuilder allocString() {
        synchronized (mStringPool) {
            int poolSize = mStringPool.size();
            StringBuilder sb = null;
            if (poolSize > 0) {
                sb = (StringBuilder) mStringPool.remove(poolSize - 1);
            } else {
                sb = new StringBuilder(MAX_LENGTH);
                mTotal++;
            }
            sb.setLength(0);
            return sb;
        }
    }

    // |3111|12|533333|844444444|255|
    public StringBuilder allocStringCompact(char[] data, int start) {
        final int len = data[start++];
        return allocString(data, start, len);
    }

    // |11100000|20000000|33333000|44444444|55000000|
    public StringBuilder allocStringWide(char[] data, int start) {
        int len = 0;
        while (data[start + len] != 0) len++;
        return allocString(data, start, len);
    }

    public void getStringCompact(StringBuilder ssb, char[] data, int start) {
        final int len = data[start++];
        ssb.append(data, start, len);
    }

    public void getStringWide(StringBuilder ssb, char[] data, int start) {
        int len = 0;
        while (data[start + len] != 0) len++;
        ssb.append(data, start, len);
    }

    public StringBuilder allocString(char[] data, int start, int len) {
        StringBuilder sb = null;
        if (len > 0) {
            sb = allocString();
            sb.append(data, start, len);
        }
        return sb;
    }

    public boolean collectGarbage(CharSequence garbage) {
        if (garbage != null && garbage instanceof StringBuilder) {
            synchronized (mStringPool) {
                mStringPool.add(garbage);
                ((StringBuilder) garbage).setLength(0);
            }
            return true;
        }
        return false;
    }

    public int collectGarbage(List<CharSequence> garbageList) {
        int garbageSize = garbageList.size();
        int savedSize = garbageSize;
        synchronized (mStringPool) {
            while (garbageSize > 0) {
                CharSequence garbage = garbageList.get(garbageSize - 1);
                if (garbage != null && garbage instanceof StringBuilder) {
                    mStringPool.add(garbage);
                    ((StringBuilder) garbage).setLength(0);
                }
                garbageSize--;
            }
        }
        garbageList.clear();
        return savedSize - garbageSize;
    }
    /**
     * 
     * @param garbageList
     * @param first including this one
     * @param end excluding this one
     */
    public int collectGarbage(List<CharSequence> garbageList, int first, int end) {
        if (first < 0 || first >= end || end > garbageList.size()) {
            return 0;
        }
        int ret = 0;
        synchronized (mStringPool) {
            for (int i = first; i < end; i++) {
                CharSequence garbage = garbageList.get(first);
                if (garbage != null && garbage instanceof StringBuilder) {
                    mStringPool.add(garbage);
                    ((StringBuilder) garbage).setLength(0);
                    ret++;
                }
                garbageList.remove(first);
            }
        }
        return ret;
    }

    public void clear() {
        synchronized (mStringPool) {
            mStringPool.clear();
        }
        mTotal = 0;
    }

    @Override
    public String toString() {
        return "StringBuilderPool [mStringPool=" + mStringPool + ", mTotal=" + mTotal + "]";
    }
    
}

