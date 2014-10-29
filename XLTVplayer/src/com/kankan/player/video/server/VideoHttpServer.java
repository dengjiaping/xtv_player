package com.kankan.player.video.server;

import android.text.TextUtils;
import com.kankan.player.app.AppConfig;
import com.kankan.player.util.SmbUtil;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileInputStream;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

public class VideoHttpServer extends NanoHTTPD {

    /**
     * Common mime type for dynamic content: binary
     */
    public static final String MIME_DEFAULT_BINARY = "application/octet-stream";

    /**
     * Hashtable mapping (String)FILENAME_EXTENSION -> (String)MIME_TYPE
     */
    private static final Map<String, String> MIME_TYPES = new HashMap<String, String>() {{
        put("css", "text/css");
        put("htm", "text/html");
        put("html", "text/html");
        put("xml", "text/xml");
        put("java", "text/x-java-source, text/java");
        put("md", "text/plain");
        put("txt", "text/plain");
        put("asc", "text/plain");
        put("gif", "image/gif");
        put("jpg", "image/jpeg");
        put("jpeg", "image/jpeg");
        put("png", "image/png");
        put("mp3", "audio/mpeg");
        put("m3u", "audio/mpeg-url");
        put("mp4", "video/mp4");
        put("mkv", "video/x-matroska");
        put("wmv", "video/x-ms-wmv");
        put("mpg", "video/mpeg");
        put("mpeg", "video/mpeg");
        put("avi", "video/x-msvideo");
        put("ogv", "video/ogg");
        put("flv", "video/x-flv");
        put("mov", "video/quicktime");
        put("swf", "application/x-shockwave-flash");
        put("js", "application/javascript");
        put("pdf", "application/pdf");
        put("doc", "application/msword");
        put("ogg", "application/x-ogg");
        put("zip", "application/octet-stream");
        put("exe", "application/octet-stream");
        put("class", "application/octet-stream");
    }};

    public VideoHttpServer(int port) {
        super(port);
    }

    public VideoHttpServer(String hostname, int port) {
        super(hostname, port);
    }

    /**
     * URL-encodes everything between "/"-characters. Encodes spaces as '%20' instead of '+'.
     */
    private String encodeUri(String uri) {
        String newUri = "";
        StringTokenizer st = new StringTokenizer(uri, "/ ", true);
        while (st.hasMoreTokens()) {
            String tok = st.nextToken();
            if (tok.equals("/"))
                newUri += "/";
            else if (tok.equals(" "))
                newUri += "%20";
            else {
                try {
                    newUri += URLEncoder.encode(tok, "UTF-8");
                } catch (UnsupportedEncodingException ignored) {
                }
            }
        }
        return newUri;
    }

    public Response serve(IHTTPSession session) {
        Map<String, String> header = session.getHeaders();
        Map<String, String> parms = session.getParms();
        String uri = session.getUri();

        if (AppConfig.DEBUG) {
            AppConfig.LOGD("[[VideoHttpServer]] serve request parameter:");
            AppConfig.LOGD("\t" + session.getMethod() + " '" + uri + "' ");
            AppConfig.LOGD("\t" + session.getQueryParameterString());

            Iterator<String> e = header.keySet().iterator();
            while (e.hasNext()) {
                String value = e.next();
                AppConfig.LOGD("\t  HDR: '" + value + "' = '" + header.get(value) + "'");
            }
            e = parms.keySet().iterator();
            while (e.hasNext()) {
                String value = e.next();
                AppConfig.LOGD("\t  PRM: '" + value + "' = '" + parms.get(value) + "'");
            }
        }

        Response res = null;
        if (!uri.startsWith(File.separator + SmbUtil.SCHEMA_SMB_PREFIX) || !parms.containsKey(SmbUtil.SMB_PLAY_KEY_PATH)) {
            res = getNotFoundResponse();
        }

        if (res == null) {
            String path = parms.get(SmbUtil.SMB_PLAY_KEY_PATH);
            if (TextUtils.isEmpty(path)) {
                res = getNotFoundResponse();
            }

            if (res == null) {
                try {
                    SmbFile smbFile = new SmbFile(path);
                    res = serveFile(uri, header, smbFile, getMimeTypeForFile(path));
                } catch (MalformedURLException e1) {
                    res = getForbiddenResponse("MalformedURLException " + e1.getMessage());
                }
            }
        }

        return res;
    }

    protected Response getForbiddenResponse(String s) {
        return createResponse(Response.Status.FORBIDDEN, NanoHTTPD.MIME_PLAINTEXT, "FORBIDDEN: "
                + s);
    }

    /**
     * Serves file from homeDir and its' subdirectories (only). Uses only URI, ignores all headers and HTTP parameters.
     */
    Response serveFile(String uri, Map<String, String> header, SmbFile smbFile, String mime) {
        AppConfig.LOGD("[[VideoHttpServer]] serveFile uri=" + uri);
        Response res;
        try {
            long fileLen = smbFile.length();
            // Calculate etag
            String etag = Integer.toHexString((smbFile.getCanonicalPath() + smbFile.getLastModified() + "" + fileLen).hashCode());
            AppConfig.LOGD("[[VideoHttpServer]] serveFile fileLen=" + fileLen);

            // Support (simple) skipping:
            long startFrom = 0;
            long endAt = -1;
            String range = header.get("range");
            if (range != null) {
                AppConfig.LOGD("[[VideoHttpServer]] serveFile range=" + range);
                if (range.startsWith("bytes=")) {
                    range = range.substring("bytes=".length());
                    int minus = range.indexOf('-');
                    try {
                        if (minus > 0) {
                            startFrom = Long.parseLong(range.substring(0, minus));
                            endAt = Long.parseLong(range.substring(minus + 1));
                        }
                    } catch (NumberFormatException ignored) {
                        AppConfig.LOGD("[[VideoHttpServer]] serveFile NumberFormatException:" + ignored.getMessage());
                    }
                    AppConfig.LOGD("[[VideoHttpServer]] serveFile startFrom=" + startFrom + ", endAt=" + endAt);
                }
            }

            // Change return code and add Content-Range header when skipping is requested
            if (range != null && startFrom >= 0) {
                if (startFrom >= fileLen) {
                    res = createResponse(Response.Status.RANGE_NOT_SATISFIABLE, NanoHTTPD.MIME_PLAINTEXT, "");
                    res.addHeader("Content-Range", "bytes 0-0/" + fileLen);
                    res.addHeader("ETag", etag);
                } else {
                    if (endAt < 0) {
                        endAt = fileLen - 1;
                    }
                    long newLen = endAt - startFrom + 1;
                    if (newLen < 0) {
                        newLen = 0;
                    }

                    SmbFileInputStream sfis = new SmbFileInputStream(smbFile);
                    sfis.skip(startFrom);

                    res = createResponse(Response.Status.PARTIAL_CONTENT, mime, new BufferedInputStream(sfis), newLen);
                    AppConfig.LOGD("[[VideoHttpServer]] serveFile create partial content response len=" + newLen);
                    res.addHeader("Content-Length", "" + newLen);
                    res.addHeader("Content-Range", "bytes " + startFrom + "-" + endAt + "/" + fileLen);
                    res.addHeader("ETag", etag);
                }
            } else {
                if (etag.equals(header.get("if-none-match")))
                    res = createResponse(Response.Status.NOT_MODIFIED, mime, "");
                else {
                    res = createResponse(Response.Status.OK, mime, new BufferedInputStream(new SmbFileInputStream(smbFile)), fileLen);
                    AppConfig.LOGD("[[VideoHttpServer]] serveFile create whole content response len=" + fileLen);
                    res.addHeader("Content-Length", "" + fileLen);
                    res.addHeader("ETag", etag);
                }
            }
        } catch (IOException ioe) {
            AppConfig.LOGD("[[VideoHttpServer]] serveFile read file IOException: " + ioe.getMessage());
            res = getForbiddenResponse("Reading file failed.");
        }

        return res;
    }

    // Get MIME type from file name extension, if possible
    private String getMimeTypeForFile(String uri) {
        AppConfig.LOGD("[[VideoHttpServer]] getMimeTypeForFile url=" + uri);
        int dot = uri.lastIndexOf('.');
        String mime = null;
        if (dot >= 0) {
            mime = MIME_TYPES.get(uri.substring(dot + 1).toLowerCase());
        }
        return mime == null ? MIME_DEFAULT_BINARY : mime;
    }

    protected Response getNotFoundResponse() {
        return createResponse(Response.Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT,
                "Error 404, file not found.");
    }

    // Announce that the file server accepts partial content requests
    private Response createResponse(Response.Status status, String mimeType, InputStream message, long dataLen) {
        Response res = new Response(status, mimeType, message, dataLen);
        res.addHeader("Accept-Ranges", "bytes");
        return res;
    }

    // Announce that the file server accepts partial content requests
    private Response createResponse(Response.Status status, String mimeType, String message) {
        Response res = new Response(status, mimeType, message);
        res.addHeader("Accept-Ranges", "bytes");
        return res;
    }

}
