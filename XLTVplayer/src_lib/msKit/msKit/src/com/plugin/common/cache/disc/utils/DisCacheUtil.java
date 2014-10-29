package com.plugin.common.cache.disc.utils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.text.TextUtils;

import com.plugin.common.cache.disc.DiscCacheOption;
import com.plugin.common.utils.LogUtil;

public class DisCacheUtil {

	

	/**
	 * 将bitmap保存为文件
	 * @param src
	 * @param saveFullPath
	 * @return
	 */
    public static boolean compressBitmapToFile(Bitmap src, File savedFile) {
        if (savedFile != null && src != null) {
            if (savedFile.exists()) {
                savedFile.delete();
            }

            try {
                FileOutputStream out = new FileOutputStream(savedFile);
                src.compress(Bitmap.CompressFormat.PNG, DiscCacheOption.BITMAP_COMPRESS_HIGH, out);
                out.close();

                return isBitmapData(savedFile.getAbsolutePath());
            } catch (Exception e) {
                e.printStackTrace();

            }
        }

        return false;
    }
    
    /**
     * 判断是否是bitmap文件类型
     * @param fileFullPath
     * @return
     */
    public static boolean isBitmapData(String fileFullPath) {
        if (!TextUtils.isEmpty(fileFullPath)) {
            try {
                BitmapFactory.Options opt = new BitmapFactory.Options();

                opt.inPurgeable = true;
                opt.inJustDecodeBounds = true;

                BitmapFactory.decodeFile(fileFullPath, opt);
                if (opt.outWidth > 0 && opt.outHeight > 0) {
                    return true;
                }
            } catch (Exception e) {
                e.printStackTrace();

            }
        }

        return false;
    }
	
	/**
	 * 写byte数组到文件
	 * 
	 * @param targetPath
	 * @param bytes
	 * @return
	 */
	public static String saveFileByBytes(File targetFile, byte[] bytes) {
		if (bytes == null || targetFile == null || targetFile.isDirectory()
				|| !targetFile.isAbsolute()) {
			return null;
		}

		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(targetFile);
			fos.write(bytes);
			fos.flush();
			return targetFile.getAbsolutePath();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (fos != null) {
					fos.close();
					fos = null;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return null;
	}

	/**
	 * 写输入流到文件
	 * 
	 * @param is
	 * @param targetFile
	 * @return
	 * @throws IOException
	 */
	public static String saveFileByStream(InputStream is, File targetFile) {
		if (is == null || targetFile == null || targetFile.isDirectory()
				|| !targetFile.isAbsolute()) {
			return null;
		}

		OutputStream os = null;
		try {
			targetFile.createNewFile();
			os = new BufferedOutputStream(new FileOutputStream(targetFile),
					DiscCacheOption.BUFFER_SIZE);
			copyStream(is, os);
			return targetFile.getAbsolutePath();
		} catch (IOException e) {
			e.printStackTrace();
		}finally{
			try {
				if(os != null){
					os.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		return null;
	}

	/**
	 * 输入流到输出流
	 * 
	 * @param is
	 * @param os
	 * @throws IOException
	 */
	public static void copyStream(InputStream is, OutputStream os)
			throws IOException {
		byte[] bytes = new byte[DiscCacheOption.BUFFER_SIZE];
		while (true) {
			int count = is.read(bytes, 0, DiscCacheOption.BUFFER_SIZE);
			if (count == -1) {
				break;
			}
			os.write(bytes, 0, count);
		}
	}
	
	  /**
     * 根据图片的全路径来获取一张图片，在获取图片的时候会对图片做就地压缩，同时会将图片 的旋转角度准换成0度。
     * 
     * @param fileFullPath
     * @return
     */
    public static Bitmap loadBitmapWithSizeOrientation(File file) {
        if (file == null) {
            return null;
        }
        return loadBitmapWithSizeCheck(file, com.plugin.common.utils.ExifHelper.getRotationFromExif(file.getAbsolutePath()));
    }
	
    public static Bitmap loadBitmapWithSizeCheck(File bitmapFile, int orientataion) {
        if (LogUtil.UTILS_DEBUG) {
            LogUtil.LOGD("load file from path = " + bitmapFile.getPath());
        }

        Bitmap bmp = null;
        FileInputStream fis = null;

        try {
            bitmapFile.setLastModified(System.currentTimeMillis());

            BitmapFactory.Options opt = new BitmapFactory.Options();
            opt.inPurgeable = true;
            opt.inJustDecodeBounds = true;

            BitmapFactory.decodeFile(bitmapFile.getAbsolutePath(), opt);
            int width = opt.outWidth;
            int height = opt.outHeight;

            BitmapFactory.Options newOpt = new BitmapFactory.Options();
            newOpt.inSampleSize = makeSample(bitmapFile, width, height);

            //check width and height with sample
            if (width > DiscCacheOption.MAX_BITMAP_WIDTH || height > DiscCacheOption.MAX_BITMAP_HEIGHT) {
                int max = width > height ? width : height;
                int maxOrigin = max;
                int curSample = newOpt.inSampleSize;
                max = maxOrigin / curSample;
                while (max > DiscCacheOption.MAX_BITMAP_WIDTH) {
                    curSample = curSample * 2;
                    max = maxOrigin / curSample;
                }
                newOpt.inSampleSize = curSample;
            }

            // newOpt.inScaled = true;
            newOpt.inPurgeable = true;
            newOpt.inInputShareable = true;

            newOpt.outHeight = height;
            newOpt.outWidth = width;
            fis = new FileInputStream(bitmapFile);

            long curTime = System.currentTimeMillis();
            if (LogUtil.UTILS_DEBUG) {

            	LogUtil.LOGD("<<<<< reused bitmap >>>>##### begin decode the bitmap file : " + bitmapFile + " #####");
                
            }
            bmp = BitmapFactory.decodeStream(fis, null, newOpt);
            if (LogUtil.UTILS_DEBUG) {
                long cost = System.currentTimeMillis() - curTime;
 
                LogUtil.LOGD("<<<<< reused bitmap >>>> ***** end decode the bitmap file : " + bitmapFile + " ***** cost time : " + cost + "ms");
                
            }

            if (orientataion != 0 && bmp != null) {
                Matrix matrix = new Matrix();
                matrix.postRotate((float) orientataion);
                Bitmap tmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);
                if (tmp != null) {
                    bmp.recycle();
                    bmp = null;
                    bmp = tmp;
                }
                if (LogUtil.UTILS_DEBUG) {
                	LogUtil.LOGD("[[loadBitmapWithSizeCheckAndBitmapReuse]] rotation = <<<<<<<<<<<< " + orientataion + " >>>>>>>>>>>>>");
                }
            }

            return bmp;
        } catch (Exception e) {
            e.printStackTrace();
            if (LogUtil.UTILS_DEBUG) {
            	LogUtil.LOGD("Exception : ", e);
            }
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
            if (LogUtil.UTILS_DEBUG) {
            	LogUtil.LOGD("Exception : ", e);
            }
            //TODO
//            OOM_COUNTER++;
//            if (OOM_COUNTER > 5) {
//                /**
//                 * 做一个释放资源的尝试
//                 */
//                OOM_COUNTER = 0;
//                CacheFactory.getCacheManager(CacheFactory.TYPE_CACHE.TYPE_IMAGE).releaseAllResource();
//                if (LogUtil.UTILS_DEBUG) {
//                    UtilsConfig.LOGD("[[loadBitmapWithSizeCheckAndBitmapReuse]] ********* Hey, this is a force release for BITMAP release, because OOM > 5");
//                }
//            }
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                    fis = null;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                if (LogUtil.UTILS_DEBUG) {
                	LogUtil.LOGD("Exception : ", ex);
                }
            }
        }
        return null;
    }
    
    private static int makeSample(File srcBitmap, int srcBtWidth, int srcBtHeight) {
        if (LogUtil.UTILS_DEBUG) {
        	LogUtil.LOGD("source bitmap size : width =  " + srcBtWidth + " height = " + srcBtHeight + " bitmap size : "
                    + convertStorage(srcBitmap.length()));
        }

        long fileMemorySize = srcBtWidth * srcBtHeight * 4;
        int sample = 1;
        if (fileMemorySize <= DiscCacheOption.MAX_MEMORY_SIZE) {
            sample = 1;
        } else if (fileMemorySize <= DiscCacheOption.MAX_MEMORY_SIZE * 4) {
            sample = 2;
        } else {
            long times = fileMemorySize / DiscCacheOption.MAX_MEMORY_SIZE;
            sample = (int) (Math.log(times) / Math.log(2.0)) + 1;
            int inSampleScale = (int) (Math.log(sample) / Math.log(2.0));
            sample = (int) Math.scalb(1, inSampleScale);

            long curFileMemorySize = (srcBtWidth / sample) * (srcBtHeight / sample) * 4;
            if (curFileMemorySize > DiscCacheOption.MAX_MEMORY_SIZE) {
                sample = sample * 2;
            }
        }

        if (sample == 1 && (srcBtWidth > DiscCacheOption.MAX_BITMAP_WIDTH || srcBtHeight > DiscCacheOption.MAX_BITMAP_WIDTH)) {
            sample = 2;
        }

        return sample;
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
	
    /**
     * 将src所指向的文件copy到targetFullPath所指向的文件，只支持文件copy，不支持文件夹copy
     * 
     * @param src
     * @param targetFullPath
     * @return
     */
    public static String copyFile(String src, String targetFullPath) {
        File file = new File(src);
        if (!file.exists() || file.isDirectory()) {
            return null;
        }
        FileInputStream fi = null;
        FileOutputStream fo = null;
        try {
            fi = new FileInputStream(file);
            File targetDir = new File(targetFullPath).getParentFile();
            if (targetDir == null) {
            	return null;
            }
            if (!targetDir.exists()) {
                if (!targetDir.mkdirs())
                    return null;
            }

            File targetFile = new File(targetFullPath);
            if (targetFile.exists()) {
            	targetFile.delete();
            }

            if (!targetFile.createNewFile()) {
                return null;
            }

            fo = new FileOutputStream(targetFile);
            int count = 102400;
            byte[] buffer = new byte[count];
            int read = 0;
            while ((read = fi.read(buffer, 0, count)) != -1) {
                fo.write(buffer, 0, read);
            }

            // TODO: set access privilege

            return targetFullPath;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fi != null)
                    fi.close();
                if (fo != null)
                    fo.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return null;
    }
}
