/**
 * FileUtil.java
 */
package com.plugin.common.utils.files;

import java.io.File;
import java.io.FilenameFilter;

import android.os.Environment;

/**
 * @author Guoqing Sun Oct 24, 201211:21:42 AM
 */
public class FileUtil {

	public static final String ROOT_PATH = "/";
	public static final String SDCARD_PATH = ROOT_PATH + "sdcard";

	private static final String ANDROID_SECURE = "/mnt/sdcard/.android_secure";

	public static boolean isSDCardReady() {
		return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
	}

	// if path1 contains path2
	public static boolean containsPath(String path1, String path2) {
		String path = path2;
		while (path != null) {
			if (path.equalsIgnoreCase(path1))
				return true;

			if (path.equals(ROOT_PATH))
				break;
			path = new File(path).getParent();
		}

		return false;
	}

	public static FileInfo getFileInfo(String filePath) {
		File lFile = new File(filePath);
		if (!lFile.exists())
			return null;

		return getFileInfo(lFile);
	}

	public static FileInfo getFileInfo(File lFile) {

		FileInfo lFileInfo = new FileInfo();
		lFileInfo.canRead = lFile.canRead();
		lFileInfo.canWrite = lFile.canWrite();
		lFileInfo.isHidden = lFile.isHidden();
		lFileInfo.fileName = getNameFromFilepath(lFile.getAbsolutePath());
		lFileInfo.modifiedDate = lFile.lastModified();
		lFileInfo.isDir = lFile.isDirectory();
		lFileInfo.filePath = lFile.getAbsolutePath();
		lFileInfo.fileSize = lFile.length();
		return lFileInfo;
	}

	public static FileInfo getFileInfo(File f, FilenameFilter filter, boolean showHidden) {
		FileInfo lFileInfo = new FileInfo();
		String filePath = f.getPath();
		File lFile = new File(filePath);
		lFileInfo.canRead = lFile.canRead();
		lFileInfo.canWrite = lFile.canWrite();
		lFileInfo.isHidden = lFile.isHidden();
		lFileInfo.fileName = f.getName();
		lFileInfo.modifiedDate = lFile.lastModified();
		lFileInfo.isDir = lFile.isDirectory();
		lFileInfo.filePath = filePath;
		if (lFileInfo.isDir) {
			int lCount = 0;
			File[] files = lFile.listFiles(filter);

			// null means we cannot access this dir
			if (files == null) {
				return null;
			}

			for (File child : files) {
				if ((!child.isHidden() || showHidden) && isNormalFile(child.getAbsolutePath())) {
					lCount++;
				}
			}
			lFileInfo.count = lCount;

		} else {

			lFileInfo.fileSize = lFile.length();

		}
		return lFileInfo;
	}

	public static String makePath(String path1, String path2) {
		if (path1.endsWith(File.separator))
			return path1 + path2;

		return path1 + File.separator + path2;
	}

	public static String getSdDirectory() {
		return Environment.getExternalStorageDirectory().getPath();
	}

	public static boolean isNormalFile(String fullName) {
		return !fullName.equals(ANDROID_SECURE);
	}

	public static String getAudioFeedName(long feedId) {
		return String.valueOf(feedId) + "_main_feed_mp3";
	}

	public static String getCommentFeedName(long commentId) {
		return String.valueOf(commentId) + "_comment_feed_mp3";
	}

	public static String getTagName(long activityTagId) {
		return String.valueOf(activityTagId) + "_activityTagId_feed_mp3";
	}

	public static String getExtFromFilename(String filename) {
		int dotPosition = filename.lastIndexOf('.');
		if (dotPosition != -1) {
			return filename.substring(dotPosition + 1, filename.length());
		}
		return "";
	}

	public static String getNameFromFilename(String filename) {
		int dotPosition = filename.lastIndexOf('.');
		if (dotPosition != -1) {
			return filename.substring(0, dotPosition);
		}
		return "";
	}

	public static String getPathFromFilepath(String filepath) {
		int pos = filepath.lastIndexOf('/');
		if (pos != -1) {
			return filepath.substring(0, pos);
		}
		return "";
	}

	public static String getNameFromFilepath(String filepath) {
		int pos = filepath.lastIndexOf('/');
		if (pos != -1) {
			return filepath.substring(pos + 1);
		}
		return "";
	}

	// storage, G M K B
	public static String convertStorage(long size) {
		long kb = 1024;
		long mb = kb * 1024;
		long gb = mb * 1024;

		if (size >= gb) {
			return String.format("%.1f GB", (float) size / gb);
		} else if (size >= mb) {
			float f = (float) size / mb;
			return String.format(f > 100 ? "%.0f MB" : "%.1f MB", f);
		} else if (size >= kb) {
			float f = (float) size / kb;
			return String.format(f > 100 ? "%.0f KB" : "%.1f KB", f);
		} else
			return String.format("%d B", size);
	}

	public static class SDCardInfo {
		public long total;

		public long free;
	}

	public static SDCardInfo getSDCardInfo() {
		String sDcString = android.os.Environment.getExternalStorageState();

		if (sDcString.equals(android.os.Environment.MEDIA_MOUNTED)) {
			File pathFile = android.os.Environment.getExternalStorageDirectory();

			try {
				android.os.StatFs statfs = new android.os.StatFs(pathFile.getPath());

				// 获取SDCard上BLOCK总数
				long nTotalBlocks = statfs.getBlockCount();

				// 获取SDCard上每个block的SIZE
				long nBlocSize = statfs.getBlockSize();

				// 获取可供程序使用的Block的数量
				long nAvailaBlock = statfs.getAvailableBlocks();

				// 获取剩下的所有Block的数量(包括预留的一般程序无法使用的块)
				long nFreeBlock = statfs.getFreeBlocks();

				SDCardInfo info = new SDCardInfo();
				// 计算SDCard 总容量大小MB
				info.total = nTotalBlocks * nBlocSize;

				// 计算 SDCard 剩余大小MB
				info.free = nAvailaBlock * nBlocSize;

				return info;
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			}
		}

		return null;
	}
}
