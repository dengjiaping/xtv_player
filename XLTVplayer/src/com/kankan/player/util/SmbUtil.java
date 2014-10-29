package com.kankan.player.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.text.TextUtils;
import android.text.format.Formatter;
import com.kankan.player.app.AppConfig;
import com.kankan.player.video.server.HttpService;
import com.plugin.common.utils.files.FileUtil;
import jcifs.Config;
import jcifs.UniAddress;
import jcifs.smb.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.*;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class SmbUtil {
    public static final String SCHEMA_HTTP = "http://";
    public static final String SCHEMA_SMB = "smb://";
    public static final String SCHEMA_SMB_PREFIX = "smb";
    public static final String SMB_PLAY_KEY_PATH = "path";
    public static final String ROUTER_NAME_DEFAULT = "路由器";

    public static final String ROUTER_XIAOMI = "XIAOMI";
    public static final String ROUTER_XUNLEI = "XUNLEIROUTER";

    public static final Map<String, String> ROUTER_NAMES = new HashMap<String, String>();

    static {
        ROUTER_NAMES.put(null, ROUTER_NAME_DEFAULT);
        ROUTER_NAMES.put(ROUTER_XIAOMI, "小米路由器");
        ROUTER_NAMES.put(ROUTER_XUNLEI, "迅雷路由器");

        //超时时间设置长一些
        Config.setProperty("jcifs.smb.client.responseTimeout", "120000");
        Config.setProperty("jcifs.smb.client.soTimeout", "120000");
        Config.setProperty("resolveOrder", "DNS");
    }

    public static String getRouterIp(Context context) {
        // 优先判断wifi地址
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();
        int ipAddress = 0;
        String routerIp = null;
        if (dhcpInfo != null) {
            ipAddress = dhcpInfo.gateway;
            routerIp = Formatter.formatIpAddress(ipAddress);
        }

        // 如果无线网没连接，那么判断有线网，这里用eth0可能有适配问题，碰到问题再增加
        if (ipAddress == 0) {
            try {
                AppConfig.LOGD("[[SmbUtil]] getRouterIp wifi is not available ");
                for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                    NetworkInterface interf = en.nextElement();
                    AppConfig.LOGD("[[SmbUtil]] getRouterIp network display name=" + interf.getDisplayName());
                    if ("eth0".equals(interf.getDisplayName())) {
                        for (Enumeration<InetAddress> addresses = interf.getInetAddresses(); addresses.hasMoreElements(); ) {
                            String addr = addresses.nextElement().getHostAddress();
                            AppConfig.LOGD("[[SmbUtil]] getRouterIp HostAddress=" + addr);
                            if (!TextUtils.isEmpty(addr) && addr.indexOf(".") != -1) {
                                int index = addr.lastIndexOf(".");
                                routerIp = addr.substring(0, index + 1) + "1";
                            }
                        }
                    }
                }
            } catch (SocketException e) {
            }
        }

        return routerIp;
    }

    public static String isSmbServerExists(Context context) {
        String routerIpAddress = getRouterIp(context);
        AppConfig.LOGD("[[SmbUtil]] isSmbServerExists routerIpAddress=" + routerIpAddress);
        try {
            SmbFile smbRootDir = new SmbFile(new StringBuilder(SCHEMA_SMB).append(routerIpAddress).append(File.separator).toString());
            smbRootDir.connect();
            if(smbRootDir != null){
                SettingManager.getInstance().setRouterSmbRootPath(smbRootDir.getPath());
            }
            return smbRootDir.getPath();
        } catch (MalformedURLException e) {
            AppConfig.LOGD("[[SmbUtil]] isSmbServerExists MalformedURLException: " + e.getMessage());
        } catch (IOException e) {
            AppConfig.LOGD("[[SmbUtil]] isSmbServerExists IOException: " + e.getMessage());
        }

        return null;
    }

    public static boolean isSmbPath(String path) {
        if (TextUtils.isEmpty(path)) {
            return false;
        }

        return path.startsWith(SCHEMA_SMB);
    }

    public static String generateSmbPlayPath(String path) {
        StringBuilder sb = new StringBuilder();
        sb.append(SCHEMA_HTTP);
        sb.append("127.0.0.1:");
        sb.append(SettingManager.getInstance().getVideoServerPort());
        sb.append(File.separator);
        sb.append(SCHEMA_SMB_PREFIX);
        sb.append("?");
        sb.append(SMB_PLAY_KEY_PATH);
        sb.append("=");
        sb.append(Uri.encode(path));
        return sb.toString();
    }

    public static String getSmbPathFromPlayUrl(String url) {
        int index = url.indexOf('=');
        return Uri.decode(url.substring(index + 1));
    }

    public static boolean isSmbPlayUrl(String url) {
        if (TextUtils.isEmpty(url)) {
            return false;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(SCHEMA_HTTP);
        sb.append("127.0.0.1:");
        sb.append(SettingManager.getInstance().getVideoServerPort());
        sb.append(File.separator);
        sb.append(SCHEMA_SMB_PREFIX);
        sb.append("?");
        sb.append(SMB_PLAY_KEY_PATH);
        sb.append("=");
        return url.startsWith(sb.toString());
    }

    public static String getRouterName(Context context) {
        String routerName = null;
        String routerIp = getRouterIp(context);
        try {
            UniAddress address = UniAddress.getByName(routerIp);
            String firstCalledName = address.firstCalledName();
            String nextCalledName = address.nextCalledName();
            AppConfig.logRemote("[[SmbUtil]] getRouterName firstCalledName=" + firstCalledName);
            AppConfig.logRemote("[[SmbUtil]] getRouterName nextCalledName=" + nextCalledName);
            SettingManager.getInstance().setRouterName(nextCalledName);
        } catch (UnknownHostException e) {
        }

        return getRouterDisplayName(routerName);
    }

    public static String getRouterDisplayName(String routerName) {
        if (routerName == null) {
            routerName = SettingManager.getInstance().getRouterName();
        }

        if (!ROUTER_NAMES.containsKey(routerName)) {
            routerName = null;
        }
        return ROUTER_NAMES.get(routerName);
    }

    public static SmbFile[] listFiles(SmbFile f, SmbFileFilter filter) {
        try {
            return f.listFiles(filter);
        } catch (SmbException e) {
            AppConfig.LOGD("[[SmbUtil]] listFiles " + e.getMessage());
        }

        return null;
    }

    public static boolean downloadSmbFile(String localPath, String smbPath) {
        SmbFileInputStream sfis = null;
        FileOutputStream fos = null;
        try {
            SmbFile smbFile = new SmbFile(smbPath);
            sfis = new SmbFileInputStream(smbFile);
            fos = new FileOutputStream(new File(localPath));
            byte[] buffer = new byte[8096];
            int length = 0;
            while ((length = sfis.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }
            fos.flush();
            return true;
        } catch (MalformedURLException e) {
            AppConfig.LOGD("[[SmbUtil]] downloadSmbFile error:" + e.getMessage());
        } catch (UnknownHostException e) {
            AppConfig.LOGD("[[SmbUtil]] downloadSmbFile error:" + e.getMessage());
        } catch (SmbException e) {
            AppConfig.LOGD("[[SmbUtil]] downloadSmbFile error:" + e.getMessage());
        } catch (FileNotFoundException e) {
            AppConfig.LOGD("[[SmbUtil]] downloadSmbFile error:" + e.getMessage());
        } catch (IOException e) {
            AppConfig.LOGD("[[SmbUtil]] downloadSmbFile error:" + e.getMessage());
        } finally {
            try {
                if (sfis != null) {
                    sfis.close();
                }
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
            }
        }

        return false;
    }

    public static String getSmbCacheDir(Context context) {
        File f = new File(context.getExternalCacheDir(), SCHEMA_SMB_PREFIX);
        if (!f.exists()) {
            f.mkdir();
        }
        return f.getAbsolutePath();
    }

    public static String getSmbApkFullPath(Context context, String apkPath) {
        return getSmbCacheDir(context) + File.separator + FileUtil.getNameFromFilepath(apkPath);
    }
}