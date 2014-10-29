package com.plugin.common.utils.zip;

import java.io.InputStream;
import java.util.Set;

public interface ExtUtil {
    public static enum ErrorType {
        NONE, FILE_UNFOUND, UNZIP_SUCCESS, UNZIP_FAILED, 
        SOURCE_FILE_UNFOUND, IS_INVALIDED, IS_NOTMARKABLE,
        IS_MARKERROR, ARGUMENTS_INVALIDED
    }

    /**
     * get the error type if other function return value is invalided.
     * 
     * @return special ErrorType
     */
    ErrorType getErrorType();
    /*
     * This interfaces get all the dir name under the path path argument is the
     * relative path for the theme, should end with the system file split
     */
	Set<String> getChildDirs(String path);

	/*
	 * This interfaces get all the file name under the path path argument is the
	 * relative path for the theme, should end with the system file split
	 */
	Set<String> getChildFiles(String path);

	/*
	 * ext a special file to the place
	 */
	boolean extFile(String fileName, String place);

	/*
	 * set the source file to ext
	 */
	boolean setExtFile(String fileToExt);

	/*
	 * set the input stream to ext
	 */
	boolean setExtStream(InputStream is);

	/*
	 * set the path for the ext file to save the path is a absolute path for the
	 * system to save the ext file
	 */
	void setExtPlace(String extPlace);

	/*
	 * ext all the file and dir under the dirName to the place. The place is a
	 * relative path for the theme
	 */
	boolean extDir(String dirName, String place);
	
	String findFirstFile(String fileName);

	boolean containFile(String fileName);

	boolean containDir(String dirName);

	boolean containFileUnderDir(String dirName);

	boolean extAllFiles();

	void recycle();
	
	public static class ExtUtilFactory {
		public static ExtUtil createExtUtil(String utilName) {
			if (utilName == "ZIPIS") {
				return new ZipStreamExtUtil();
			}
			return null;
		}
	}
}