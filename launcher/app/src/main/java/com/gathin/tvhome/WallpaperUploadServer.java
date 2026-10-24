package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class WallpaperUploadServer {
    private static final String TAG = "TvHome.UploadServer";

    public interface UploadListener {
        void onUploaded(File imageFile);
        default void onDeleted(File file) {}
        default void onSelected(File file) {}
        default void onSlideshowMode() {}
        default void onCleared() {}
    }

    public interface OnWallpaperUploadedListener extends UploadListener {}

    private final Context context;
    private final int port;
    private final UploadListener listener;
    private ServerSocket serverSocket;
    private volatile boolean running = false;
    private Thread serverThread;

    public WallpaperUploadServer(Context context, int port, UploadListener listener) {
        this.context = context.getApplicationContext();
        this.port = port;
        this.listener = listener;
    }

    public static String getLocalIpAddress() {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            Collections.sort(interfaces, (a, b) -> {
                String an = a.getName().toLowerCase(Locale.ROOT);
                String bn = b.getName().toLowerCase(Locale.ROOT);
                if (an.startsWith("wlan") || an.startsWith("eth")) return -1;
                if (bn.startsWith("wlan") || bn.startsWith("eth")) return 1;
                return an.compareTo(bn);
            });
            for (NetworkInterface nif : interfaces) {
                if (nif.isLoopback() || !nif.isUp()) continue;
                List<InetAddress> addrs = Collections.list(nif.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress() && addr instanceof Inet4Address) {
                        String ip = addr.getHostAddress();
                        if (ip != null && !ip.startsWith("127.")) {
                            return ip;
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Error resolving IP: " + e.getMessage());
        }
        return "127.0.0.1";
    }

    public synchronized boolean start() {
        if (running) return true;
        try {
            serverSocket = new ServerSocket();
            serverSocket.setReuseAddress(true);
            serverSocket.bind(new InetSocketAddress(port));
            running = true;

            serverThread = new Thread(() -> {
                Log.i(TAG, "Server started on port " + port);
                while (running && !serverSocket.isClosed()) {
                    try {
                        Socket client = serverSocket.accept();
                        new Thread(() -> handleClient(client), "upload-client").start();
                    } catch (Exception e) {
                        if (!running) break;
                        Log.w(TAG, "Accept error: " + e.getMessage());
                    }
                }
            }, "wallpaper-http-server");
            serverThread.setDaemon(true);
            serverThread.start();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to start server on port " + port + ": " + e.getMessage());
            running = false;
            return false;
        }
    }

    public synchronized void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (Exception ignored) {}
        if (serverThread != null) {
            serverThread.interrupt();
            serverThread = null;
        }
        Log.i(TAG, "Server stopped");
    }

    private void handleClient(Socket client) {
        try {
            client.setSoTimeout(30000); // 30 seconds timeout
            InputStream in = client.getInputStream();
            OutputStream out = client.getOutputStream();

            ByteArrayOutputStream headerBuf = new ByteArrayOutputStream();
            int prev3 = 0, prev2 = 0, prev1 = 0, b;
            while ((b = in.read()) != -1) {
                headerBuf.write(b);
                if (prev3 == '\r' && prev2 == '\n' && prev1 == '\r' && b == '\n') {
                    break;
                }
                prev3 = prev2;
                prev2 = prev1;
                prev1 = b;
            }

            String headerText = headerBuf.toString("UTF-8");
            String[] lines = headerText.split("\r\n");
            if (lines.length == 0 || lines[0].isEmpty()) {
                client.close();
                return;
            }

            String requestLine = lines[0];
            Log.d(TAG, "Request: " + requestLine);

            if (requestLine.startsWith("OPTIONS")) {
                String response = "HTTP/1.1 204 No Content\r\n" +
                        "Access-Control-Allow-Origin: *\r\n" +
                        "Access-Control-Allow-Methods: GET, POST, OPTIONS\r\n" +
                        "Access-Control-Allow-Headers: Content-Type\r\n" +
                        "Connection: close\r\n\r\n";
                out.write(response.getBytes("UTF-8"));
                out.flush();
            } else if (requestLine.startsWith("GET /thumb")) {
                String fileName = getQueryParam(requestLine, "file");
                serveThumbnail(fileName, out);
            } else if (requestLine.startsWith("GET /api/list")) {
                serveApiList(out);
            } else if (requestLine.startsWith("POST /api/select")) {
                String fileName = getQueryParam(requestLine, "file");
                handleApiSelect(fileName, out);
            } else if (requestLine.startsWith("POST /api/delete")) {
                String fileName = getQueryParam(requestLine, "file");
                handleApiDelete(fileName, out);
            } else if (requestLine.startsWith("POST /api/slideshow")) {
                handleApiSlideshow(out);
            } else if (requestLine.startsWith("POST /api/clear")) {
                handleApiClear(out);
            } else if (requestLine.startsWith("GET")) {
                byte[] pageBytes = getHtmlPage().getBytes("UTF-8");
                String response = "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: text/html; charset=UTF-8\r\n" +
                        "Content-Length: " + pageBytes.length + "\r\n" +
                        "Access-Control-Allow-Origin: *\r\n" +
                        "Connection: close\r\n\r\n";
                out.write(response.getBytes("UTF-8"));
                out.write(pageBytes);
                out.flush();
            } else if (requestLine.startsWith("POST /upload")) {
                long contentLength = 0;
                for (String line : lines) {
                    if (line.toLowerCase(Locale.ROOT).startsWith("content-length:")) {
                        try {
                            contentLength = Long.parseLong(line.substring(15).trim());
                        } catch (Exception ignored) {}
                        break;
                    }
                }

                if (contentLength > 0 && contentLength < 60 * 1024 * 1024) { // Max 60 MB
                    File customDir = new File(context.getFilesDir(), "custom_wallpapers");
                    if (!customDir.exists()) customDir.mkdirs();

                    File targetFile = new File(customDir, "custom_" + System.currentTimeMillis() + ".jpg");
                    FileOutputStream fos = new FileOutputStream(targetFile);

                    byte[] buffer = new byte[8192];
                    long remaining = contentLength;
                    while (remaining > 0) {
                        int toRead = (int) Math.min(buffer.length, remaining);
                        int r = in.read(buffer, 0, toRead);
                        if (r == -1) break;
                        fos.write(buffer, 0, r);
                        remaining -= r;
                    }
                    fos.flush();
                    fos.close();

                    String json = "{\"status\":\"ok\",\"file\":\"" + targetFile.getName() + "\"}";
                    byte[] jsonBytes = json.getBytes("UTF-8");
                    String response = "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: application/json; charset=UTF-8\r\n" +
                            "Content-Length: " + jsonBytes.length + "\r\n" +
                            "Access-Control-Allow-Origin: *\r\n" +
                            "Connection: close\r\n\r\n";
                    out.write(response.getBytes("UTF-8"));
                    out.write(jsonBytes);
                    out.flush();

                    if (listener != null && targetFile.exists() && targetFile.length() > 0) {
                        listener.onUploaded(targetFile);
                    }
                } else {
                    String error = "HTTP/1.1 400 Bad Request\r\nConnection: close\r\n\r\nInvalid file size";
                    out.write(error.getBytes("UTF-8"));
                    out.flush();
                }
            } else {
                String notFound = "HTTP/1.1 404 Not Found\r\nConnection: close\r\n\r\nNot Found";
                out.write(notFound.getBytes("UTF-8"));
                out.flush();
            }

            out.close();
            in.close();
            client.close();
        } catch (Exception e) {
            Log.w(TAG, "Client handle exception: " + e.getMessage());
            try { client.close(); } catch (Exception ignored) {}
        }
    }

    private String getQueryParam(String requestLine, String param) {
        try {
            int qIdx = requestLine.indexOf('?');
            if (qIdx == -1) return "";
            int spIdx = requestLine.indexOf(' ', qIdx);
            String query = spIdx > qIdx ? requestLine.substring(qIdx + 1, spIdx) : requestLine.substring(qIdx + 1);
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                int eq = pair.indexOf('=');
                if (eq > 0 && pair.substring(0, eq).equals(param)) {
                    return URLDecoder.decode(pair.substring(eq + 1), "UTF-8");
                }
            }
        } catch (Exception ignored) {}
        return "";
    }

    private void serveThumbnail(String fileName, OutputStream out) {
        try {
            File customDir = new File(context.getFilesDir(), "custom_wallpapers");
            String safeName = fileName.replaceAll("[^a-zA-Z0-9._-]", "");
            File target = new File(customDir, safeName);
            if (!target.exists() || !target.isFile()) {
                String notFound = "HTTP/1.1 404 Not Found\r\nConnection: close\r\n\r\n";
                out.write(notFound.getBytes("UTF-8"));
                out.flush();
                return;
            }

            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(target.getAbsolutePath(), opts);

            int sample = 1;
            while (opts.outWidth / (sample * 2) >= 200 && opts.outHeight / (sample * 2) >= 150) sample *= 2;
            opts.inJustDecodeBounds = false;
            opts.inSampleSize = sample;
            opts.inPreferredConfig = Bitmap.Config.RGB_565;

            Bitmap bmp = BitmapFactory.decodeFile(target.getAbsolutePath(), opts);
            if (bmp == null) {
                String err = "HTTP/1.1 500 Error\r\nConnection: close\r\n\r\n";
                out.write(err.getBytes("UTF-8"));
                out.flush();
                return;
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.JPEG, 75, baos);
            bmp.recycle();
            byte[] bytes = baos.toByteArray();

            String response = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: image/jpeg\r\n" +
                    "Content-Length: " + bytes.length + "\r\n" +
                    "Cache-Control: max-age=3600\r\n" +
                    "Access-Control-Allow-Origin: *\r\n" +
                    "Connection: close\r\n\r\n";
            out.write(response.getBytes("UTF-8"));
            out.write(bytes);
            out.flush();
        } catch (Exception e) {
            Log.w(TAG, "Thumbnail error: " + e.getMessage());
        }
    }

    private void serveApiList(OutputStream out) {
        try {
            File customDir = new File(context.getFilesDir(), "custom_wallpapers");
            File[] files = customDir.listFiles((dir, name) -> name.endsWith(".jpg") || name.endsWith(".png") || name.endsWith(".jpeg"));
            JSONArray arr = new JSONArray();
            String activeSingle = context.getSharedPreferences("wallpaper_prefs", Context.MODE_PRIVATE)
                    .getString("custom_single_file", "");
            boolean isSingleMode = context.getSharedPreferences("wallpaper_prefs", Context.MODE_PRIVATE)
                    .getBoolean("custom_single_mode", false);

            if (files != null && files.length > 0) {
                Arrays.sort(files, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
                SimpleDateFormat sdf = new SimpleDateFormat("d MMM, HH:mm", Locale.getDefault());
                for (File f : files) {
                    JSONObject obj = new JSONObject();
                    obj.put("name", f.getName());
                    double kb = f.length() / 1024.0;
                    if (kb > 1024) {
                        obj.put("size", String.format(Locale.ROOT, "%.1f MB", kb / 1024.0));
                    } else {
                        obj.put("size", String.format(Locale.ROOT, "%.0f KB", kb));
                    }
                    obj.put("date", sdf.format(new Date(f.lastModified())));
                    obj.put("active", isSingleMode && f.getName().equals(activeSingle));
                    arr.put(obj);
                }
            }

            byte[] jsonBytes = arr.toString().getBytes("UTF-8");
            String response = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: application/json; charset=UTF-8\r\n" +
                    "Content-Length: " + jsonBytes.length + "\r\n" +
                    "Access-Control-Allow-Origin: *\r\n" +
                    "Connection: close\r\n\r\n";
            out.write(response.getBytes("UTF-8"));
            out.write(jsonBytes);
            out.flush();
        } catch (Exception e) {
            Log.w(TAG, "List API error: " + e.getMessage());
        }
    }

    private void handleApiSelect(String fileName, OutputStream out) {
        try {
            File customDir = new File(context.getFilesDir(), "custom_wallpapers");
            String safeName = fileName.replaceAll("[^a-zA-Z0-9._-]", "");
            File target = new File(customDir, safeName);
            if (target.exists() && listener != null) {
                listener.onSelected(target);
            }
            sendSimpleJsonResponse(out, "{\"status\":\"ok\"}");
        } catch (Exception ignored) {}
    }

    private void handleApiDelete(String fileName, OutputStream out) {
        try {
            File customDir = new File(context.getFilesDir(), "custom_wallpapers");
            String safeName = fileName.replaceAll("[^a-zA-Z0-9._-]", "");
            File target = new File(customDir, safeName);
            if (target.exists()) {
                target.delete();
                if (listener != null) listener.onDeleted(target);
            }
            sendSimpleJsonResponse(out, "{\"status\":\"ok\"}");
        } catch (Exception ignored) {}
    }

    private void handleApiSlideshow(OutputStream out) {
        try {
            if (listener != null) listener.onSlideshowMode();
            sendSimpleJsonResponse(out, "{\"status\":\"ok\"}");
        } catch (Exception ignored) {}
    }

    private void handleApiClear(OutputStream out) {
        try {
            File customDir = new File(context.getFilesDir(), "custom_wallpapers");
            File[] files = customDir.listFiles();
            if (files != null) {
                for (File f : files) f.delete();
            }
            if (listener != null) listener.onCleared();
            sendSimpleJsonResponse(out, "{\"status\":\"ok\"}");
        } catch (Exception ignored) {}
    }

    private void sendSimpleJsonResponse(OutputStream out, String json) throws Exception {
        byte[] bytes = json.getBytes("UTF-8");
        String response = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: application/json; charset=UTF-8\r\n" +
                "Content-Length: " + bytes.length + "\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Connection: close\r\n\r\n";
        out.write(response.getBytes("UTF-8"));
        out.write(bytes);
        out.flush();
    }

    private String getHtmlPage() {
        return "<!DOCTYPE html>\n" +
                "<html lang=\"ms\">\n" +
                "<head>\n" +
                "  <meta charset=\"UTF-8\">\n" +
                "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no\">\n" +
                "  <title>Glass TV • Upload Wallpaper</title>\n" +
                "  <style>\n" +
                "    * { box-sizing: border-box; margin: 0; padding: 0; }\n" +
                "    body {\n" +
                "      background: #0b0f19;\n" +
                "      color: #f1f5f9;\n" +
                "      font-family: -apple-system, BlinkMacSystemFont, \"Segoe UI\", Roboto, sans-serif;\n" +
                "      min-height: 100vh;\n" +
                "      display: flex;\n" +
                "      flex-direction: column;\n" +
                "      align-items: center;\n" +
                "      padding: 24px 16px 40px;\n" +
                "    }\n" +
                "    .card {\n" +
                "      width: 100%;\n" +
                "      max-width: 440px;\n" +
                "      background: rgba(255, 255, 255, 0.05);\n" +
                "      border: 1px solid rgba(255, 255, 255, 0.12);\n" +
                "      border-radius: 28px;\n" +
                "      padding: 26px 20px;\n" +
                "      box-shadow: 0 20px 40px rgba(0, 0, 0, 0.6), inset 0 1px 0 rgba(255, 255, 255, 0.2);\n" +
                "      backdrop-filter: blur(24px);\n" +
                "      -webkit-backdrop-filter: blur(24px);\n" +
                "      text-align: center;\n" +
                "    }\n" +
                "    .badge {\n" +
                "      display: inline-block;\n" +
                "      padding: 6px 14px;\n" +
                "      background: rgba(56, 189, 248, 0.15);\n" +
                "      color: #38bdf8;\n" +
                "      border: 1px solid rgba(56, 189, 248, 0.3);\n" +
                "      border-radius: 9999px;\n" +
                "      font-size: 12px;\n" +
                "      font-weight: 600;\n" +
                "      letter-spacing: 0.5px;\n" +
                "      margin-bottom: 14px;\n" +
                "      text-transform: uppercase;\n" +
                "    }\n" +
                "    .watermark {\n" +
                "      margin-top: 18px;\n" +
                "      font-size: 13px;\n" +
                "      font-weight: 500;\n" +
                "      color: #94a3b8;\n" +
                "      display: flex;\n" +
                "      align-items: center;\n" +
                "      justify-content: center;\n" +
                "      gap: 6px;\n" +
                "      letter-spacing: 0.3px;\n" +
                "    }\n" +
                "    .watermark strong {\n" +
                "      color: #38bdf8;\n" +
                "      font-weight: 700;\n" +
                "    }\n" +
                "    h1 {\n" +
                "      font-size: 23px;\n" +
                "      font-weight: 700;\n" +
                "      margin-bottom: 8px;\n" +
                "      color: #ffffff;\n" +
                "    }\n" +
                "    p.subtitle {\n" +
                "      font-size: 13.5px;\n" +
                "      color: #94a3b8;\n" +
                "      margin-bottom: 20px;\n" +
                "      line-height: 1.5;\n" +
                "    }\n" +
                "    .dropzone {\n" +
                "      border: 2px dashed rgba(255, 255, 255, 0.2);\n" +
                "      border-radius: 20px;\n" +
                "      padding: 24px 16px;\n" +
                "      cursor: pointer;\n" +
                "      transition: all 0.2s ease;\n" +
                "      background: rgba(0, 0, 0, 0.2);\n" +
                "      margin-bottom: 18px;\n" +
                "    }\n" +
                "    .dropzone:active {\n" +
                "      border-color: #38bdf8;\n" +
                "      background: rgba(56, 189, 248, 0.08);\n" +
                "    }\n" +
                "    .dropzone-icon {\n" +
                "      font-size: 38px;\n" +
                "      margin-bottom: 8px;\n" +
                "    }\n" +
                "    .dropzone-text {\n" +
                "      font-size: 15px;\n" +
                "      font-weight: 600;\n" +
                "      color: #e2e8f0;\n" +
                "      margin-bottom: 4px;\n" +
                "    }\n" +
                "    .dropzone-hint {\n" +
                "      font-size: 12px;\n" +
                "      color: #64748b;\n" +
                "    }\n" +
                "    #preview-container {\n" +
                "      display: none;\n" +
                "      margin-bottom: 18px;\n" +
                "      position: relative;\n" +
                "      border-radius: 18px;\n" +
                "      overflow: hidden;\n" +
                "      border: 1px solid rgba(255, 255, 255, 0.15);\n" +
                "      background: #000;\n" +
                "      max-height: 220px;\n" +
                "    }\n" +
                "    #preview {\n" +
                "      width: 100%;\n" +
                "      height: 220px;\n" +
                "      object-fit: cover;\n" +
                "      display: block;\n" +
                "    }\n" +
                "    .change-btn {\n" +
                "      position: absolute;\n" +
                "      bottom: 10px;\n" +
                "      right: 10px;\n" +
                "      background: rgba(0, 0, 0, 0.7);\n" +
                "      backdrop-filter: blur(8px);\n" +
                "      color: #fff;\n" +
                "      border: 1px solid rgba(255, 255, 255, 0.3);\n" +
                "      padding: 6px 12px;\n" +
                "      border-radius: 12px;\n" +
                "      font-size: 12px;\n" +
                "      cursor: pointer;\n" +
                "    }\n" +
                "    input[type=\"file\"] { display: none; }\n" +
                "    .btn-submit {\n" +
                "      width: 100%;\n" +
                "      background: linear-gradient(135deg, #0ea5e9 0%, #2563eb 100%);\n" +
                "      color: #ffffff;\n" +
                "      border: none;\n" +
                "      border-radius: 16px;\n" +
                "      padding: 15px;\n" +
                "      font-size: 15px;\n" +
                "      font-weight: 600;\n" +
                "      cursor: pointer;\n" +
                "      box-shadow: 0 8px 20px rgba(14, 165, 233, 0.35);\n" +
                "      transition: all 0.2s ease;\n" +
                "      display: flex;\n" +
                "      align-items: center;\n" +
                "      justify-content: center;\n" +
                "      gap: 8px;\n" +
                "    }\n" +
                "    .btn-submit:disabled {\n" +
                "      opacity: 0.5;\n" +
                "      cursor: not-allowed;\n" +
                "      box-shadow: none;\n" +
                "    }\n" +
                "    .btn-submit:active:not(:disabled) {\n" +
                "      transform: scale(0.98);\n" +
                "    }\n" +
                "    .progress-wrap {\n" +
                "      display: none;\n" +
                "      width: 100%;\n" +
                "      height: 8px;\n" +
                "      background: rgba(255, 255, 255, 0.1);\n" +
                "      border-radius: 9999px;\n" +
                "      margin-top: 16px;\n" +
                "      overflow: hidden;\n" +
                "    }\n" +
                "    .progress-bar {\n" +
                "      width: 0%;\n" +
                "      height: 100%;\n" +
                "      background: linear-gradient(90deg, #38bdf8, #4ade80);\n" +
                "      border-radius: 9999px;\n" +
                "      transition: width 0.2s ease;\n" +
                "    }\n" +
                "    .status-msg {\n" +
                "      margin-top: 14px;\n" +
                "      font-size: 13.5px;\n" +
                "      font-weight: 500;\n" +
                "      display: none;\n" +
                "    }\n" +
                "    .status-msg.success { color: #4ade80; }\n" +
                "    .status-msg.error { color: #f87171; }\n" +
                "    \n" +
                "    /* Manager Gallery Section */\n" +
                "    .gallery-card {\n" +
                "      width: 100%;\n" +
                "      max-width: 440px;\n" +
                "      margin-top: 22px;\n" +
                "      background: rgba(255, 255, 255, 0.05);\n" +
                "      border: 1px solid rgba(255, 255, 255, 0.12);\n" +
                "      border-radius: 28px;\n" +
                "      padding: 22px 18px;\n" +
                "      box-shadow: 0 20px 40px rgba(0, 0, 0, 0.6);\n" +
                "      backdrop-filter: blur(24px);\n" +
                "      -webkit-backdrop-filter: blur(24px);\n" +
                "    }\n" +
                "    .gallery-header {\n" +
                "      display: flex;\n" +
                "      justify-content: space-between;\n" +
                "      align-items: center;\n" +
                "      margin-bottom: 8px;\n" +
                "    }\n" +
                "    .gallery-title {\n" +
                "      font-size: 17px;\n" +
                "      font-weight: 700;\n" +
                "      color: #ffffff;\n" +
                "    }\n" +
                "    .gallery-desc {\n" +
                "      font-size: 12.5px;\n" +
                "      color: #94a3b8;\n" +
                "      text-align: left;\n" +
                "      margin-bottom: 14px;\n" +
                "      line-height: 1.4;\n" +
                "    }\n" +
                "    .btn-slideshow {\n" +
                "      background: rgba(56, 189, 248, 0.18);\n" +
                "      border: 1px solid rgba(56, 189, 248, 0.35);\n" +
                "      color: #38bdf8;\n" +
                "      padding: 6px 12px;\n" +
                "      border-radius: 12px;\n" +
                "      font-size: 12px;\n" +
                "      font-weight: 600;\n" +
                "      cursor: pointer;\n" +
                "      transition: all 0.2s ease;\n" +
                "    }\n" +
                "    .btn-slideshow:active { transform: scale(0.96); }\n" +
                "    .gallery-list {\n" +
                "      display: flex;\n" +
                "      flex-direction: column;\n" +
                "      gap: 10px;\n" +
                "    }\n" +
                "    .photo-row {\n" +
                "      display: flex;\n" +
                "      align-items: center;\n" +
                "      gap: 10px;\n" +
                "      background: rgba(0, 0, 0, 0.35);\n" +
                "      border: 1px solid rgba(255, 255, 255, 0.08);\n" +
                "      border-radius: 16px;\n" +
                "      padding: 8px 10px;\n" +
                "      transition: all 0.2s ease;\n" +
                "    }\n" +
                "    .photo-row.active {\n" +
                "      border-color: rgba(56, 189, 248, 0.4);\n" +
                "      background: rgba(56, 189, 248, 0.08);\n" +
                "    }\n" +
                "    .photo-thumb {\n" +
                "      width: 60px;\n" +
                "      height: 44px;\n" +
                "      border-radius: 10px;\n" +
                "      object-fit: cover;\n" +
                "      background: #1e293b;\n" +
                "      flex-shrink: 0;\n" +
                "    }\n" +
                "    .photo-meta {\n" +
                "      flex: 1;\n" +
                "      text-align: left;\n" +
                "      min-width: 0;\n" +
                "    }\n" +
                "    .photo-name {\n" +
                "      font-size: 13px;\n" +
                "      font-weight: 600;\n" +
                "      color: #f1f5f9;\n" +
                "      white-space: nowrap;\n" +
                "      overflow: hidden;\n" +
                "      text-overflow: ellipsis;\n" +
                "    }\n" +
                "    .photo-sub {\n" +
                "      font-size: 11px;\n" +
                "      color: #94a3b8;\n" +
                "      margin-top: 2px;\n" +
                "    }\n" +
                "    .photo-btns {\n" +
                "      display: flex;\n" +
                "      gap: 6px;\n" +
                "      flex-shrink: 0;\n" +
                "    }\n" +
                "    .btn-act {\n" +
                "      background: rgba(255, 255, 255, 0.08);\n" +
                "      border: 1px solid rgba(255, 255, 255, 0.15);\n" +
                "      color: #cbd5e1;\n" +
                "      padding: 6px 9px;\n" +
                "      border-radius: 10px;\n" +
                "      font-size: 11.5px;\n" +
                "      font-weight: 500;\n" +
                "      cursor: pointer;\n" +
                "    }\n" +
                "    .btn-act:active { transform: scale(0.95); }\n" +
                "    .btn-act.active {\n" +
                "      background: #0284c7;\n" +
                "      border-color: #38bdf8;\n" +
                "      color: #ffffff;\n" +
                "      font-weight: 700;\n" +
                "    }\n" +
                "    .btn-act.del {\n" +
                "      color: #f87171;\n" +
                "      border-color: rgba(248, 113, 113, 0.3);\n" +
                "      background: rgba(248, 113, 113, 0.1);\n" +
                "    }\n" +
                "    footer {\n" +
                "      margin-top: 24px;\n" +
                "      font-size: 12px;\n" +
                "      color: #475569;\n" +
                "    }\n" +
                "  </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "  <div class=\"card\">\n" +
                "    <div class=\"badge\">Glass TV OS • by @imsomangge</div>\n" +
                "    <h1>Muat Naik Wallpaper</h1>\n" +
                "    <p class=\"subtitle\">Pilih mana-mana foto dari galeri telefon untuk dipaparkan secara langsung pada skrin TV anda.</p>\n" +
                "\n" +
                "    <div class=\"dropzone\" id=\"dropzone\" onclick=\"document.getElementById('file-input').click()\">\n" +
                "      <div class=\"dropzone-icon\">🖼️</div>\n" +
                "      <div class=\"dropzone-text\">Pilih Foto dari Galeri</div>\n" +
                "      <div class=\"dropzone-hint\">Menyokong JPG, PNG, WEBP (Resolusi 1080p / 4K)</div>\n" +
                "    </div>\n" +
                "\n" +
                "    <div id=\"preview-container\">\n" +
                "      <img id=\"preview\" alt=\"Preview\">\n" +
                "      <button class=\"change-btn\" onclick=\"document.getElementById('file-input').click()\">Tukar Foto</button>\n" +
                "    </div>\n" +
                "\n" +
                "    <input type=\"file\" id=\"file-input\" accept=\"image/*\">\n" +
                "\n" +
                "    <button class=\"btn-submit\" id=\"btn-submit\" disabled onclick=\"uploadImage()\">\n" +
                "      <span>Hantar ke Skrin TV</span>\n" +
                "    </button>\n" +
                "\n" +
                "    <div class=\"progress-wrap\" id=\"prog-wrap\">\n" +
                "      <div class=\"progress-bar\" id=\"prog-bar\"></div>\n" +
                "    </div>\n" +
                "\n" +
                "    <div class=\"status-msg\" id=\"status-msg\"></div>\n" +
                "    <div class=\"watermark\">✨ Powered by <strong>@imsomangge</strong></div>\n" +
                "  </div>\n" +
                "\n" +
                "  <div class=\"gallery-card\">\n" +
                "    <div class=\"gallery-header\">\n" +
                "      <div class=\"gallery-title\">📱 Gambar di TV (<span id=\"photo-count\">0</span>)</div>\n" +
                "      <button class=\"btn-slideshow\" onclick=\"setSlideshow()\">🔄 Putar Semua</button>\n" +
                "    </div>\n" +
                "    <div class=\"gallery-desc\">Pilih <strong>Kunci</strong> untuk kekalkan 1 gambar sahaja sebagai wallpaper, atau padam foto lama:</div>\n" +
                "    <div class=\"gallery-list\" id=\"gallery-list\">\n" +
                "      <div style=\"color:#64748b; font-size:12.5px; padding:12px 0;\">Memuat senarai foto...</div>\n" +
                "    </div>\n" +
                "  </div>\n" +
                "\n" +
                "  <footer>\n" +
                "    <div>Bersambung terus ke TV melalui Wi-Fi Tempatan</div>\n" +
                "    <div style=\"margin-top: 6px; font-weight: 600; color: #64748b;\">Powered by @imsomangge</div>\n" +
                "  </footer>\n" +
                "\n" +
                "  <script>\n" +
                "    const fileInput = document.getElementById('file-input');\n" +
                "    const dropzone = document.getElementById('dropzone');\n" +
                "    const previewContainer = document.getElementById('preview-container');\n" +
                "    const preview = document.getElementById('preview');\n" +
                "    const btnSubmit = document.getElementById('btn-submit');\n" +
                "    const progWrap = document.getElementById('prog-wrap');\n" +
                "    const progBar = document.getElementById('prog-bar');\n" +
                "    const statusMsg = document.getElementById('status-msg');\n" +
                "\n" +
                "    let selectedFile = null;\n" +
                "\n" +
                "    fileInput.addEventListener('change', (e) => {\n" +
                "      const file = e.target.files[0];\n" +
                "      if (!file) return;\n" +
                "      selectedFile = file;\n" +
                "\n" +
                "      const reader = new FileReader();\n" +
                "      reader.onload = (evt) => {\n" +
                "        preview.src = evt.target.result;\n" +
                "        dropzone.style.display = 'none';\n" +
                "        previewContainer.style.display = 'block';\n" +
                "        btnSubmit.disabled = false;\n" +
                "        btnSubmit.innerHTML = '<span>✨ Tetapkan Sebagai Wallpaper TV</span>';\n" +
                "        statusMsg.style.display = 'none';\n" +
                "      };\n" +
                "      reader.readAsDataURL(file);\n" +
                "    });\n" +
                "\n" +
                "    function uploadImage() {\n" +
                "      if (!selectedFile) return;\n" +
                "\n" +
                "      btnSubmit.disabled = true;\n" +
                "      btnSubmit.innerText = 'Menghantar ke TV...';\n" +
                "      progWrap.style.display = 'block';\n" +
                "      progBar.style.width = '15%';\n" +
                "      statusMsg.style.display = 'none';\n" +
                "\n" +
                "      const xhr = new XMLHttpRequest();\n" +
                "      xhr.open('POST', '/upload?name=' + encodeURIComponent(selectedFile.name), true);\n" +
                "      xhr.setRequestHeader('Content-Type', selectedFile.type || 'image/jpeg');\n" +
                "\n" +
                "      xhr.upload.onprogress = (e) => {\n" +
                "        if (e.lengthComputable) {\n" +
                "          const percent = Math.round((e.loaded / e.total) * 100);\n" +
                "          progBar.style.width = percent + '%';\n" +
                "        }\n" +
                "      };\n" +
                "\n" +
                "      xhr.onload = () => {\n" +
                "        if (xhr.status >= 200 && xhr.status < 300) {\n" +
                "          progBar.style.width = '100%';\n" +
                "          btnSubmit.innerText = '✅ Berjaya Dihantar!';\n" +
                "          statusMsg.className = 'status-msg success';\n" +
                "          statusMsg.innerHTML = '🎉 Hebat! Gambar telah dipaparkan pada skrin TV anda.';\n" +
                "          statusMsg.style.display = 'block';\n" +
                "          loadGallery();\n" +
                "        } else {\n" +
                "          showError('Gagal memuat naik: ' + xhr.statusText);\n" +
                "        }\n" +
                "      };\n" +
                "\n" +
                "      xhr.onerror = () => {\n" +
                "        showError('Sambungan terputus. Pastikan telefon dan TV dalam Wi-Fi yang sama.');\n" +
                "      };\n" +
                "\n" +
                "      xhr.send(selectedFile);\n" +
                "    }\n" +
                "\n" +
                "    function showError(msg) {\n" +
                "      btnSubmit.disabled = false;\n" +
                "      btnSubmit.innerText = 'Cuba Semula';\n" +
                "      statusMsg.className = 'status-msg error';\n" +
                "      statusMsg.innerText = '❌ ' + msg;\n" +
                "      statusMsg.style.display = 'block';\n" +
                "      progWrap.style.display = 'none';\n" +
                "    }\n" +
                "\n" +
                "    function loadGallery() {\n" +
                "      fetch('/api/list')\n" +
                "        .then(r => r.json())\n" +
                "        .then(items => {\n" +
                "          const listEl = document.getElementById('gallery-list');\n" +
                "          const countEl = document.getElementById('photo-count');\n" +
                "          if (countEl) countEl.innerText = items.length;\n" +
                "          if (!items || items.length === 0) {\n" +
                "            listEl.innerHTML = '<div style=\"color:#64748b; font-size:12.5px; padding:12px 0;\">Tiada gambar tersimpan di TV.</div>';\n" +
                "            return;\n" +
                "          }\n" +
                "          let html = '';\n" +
                "          for (const item of items) {\n" +
                "            const enc = encodeURIComponent(item.name);\n" +
                "            html += '<div class=\"photo-row' + (item.active ? ' active' : '') + '\">' +\n" +
                "              '<img class=\"photo-thumb\" src=\"/thumb?file=' + enc + '\" alt=\"thumb\">' +\n" +
                "              '<div class=\"photo-meta\">' +\n" +
                "                '<div class=\"photo-name\">' + (item.active ? '✨ ' : '') + item.name + '</div>' +\n" +
                "                '<div class=\"photo-sub\">' + item.date + ' • ' + item.size + '</div>' +\n" +
                "              '</div>' +\n" +
                "              '<div class=\"photo-btns\">' +\n" +
                "                '<button class=\"btn-act' + (item.active ? ' active' : '') + '\" onclick=\"selectSingle(\\'' + enc + '\\')\">' +\n" +
                "                  (item.active ? 'Kekal 📌' : 'Kunci 📌') +\n" +
                "                '</button>' +\n" +
                "                '<button class=\"btn-act del\" onclick=\"deletePhoto(\\'' + enc + '\\')\">🗑️</button>' +\n" +
                "              '</div>' +\n" +
                "            '</div>';\n" +
                "          }\n" +
                "          listEl.innerHTML = html;\n" +
                "        })\n" +
                "        .catch(() => {});\n" +
                "    }\n" +
                "\n" +
                "    function selectSingle(fileName) {\n" +
                "      fetch('/api/select?file=' + fileName, { method: 'POST' })\n" +
                "        .then(r => r.json())\n" +
                "        .then(() => {\n" +
                "          loadGallery();\n" +
                "        });\n" +
                "    }\n" +
                "\n" +
                "    function deletePhoto(fileName) {\n" +
                "      if (!confirm('Padam gambar ini dari skrin TV?')) return;\n" +
                "      fetch('/api/delete?file=' + fileName, { method: 'POST' })\n" +
                "        .then(r => r.json())\n" +
                "        .then(() => {\n" +
                "          loadGallery();\n" +
                "        });\n" +
                "    }\n" +
                "\n" +
                "    function setSlideshow() {\n" +
                "      fetch('/api/slideshow', { method: 'POST' })\n" +
                "        .then(r => r.json())\n" +
                "        .then(() => {\n" +
                "          loadGallery();\n" +
                "          alert('Mod putaran semua gambar aktif pada TV.');\n" +
                "        });\n" +
                "    }\n" +
                "\n" +
                "    // Muat galeri gambar sebaik sahaja laman dibuka\n" +
                "    loadGallery();\n" +
                "  </script>\n" +
                "</body>\n" +
                "</html>";
    }
}
