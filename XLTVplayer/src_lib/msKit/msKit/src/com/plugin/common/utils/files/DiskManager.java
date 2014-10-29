/**
 * DiskManager.java
 */
package com.plugin.common.utils.files;

import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;

import android.text.TextUtils;

import com.plugin.common.utils.UtilsConfig;
import com.plugin.common.utils.UtilsRuntime;

/**
 * @author Guoqing Sun Oct 24, 20122:42:55 PM
 */
public class DiskManager {

    private static final boolean DEBUG = false && UtilsConfig.UTILS_DEBUG;

	private static String DISK_DIR = null;

    private static String DISK_TMP_DIR = null;

	private static final long MAX_DISK_SIZE = ((long) 50) * 1024 * 1024; // 100M , 20M for debug
	private static final long MAX_TMP_DISK_SIZE = ((long) 20) * 1024 * 1024; // 100M , 20M for debug
	private static final long MAX_FLASH_SIZE = ((long) 5) * 1024 * 1024; //5M

	public static enum DiskCacheType {
		INPUTSTREAM_BIG_FILE_CACHE,
        PICTURE,
        DOWNLOAD_AUDIO,
        CRASH_LOG,
        CRASH_DEBUG_LOG,
        AUDIO_RECORD,
        MQTT_TRACE,
        AUDIO_EFFECTS,
        TMP,
        BASE
	}

	public static void init() {
	    DISK_DIR = UtilsConfig.DISK_DIR_PATH_BINDING;
	    DISK_TMP_DIR = UtilsConfig.DISK_TMP_DIR_PATH_BINDING;
	}
	
	/**
	 * 尝试根据类型获取cache的路径, 注意不会判断该路径是否可用
	 * 
	 * @param type
	 * @return
	 */
	public static String tryToFetchCachePathByTypeBinding(DiskCacheType type) {
		String retDir = DISK_DIR;

		switch (type) {
		case INPUTSTREAM_BIG_FILE_CACHE:
			retDir = retDir + "big_file_cache/";
			break;
		case PICTURE:
			break;
		case DOWNLOAD_AUDIO:
			// 语音下载的路径
			if (FileUtil.isSDCardReady()) {
				retDir = retDir + "audio_download/";
			} else {
				retDir = UtilsConfig.DEVICE_INFO.flashDataPath;
			}
			break;
		case CRASH_LOG:
			retDir = retDir + "crash/";
			break;
		case CRASH_DEBUG_LOG:
			retDir = retDir + "debug_crash/";
			break;
		case AUDIO_RECORD:
			retDir = retDir + "sound_records/";
			break;
		case MQTT_TRACE:
			retDir = retDir + "push_trace/";
			break;
		case AUDIO_EFFECTS:
			retDir = retDir + "effects/";
			break;
        case TMP:
            retDir = DISK_TMP_DIR + "camera/";
            break;
		case BASE:
			break;
		}

		File dirCheck = new File(retDir);
		if (dirCheck.exists() && !dirCheck.isDirectory()) {
			dirCheck.delete();
		}

		if (!dirCheck.exists()) {
			dirCheck.mkdirs();
		}

		return retDir;
	}

	public static LinkedList<FileInfo> collectCrashLogs() {
		if (!UtilsConfig.RELEASE_UPLOAD_CRASH_LOG) {
			return null;
		}
		
		LinkedList<FileInfo> files = FileOperatorHelper
				.getFileInfoUnderDir(tryToFetchCachePathByTypeBinding(DiskCacheType.CRASH_LOG));

		if (DEBUG && files != null) {
			UtilsConfig.LOGD("=================== beign list tryToUploadCrash ==================");
			for (FileInfo info : files) {
				UtilsConfig.LOGD("        info : " + info.toString());
			}
			UtilsConfig.LOGD("=================== end list tryToUploadCrash ==================");
		}

		if (files != null && files.size() > 0) {
			// String fileName = "crash-" + time + "-" + timestamp + ".log";
			Collections.sort(files, new Comparator<FileInfo>() {
				@Override
				public int compare(FileInfo lhs, FileInfo rhs) {
					if (lhs == null || rhs == null) {
						return 0;
					}
					if (lhs.modifiedDate == rhs.modifiedDate) {
						return 0;
					} else if (lhs.modifiedDate > rhs.modifiedDate) {
						return -1;
					} else if (lhs.modifiedDate < rhs.modifiedDate) {
						return 1;
					}
					return 0;
				}
			});

			if (DEBUG) {
				UtilsConfig.LOGD("=================== beign tryToUploadCrash after sort file ==================");
				for (FileInfo info : files) {
					UtilsConfig.LOGD("        info : " + info.toString());
				}
				UtilsConfig.LOGD("=================== beign tryToUploadCrash end sort file ==================");
			}
		}
		
		return files;
	}
	
	/**
	 * 尝试上传以前的crash log，如果在wifi环境下，那么会尝试上传所有的crah log，如果在移动网络下，那么只尝试上传最近的
	 * 一个crash log，上传成功以后，删除此文件.
	 * 
	 */
//	@Deprecated
//	public static void tryToUploadCrash(Context context, boolean isWifi) {
//		if (!Config.RELEASE_UPLOAD_CRASH_LOG) {
//			return;
//		}
//
//		LinkedList<FileInfo> files = FileOperatorHelper
//				.getFileInfoUnderDir(tryToFetchCachePathByType(DiskCacheType.CRASH_LOG));
//
//		if (Config.UTILS_DEBUG && files != null) {
//			Config.LOGD("=================== beign list tryToUploadCrash ==================");
//			for (FileInfo info : files) {
//				Config.LOGD("        info : " + info.toString());
//			}
//			Config.LOGD("=================== end list tryToUploadCrash ==================");
//		}
//
//		if (files != null && files.size() > 0) {
//			// String fileName = "crash-" + time + "-" + timestamp + ".log";
//			Collections.sort(files, new Comparator<FileInfo>() {
//				@Override
//				public int compare(FileInfo lhs, FileInfo rhs) {
//					if (lhs == null || rhs == null) {
//						return 0;
//					}
//					if (lhs.modifiedDate == rhs.modifiedDate) {
//						return 0;
//					} else if (lhs.modifiedDate > rhs.modifiedDate) {
//						return -1;
//					} else if (lhs.modifiedDate < rhs.modifiedDate) {
//						return 1;
//					}
//					return 0;
//				}
//			});
//
//			if (Config.UTILS_DEBUG) {
//				Config.LOGD("=================== beign tryToUploadCrash after sort file ==================");
//				for (FileInfo info : files) {
//					Config.LOGD("        info : " + info.toString());
//				}
//				Config.LOGD("=================== beign tryToUploadCrash end sort file ==================");
//			}
//
//			if (!isWifi) {
//				// just update the first file
//				FileInfo upFileInfo = files.get(0);
//				try {
//					SYSUploadCrashLogRequest request = new SYSUploadCrashLogRequest.Builder(upFileInfo.fileName,
//							upFileInfo.filePath).create();
//					request.setIgnoreResult(true);
//					SYSUploadCrashLogResponse r = InternetUtils.request(context, request);
//					if (r != null && r.result == SYSUploadCrashLogResponse.RESULT_SUCCESS) {
//						FileOperatorHelper.DeleteFile(upFileInfo);
//					}
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//			} else {
//				// try to upload all crash log
//				try {
//					for (FileInfo info : files) {
//						SYSUploadCrashLogRequest request = new SYSUploadCrashLogRequest.Builder(info.fileName,
//								info.filePath).create();
//						request.setIgnoreResult(true);
//						SYSUploadCrashLogResponse r = InternetUtils.request(context, request);
//						if (r != null && r.result == SYSUploadCrashLogResponse.RESULT_SUCCESS) {
//							FileOperatorHelper.DeleteFile(info);
//						}
//					}
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//			}
//		}
//	}

	private static void tryToCleanDiskUnderDir(String fullPath, long maxSize) {
		UtilsConfig.LOGD("[[tryToCleanDiskUnderDir]] try to clean path : " + fullPath + " }}}}}}}} =================");

		if (!TextUtils.isEmpty(fullPath)) {
			File cleanDir = new File(fullPath);
			if (cleanDir.exists()) {
				long beginTime = System.currentTimeMillis();
				if (DEBUG) {
					UtilsConfig.LOGD("[[tryToCleanDisk]] entry +++++++++++++++ current time = "
							+ UtilsRuntime.debugFormatTime(beginTime));
				}

				long diskSize = FileOperatorHelper.getDirectorySize(new File(fullPath));

				if (DEBUG) {
					UtilsConfig.LOGD("          current disk Size = " + FileUtil.convertStorage(diskSize) + " max size = "
							+ FileUtil.convertStorage(maxSize) + " current time = "
							+ UtilsRuntime.debugFormatTime(System.currentTimeMillis()));
				}

				if (diskSize >= maxSize) {
					UtilsConfig.LOGD("          disk size > MaxSize, so begin clean the disk to 1.0 of MAX");
					LinkedList<FileInfo> fileList = FileOperatorHelper.getFileInfoUnderDir(new File(fullPath));
					if (DEBUG) {
						UtilsConfig.LOGD("------------------------------------------------");
						UtilsConfig.LOGD("  before sort list by time : file under " + fullPath + " list : ");
						if (fileList != null) {
							for (FileInfo info : fileList) {
								UtilsConfig.LOGD("          Info : " + info.toString());
							}
						}
						UtilsConfig.LOGD("------------------------------------------------");
					}

					if (fileList == null || fileList.size() == 0) {
						return;
					}

					Collections.sort(fileList, new Comparator<FileInfo>() {
						@Override
						public int compare(FileInfo lhs, FileInfo rhs) {
							if (lhs == null || rhs == null) {
								return 0;
							}
							if (lhs.modifiedDate == rhs.modifiedDate) {
								return 0;
							} else if (lhs.modifiedDate > rhs.modifiedDate) {
								return 1;
							} else if (lhs.modifiedDate < rhs.modifiedDate) {
								return -1;
							}

							return 0;
						}

					});

					if (DEBUG) {
						UtilsConfig.LOGD("-------------------============------------------");
						UtilsConfig.LOGD("  after sort list by time : file under " + fullPath + " list : ");
						for (FileInfo info : fileList) {
							UtilsConfig.LOGD("          Info : " + info.toString());
						}
						UtilsConfig.LOGD("-------------------============------------------");
					}

					long cleanSize = diskSize - ((long) (maxSize * 1.0 * 0.75));
					UtilsConfig.LOGD(" should delete size = " + FileUtil.convertStorage(cleanSize));

					LinkedList<FileInfo> cleanFileInfo = new LinkedList<FileInfo>();
					long curSize = 0;
					for (FileInfo info : fileList) {
						if (!info.isDir) {
							curSize += info.fileSize;
							if (UtilsConfig.UTILS_DEBUG) {
								UtilsConfig.LOGD("    add file [[" + info.toString() + "]] into delete list. delete Size = "
										+ FileUtil.convertStorage(curSize));
							}
							cleanFileInfo.add(info);
							if (curSize >= cleanSize) {
								break;
							}
						}
					}
					if (DEBUG) {
						UtilsConfig.LOGD(" curSize = " + FileUtil.convertStorage(curSize));
					}

					if (DEBUG) {
						UtilsConfig.LOGD("<><><><><><><><><><><><><><><><><><><><><><>");
						UtilsConfig.LOGD(" should clean file info : file under " + fullPath + " list : ");
						for (FileInfo info : cleanFileInfo) {
							UtilsConfig.LOGD("          Info : " + info.toString());
						}
						UtilsConfig.LOGD("<><><><><><><><><><><><><><><><><><><><><><>");
					}

					for (FileInfo info : cleanFileInfo) {
						UtilsConfig.LOGD("        delete file : " + info.toString());
						FileOperatorHelper.DeleteFile(info);
					}
				}

				if (DEBUG) {
					UtilsConfig.LOGD("[[tryToCleanDisk]] leave +++++++++++++++ current time = "
							+ UtilsRuntime.debugFormatTime(System.currentTimeMillis()) + " begin time = "
							+ UtilsRuntime.debugFormatTime(beginTime));
				}
			}
		}
	}

	public static void tryToCleanDisk() {
		tryToCleanDiskUnderDir(DISK_DIR, MAX_DISK_SIZE);
		tryToCleanDiskUnderDir(DISK_TMP_DIR, MAX_TMP_DISK_SIZE);
		tryToCleanDiskUnderDir(UtilsConfig.DEVICE_INFO.flashDataPath, MAX_FLASH_SIZE);
	}

}
