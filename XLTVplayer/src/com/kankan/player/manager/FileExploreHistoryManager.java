package com.kankan.player.manager;

import android.content.Context;
import com.kankan.player.app.AppConfig;
import com.kankan.player.dao.model.DaoSession;
import com.kankan.player.dao.model.FileExploreHistory;
import com.kankan.player.dao.model.FileExploreHistoryDao;
import com.kankan.player.explorer.FileItem;
import com.kankan.player.item.DeviceItem;
import com.kankan.player.util.DaoUtils;
import com.plugin.common.utils.UtilsConfig;

import java.util.List;

public class FileExploreHistoryManager {

    private final Context mContext;
    private FileExploreHistoryDao mDao;

    public FileExploreHistoryManager(Context context) {
        mContext = context;
        DaoSession session = DaoUtils.getDaoSession(context);
        mDao = session.getFileExploreHistoryDao();
    }

    public void addFileToExploreHistory(FileItem fileItem, DeviceItem deviceItem) {
        if (fileItem == null || deviceItem == null || fileItem.filePath == null || deviceItem.getPath() == null) {
            return;
        }

        UtilsConfig.LOGD("[[addFileToExploreHistory]]\n\tfileItem:\n\t" + fileItem + "\n\tdeviceItem:\n\t" + deviceItem);

        DeviceItem.DeviceType deviceType = deviceItem.getType();
        if (deviceType == DeviceItem.DeviceType.EXTERNAL || deviceType == DeviceItem.DeviceType.HHD
                || deviceType == DeviceItem.DeviceType.USB) {
            if (isFileNew(fileItem, deviceItem) && fileItem.filePath.startsWith(deviceItem.getPath())) {
                FileExploreHistory history = new FileExploreHistory();
                history.setFileCategory(fileItem.category.ordinal());
                history.setFilePath(fileItem.filePath.substring(deviceItem.getPath().length()));
                history.setFileSize(fileItem.fileSize);
                history.setLastModifyTime(fileItem.lastModifyTime);
                history.setDeviceName(deviceItem.getName());
                history.setDeviceType(deviceItem.getType().ordinal());
                history.setDeviceSize(deviceItem.getSize());
                history.setDevicePath(deviceItem.getPath());

                mDao.insertOrReplace(history);
            }
        } else if (deviceType == DeviceItem.DeviceType.TD_DOWNLOAD) {
            if (isFileNew(fileItem, deviceItem)) {
                FileExploreHistory history = new FileExploreHistory();
                history.setFileCategory(fileItem.category.ordinal());
                history.setFilePath(fileItem.filePath);
                history.setFileSize(fileItem.fileSize);
                history.setLastModifyTime(fileItem.lastModifyTime);
                history.setCid(fileItem.cid);

                mDao.insertOrReplace(history);
            }
        } else if (deviceType == DeviceItem.DeviceType.XL_ROUTER) {
            if (isFileNew(fileItem, deviceItem)) {
                FileExploreHistory history = new FileExploreHistory();
                history.setFileCategory(fileItem.category.ordinal());
                history.setFilePath(fileItem.filePath);
                history.setFileSize(fileItem.fileSize);
                history.setLastModifyTime(fileItem.lastModifyTime);
                history.setDeviceName(deviceItem.getName());
                history.setDeviceType(deviceItem.getType().ordinal());
                history.setDeviceSize(deviceItem.getSize());
                history.setDevicePath(deviceItem.getPath());

                mDao.insertOrReplace(history);
            }
        }else if(deviceType == DeviceItem.DeviceType.XL_ROUTER_TDDOWNLOAD){
            if (isFileNew(fileItem, deviceItem)) {
                FileExploreHistory history = new FileExploreHistory();
                history.setFileCategory(fileItem.category.ordinal());
                history.setFilePath(fileItem.filePath);
                history.setFileSize(fileItem.fileSize);
                history.setLastModifyTime(fileItem.lastModifyTime);
                history.setCid(fileItem.cid);
                mDao.insertOrReplace(history);
            }
        }
    }

    /**
     * 这里我们用deviceName, deviceType, deviceSize三个参数来决定设备，虽然逻辑有问题，但是就先这么着吧
     * <p/>
     * 如果是本地文件或者移动设备上的文件，那么我们取相对根目录的路径保存起来，以后判断的时候只判断这个相对路径
     * 而不是去匹配全路径，可以解决移动设备路径不断变化的问题
     *
     * @param fileItem
     * @param deviceItem
     * @return
     */
    public boolean isFileNew(FileItem fileItem, DeviceItem deviceItem) {
        if (fileItem == null || deviceItem == null) {
            return true;
        }

        UtilsConfig.LOGD("[[isFileNew]]\n\tfileItem:\n\t" + fileItem + "\n\tdeviceItem:\n\t" + deviceItem);

        DeviceItem.DeviceType deviceType = deviceItem.getType();
        if (deviceType == DeviceItem.DeviceType.EXTERNAL || deviceType == DeviceItem.DeviceType.HHD
                || deviceType == DeviceItem.DeviceType.USB) {
            if (fileItem.filePath.startsWith(deviceItem.getPath())) {
                String fileRelativePath = fileItem.filePath.substring(deviceItem.getPath().length());
                List<FileExploreHistory> histories = mDao.queryBuilder().where(FileExploreHistoryDao.Properties.FilePath.eq(fileRelativePath),
                        FileExploreHistoryDao.Properties.FileCategory.eq(fileItem.category.ordinal()),
                        FileExploreHistoryDao.Properties.DeviceType.eq(deviceItem.getType().ordinal()),
                        FileExploreHistoryDao.Properties.LastModifyTime.eq(fileItem.lastModifyTime),
                        FileExploreHistoryDao.Properties.FileSize.eq(fileItem.fileSize),
                        FileExploreHistoryDao.Properties.DeviceSize.eq(deviceItem.getSize())
                ).list();

                return histories.size() == 0;
            }
        } else if (deviceType == DeviceItem.DeviceType.TD_DOWNLOAD) {
            //现在是ftp，以后换成了smb要改造
            List<FileExploreHistory> histories = mDao.queryBuilder().where(FileExploreHistoryDao.Properties.FilePath.eq(fileItem.filePath),
                    FileExploreHistoryDao.Properties.FileCategory.eq(fileItem.category.ordinal()),
                    FileExploreHistoryDao.Properties.LastModifyTime.eq(fileItem.lastModifyTime),
                    FileExploreHistoryDao.Properties.FileSize.eq(fileItem.fileSize),
                    FileExploreHistoryDao.Properties.Cid.eq(fileItem.cid)
            ).list();

            return histories.size() == 0;
        } else if (deviceType == DeviceItem.DeviceType.XL_ROUTER) {
            //现在是ftp，以后换成了smb要改造
            List<FileExploreHistory> histories = mDao.queryBuilder().where(FileExploreHistoryDao.Properties.FilePath.eq(fileItem.filePath),
                    FileExploreHistoryDao.Properties.FileCategory.eq(fileItem.category.ordinal()),
                    FileExploreHistoryDao.Properties.DeviceName.eq(deviceItem.getName()),
                    FileExploreHistoryDao.Properties.DeviceType.eq(deviceItem.getType().ordinal()),
                    FileExploreHistoryDao.Properties.LastModifyTime.eq(fileItem.lastModifyTime),
                    FileExploreHistoryDao.Properties.FileSize.eq(fileItem.fileSize),
                    FileExploreHistoryDao.Properties.DeviceSize.eq(deviceItem.getSize())
            ).list();

            return histories.size() == 0;
        }else if(deviceType == DeviceItem.DeviceType.XL_ROUTER_TDDOWNLOAD){
            List<FileExploreHistory> histories = mDao.queryBuilder().where(FileExploreHistoryDao.Properties.FilePath.eq(fileItem.filePath),
                    FileExploreHistoryDao.Properties.FileCategory.eq(fileItem.category.ordinal()),
                    FileExploreHistoryDao.Properties.LastModifyTime.eq(fileItem.lastModifyTime),
                    FileExploreHistoryDao.Properties.FileSize.eq(fileItem.fileSize),
                    FileExploreHistoryDao.Properties.Cid.eq(fileItem.cid)
            ).list();

            return histories.size() == 0;
        }

        return true;
    }

}
