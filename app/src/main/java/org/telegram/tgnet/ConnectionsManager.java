package org.telegram.tgnet;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.PowerManager;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import ir.we.diagram.BuildConfig;
import ir.we.diagram.DiagramApplication;
import ir.we.diagram.UserInfo;
import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.Utilities;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public class ConnectionsManager {

    public static final String TAG = "TGConnectionsManager";
    
    public final static int ConnectionTypeGeneric = 1;
    public final static int ConnectionTypeDownload = 2;
    public final static int ConnectionTypeUpload = 4;
    public final static int ConnectionTypePush = 8;
    public final static int ConnectionTypeDownload2 = ConnectionTypeDownload | (1 << 16);

    public final static int FileTypePhoto = 0x01000000;
    public final static int FileTypeVideo = 0x02000000;
    public final static int FileTypeAudio = 0x03000000;
    public final static int FileTypeFile = 0x04000000;

    public final static int RequestFlagEnableUnauthorized = 1;
    public final static int RequestFlagFailOnServerErrors = 2;
    public final static int RequestFlagCanCompress = 4;
    public final static int RequestFlagWithoutLogin = 8;
    public final static int RequestFlagTryDifferentDc = 16;
    public final static int RequestFlagForceDownload = 32;
    public final static int RequestFlagInvokeAfter = 64;
    public final static int RequestFlagNeedQuickAck = 128;

    public final static int ConnectionStateConnecting = 1;
    public final static int ConnectionStateWaitingForNetwork = 2;
    public final static int ConnectionStateConnected = 3;
    public final static int ConnectionStateConnectingToProxy = 4;
    public final static int ConnectionStateUpdating = 5;

    private final static int dnsConfigVersion = 0;

    private static long lastDnsRequestTime;

    public final static int DEFAULT_DATACENTER_ID = Integer.MAX_VALUE;

    private long lastPauseTime = System.currentTimeMillis();
    private boolean appPaused = true;
    private int lastClassGuid = 1;
    private int connectionState = native_getConnectionState();
    private AtomicInteger lastRequestToken = new AtomicInteger(1);
    private PowerManager.WakeLock wakeLock;
    private int appResumeCount;

    private static AsyncTask currentTask;

    private static volatile ConnectionsManager Instance = null;

    public static ConnectionsManager getInstance() {
        ConnectionsManager localInstance = Instance;
        if (localInstance == null) {
            synchronized (ConnectionsManager.class) {
                localInstance = Instance;
                if (localInstance == null) {
                    Instance = localInstance = new ConnectionsManager();
                }
            }
        }
        return localInstance;
    }

    public ConnectionsManager() {
        String deviceModel;
        String systemLangCode;
        String langCode;
        String appVersion;
        String systemVersion;
        String configPath = DiagramApplication.getFilesDirFixed().toString();
        SharedPreferences preferences = DiagramApplication.getInstance().getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
        boolean enablePushConnection = preferences.getBoolean("pushConnection", true);
        try {
            systemLangCode = "en"; // TODO LocaleController.getSystemLocaleStringIso639();
            langCode = "en"; // TODO LocaleController.getLocaleStringIso639();
            deviceModel = Build.MANUFACTURER + Build.MODEL;
            PackageInfo pInfo = DiagramApplication.getInstance().getPackageManager().getPackageInfo(DiagramApplication.getInstance().getPackageName(), 0);
            appVersion = pInfo.versionName + " (" + pInfo.versionCode + ")";
            systemVersion = "SDK " + Build.VERSION.SDK_INT;
        } catch (Exception e) {
            systemLangCode = "en";
            langCode = "";
            deviceModel = "Android unknown";
            appVersion = "App version unknown";
            systemVersion = "SDK " + Build.VERSION.SDK_INT;
        }
        if (systemLangCode.trim().length() == 0) {
            langCode = "en";
        }
        if (deviceModel.trim().length() == 0) {
            deviceModel = "Android unknown";
        }
        if (appVersion.trim().length() == 0) {
            appVersion = "App version unknown";
        }
        if (systemVersion.trim().length() == 0) {
            systemVersion = "SDK Unknown";
        }
        init(BuildConfig.VERSION_CODE, TLRPC.LAYER, DiagramApplication.APP_ID, deviceModel, systemVersion, appVersion, langCode, systemLangCode, configPath, "", UserInfo.getUserId(), enablePushConnection);
        try {
            PowerManager pm = (PowerManager) DiagramApplication.getInstance().getSystemService(Context.POWER_SERVICE);
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "lock");
            wakeLock.setReferenceCounted(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public long getCurrentTimeMillis() {
        return native_getCurrentTimeMillis();
    }

    public int getCurrentTime() {
        return native_getCurrentTime();
    }

    public int getTimeDifference() {
        return native_getTimeDifference();
    }

    public int sendRequest(TLObject object, RequestDelegate completionBlock) {
        return sendRequest(object, completionBlock, null, 0);
    }

    public int sendRequest(TLObject object, RequestDelegate completionBlock, int flags) {
        return sendRequest(object, completionBlock, null, null, flags, DEFAULT_DATACENTER_ID, ConnectionTypeGeneric, true);
    }

    public int sendRequest(TLObject object, RequestDelegate completionBlock, int flags, int connetionType) {
        return sendRequest(object, completionBlock, null, null, flags, DEFAULT_DATACENTER_ID, connetionType, true);
    }

    public int sendRequest(TLObject object, RequestDelegate completionBlock, QuickAckDelegate quickAckBlock, int flags) {
        return sendRequest(object, completionBlock, quickAckBlock, null, flags, DEFAULT_DATACENTER_ID, ConnectionTypeGeneric, true);
    }

    public int sendRequest(final TLObject object, final RequestDelegate onComplete, final QuickAckDelegate onQuickAck, final WriteToSocketDelegate onWriteToSocket, final int flags, final int datacenterId, final int connetionType, final boolean immediate) {
        final int requestToken = lastRequestToken.getAndIncrement();
        Utilities.stageQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "send request " + object + " with token = " + requestToken);
                try {
                    NativeByteBuffer buffer = new NativeByteBuffer(object.getObjectSize());
                    object.serializeToStream(buffer);
                    object.freeResources();

                    native_sendRequest(buffer.address, new RequestDelegateInternal() {
                        @Override
                        public void run(int response, int errorCode, String errorText, int networkType) {
                            try {
                                TLObject resp = null;
                                TLRPC.TL_error error = null;
                                if (response != 0) {
                                    NativeByteBuffer buff = NativeByteBuffer.wrap(response);
                                    buff.reused = true;
                                    resp = object.deserializeResponse(buff, buff.readInt32(true), true);
                                } else if (errorText != null) {
                                    error = new TLRPC.TL_error();
                                    error.code = errorCode;
                                    error.text = errorText;
                                    Log.e(TAG, object + " got error " + error.code + " " + error.text);
                                }
                                if (resp != null) {
                                    resp.networkType = networkType;
                                }
                                Log.d(TAG, "java received " + resp + " error = " + error);
                                final TLObject finalResponse = resp;
                                final TLRPC.TL_error finalError = error;
                                Utilities.stageQueue.postRunnable(new Runnable() {
                                    @Override
                                    public void run() {
                                        onComplete.run(finalResponse, finalError);
                                        if (finalResponse != null) {
                                            finalResponse.freeResources();
                                        }
                                    }
                                });
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }, onQuickAck, onWriteToSocket, flags, datacenterId, connetionType, immediate, requestToken);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        return requestToken;
    }

    public void cancelRequest(int token, boolean notifyServer) {
        native_cancelRequest(token, notifyServer);
    }

    public void cleanup() {
        native_cleanUp();
    }

    public void cancelRequestsForGuid(int guid) {
        native_cancelRequestsForGuid(guid);
    }

    public void bindRequestToGuid(int requestToken, int guid) {
        native_bindRequestToGuid(requestToken, guid);
    }

    public void applyDatacenterAddress(int datacenterId, String ipAddress, int port) {
        native_applyDatacenterAddress(datacenterId, ipAddress, port);
    }

    public void setUserId(int id) {
        native_setUserId(id);
    }

//    private void checkConnection() {
//        native_setUseIpv6(useIpv6Address());
//        native_setNetworkAvailable(isNetworkOnline(), getCurrentNetworkType());
//    }


    public void init(int version, int layer, int apiId, String deviceModel, String systemVersion, String appVersion, String langCode, String systemLangCode, String configPath, String logPath, int userId, boolean enablePushConnection) {
        SharedPreferences preferences = DiagramApplication.getInstance().getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
        String proxyAddress = preferences.getString("proxy_ip", "");
        String proxyUsername = preferences.getString("proxy_user", "");
        String proxyPassword = preferences.getString("proxy_pass", "");
        int proxyPort = preferences.getInt("proxy_port", 1080);
        if (preferences.getBoolean("proxy_enabled", false) && !TextUtils.isEmpty(proxyAddress)) {
            native_setProxySettings(proxyAddress, proxyPort, proxyUsername, proxyPassword);
        }

        native_init(version, layer, apiId, deviceModel, systemVersion, appVersion, langCode, systemLangCode, configPath, logPath, userId, enablePushConnection, isNetworkOnline(), -1);
        checkConnection();
        BroadcastReceiver networkStateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                checkConnection();
            }
        };
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        DiagramApplication.getInstance().registerReceiver(networkStateReceiver, filter);
    }

    public void setLangCode(String langCode) {
        native_setLangCode(langCode);
    }

    public void switchBackend() {
        SharedPreferences preferences = DiagramApplication.getInstance().getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
        preferences.edit().remove("language_showed2").commit();
        native_switchBackend();
    }

    public void resumeNetworkMaybe() {
        native_resumeNetwork(true);
    }

    public void updateDcSettings() {
        native_updateDcSettings();
    }

    public long getPauseTime() {
        return lastPauseTime;
    }


    private void checkConnection() {
        native_setUseIpv6(useIpv6Address());
        native_setNetworkAvailable(isNetworkOnline(), -1);
    }


    public static void onLogout() {
        UserInfo.setUserId(0);
    }

    public static void onRequestNewServerIpAndPort(int second) {
        if (currentTask != null || second != 1 && Math.abs(lastDnsRequestTime - System.currentTimeMillis()) < 10000 || !isNetworkOnline()) {
            return;
        }
        lastDnsRequestTime = System.currentTimeMillis();
        if (second == 1) {
            DnsTxtLoadTask task = new DnsTxtLoadTask();
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null, null, null);
            currentTask = task;
        } else {
            DnsLoadTask task = new DnsLoadTask();
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null, null, null);
            currentTask = task;
        }
    }

    public static native void native_switchBackend();
    public static native int native_isTestBackend();
    public static native void native_pauseNetwork();
    public static native void native_setUseIpv6(boolean value);
    public static native void native_updateDcSettings();
    public static native void native_setNetworkAvailable(boolean value, int networkType);
    public static native void native_resumeNetwork(boolean partial);
    public static native long native_getCurrentTimeMillis();
    public static native int native_getCurrentTime();
    public static native int native_getTimeDifference();
    public static native void native_sendRequest(int object, RequestDelegateInternal onComplete, QuickAckDelegate onQuickAck, WriteToSocketDelegate onWriteToSocket, int flags, int datacenterId, int connetionType, boolean immediate, int requestToken);
    public static native void native_cancelRequest(int token, boolean notifyServer);
    public static native void native_cleanUp();
    public static native void native_cancelRequestsForGuid(int guid);
    public static native void native_bindRequestToGuid(int requestToken, int guid);
    public static native void native_applyDatacenterAddress(int datacenterId, String ipAddress, int port);
    public static native int native_getConnectionState();
    public static native void native_setUserId(int id);
    public static native void native_init(int version, int layer, int apiId, String deviceModel, String systemVersion, String appVersion, String langCode, String systemLangCode, String configPath, String logPath, int userId, boolean enablePushConnection, boolean hasNetwork, int networkType);
    public static native void native_setProxySettings(String address, int port, String username, String password);
    public static native void native_setLangCode(String langCode);
    public static native void native_setJava(boolean useJavaByteBuffers);
    public static native void native_setPushConnectionEnabled(boolean value);
    public static native void native_applyDnsConfig(int address);

    public int generateClassGuid() {
        return lastClassGuid++;
    }


    public static boolean isConnectedOrConnectingToWiFi() {
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) DiagramApplication.getInstance().getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            NetworkInfo.State state = netInfo.getState();
            if (netInfo != null && (state == NetworkInfo.State.CONNECTED || state == NetworkInfo.State.CONNECTING || state == NetworkInfo.State.SUSPENDED)) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean isConnectedToWiFi() {
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) DiagramApplication.getInstance().getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            if (netInfo != null && netInfo.getState() == NetworkInfo.State.CONNECTED) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @SuppressLint("NewApi")
    protected static boolean useIpv6Address() {
        if (Build.VERSION.SDK_INT < 19) {
            return false;
        }
        if (BuildConfig.DEBUG) {
            try {
                NetworkInterface networkInterface;
                Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
                while (networkInterfaces.hasMoreElements()) {
                    networkInterface = networkInterfaces.nextElement();
                    if (!networkInterface.isUp() || networkInterface.isLoopback() || networkInterface.getInterfaceAddresses().isEmpty()) {
                        continue;
                    }
                    Log.e(TAG, "valid interface: " + networkInterface);
                    List<InterfaceAddress> interfaceAddresses = networkInterface.getInterfaceAddresses();
                    for (int a = 0; a < interfaceAddresses.size(); a++) {
                        InterfaceAddress address = interfaceAddresses.get(a);
                        InetAddress inetAddress = address.getAddress();
                        if (BuildConfig.DEBUG) {
                            Log.e(TAG, "address: " + inetAddress.getHostAddress());
                        }
                        if (inetAddress.isLinkLocalAddress() || inetAddress.isLoopbackAddress() || inetAddress.isMulticastAddress()) {
                            continue;
                        }
                        if (BuildConfig.DEBUG) {
                            Log.e(TAG, "address is good");
                        }
                    }
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        try {
            NetworkInterface networkInterface;
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            boolean hasIpv4 = false;
            boolean hasIpv6 = false;
            while (networkInterfaces.hasMoreElements()) {
                networkInterface = networkInterfaces.nextElement();
                if (!networkInterface.isUp() || networkInterface.isLoopback()) {
                    continue;
                }
                List<InterfaceAddress> interfaceAddresses = networkInterface.getInterfaceAddresses();
                for (int a = 0; a < interfaceAddresses.size(); a++) {
                    InterfaceAddress address = interfaceAddresses.get(a);
                    InetAddress inetAddress = address.getAddress();
                    if (inetAddress.isLinkLocalAddress() || inetAddress.isLoopbackAddress() || inetAddress.isMulticastAddress()) {
                        continue;
                    }
                    if (inetAddress instanceof Inet6Address) {
                        hasIpv6 = true;
                    } else if (inetAddress instanceof Inet4Address) {
                        String addrr = inetAddress.getHostAddress();
                        if (!addrr.startsWith("192.0.0.")) {
                            hasIpv4 = true;
                        }
                    }
                }
            }
            if (!hasIpv4 && hasIpv6) {
                return true;
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }

        return false;
    }

    public static boolean isNetworkOnline() {
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) DiagramApplication.getInstance().getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = connectivityManager.getActiveNetworkInfo();
            if (netInfo != null && (netInfo.isConnectedOrConnecting() || netInfo.isAvailable())) {
                return true;
            }

            netInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

            if (netInfo != null && netInfo.isConnectedOrConnecting()) {
                return true;
            } else {
                netInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                if (netInfo != null && netInfo.isConnectedOrConnecting()) {
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return true;
        }
        return false;
    }

    private static class DnsTxtLoadTask extends AsyncTask<Void, Void, NativeByteBuffer> {
        protected NativeByteBuffer doInBackground(Void... voids) {
            try {
                String domain = String.format(Locale.US, native_isTestBackend() != 0 ? "tap%1$s.stel.com" : "ap%1$s.stel.com", dnsConfigVersion == 0 ? "" : "" + dnsConfigVersion);
                URL downloadUrl = new URL("https://google.com/resolve?name=" + domain + "&type=16");
                URLConnection httpConnection = downloadUrl.openConnection();
                httpConnection.addRequestProperty("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 10_0 like Mac OS X) AppleWebKit/602.1.38 (KHTML, like Gecko) Version/10.0 Mobile/14A5297c Safari/602.1");
                httpConnection.addRequestProperty("Host", "dns.google.com");
                httpConnection.setConnectTimeout(5000);
                httpConnection.setReadTimeout(5000);
                httpConnection.connect();
                InputStream httpConnectionStream = httpConnection.getInputStream();

                ByteArrayOutputStream outbuf = new ByteArrayOutputStream();

                byte[] data = new byte[1024 * 32];
                while (true) {
                    if (isCancelled()) {
                        break;
                    }
                    int read = httpConnectionStream.read(data);
                    if (read > 0) {
                        outbuf.write(data, 0, read);
                    } else if (read == -1) {
                        break;
                    } else {
                        break;
                    }
                }
                try {
                    if (httpConnectionStream != null) {
                        httpConnectionStream.close();
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                }
                JSONObject jsonObject = new JSONObject(new String(outbuf.toByteArray(), "UTF-8"));
                JSONArray array = jsonObject.getJSONArray("Answer");
                int len = array.length();
                ArrayList<String> arrayList = new ArrayList<>(len);
                for (int a = 0; a < len; a++) {
                    arrayList.add(array.getJSONObject(a).getString("data"));
                }
                Collections.sort(arrayList, new Comparator<String>() {
                    @Override
                    public int compare(String o1, String o2) {
                        int l1 = o1.length();
                        int l2 = o2.length();
                        if (l1 > l2) {
                            return -1;
                        } else if (l1 < l2) {
                            return 1;
                        }
                        return 0;
                    }
                });
                StringBuilder builder = new StringBuilder();
                for (int a = 0; a < arrayList.size(); a++) {
                    builder.append(arrayList.get(a).replace("\"", ""));
                }
                byte[] bytes = Base64.decode(builder.toString(), Base64.DEFAULT);
                NativeByteBuffer buffer = new NativeByteBuffer(bytes.length);
                buffer.writeBytes(bytes);
                return buffer;
            } catch (Throwable e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(NativeByteBuffer result) {
            if (result != null) {
                native_applyDnsConfig(result.address);
            }
            currentTask = null;
        }
    }

    private static class DnsLoadTask extends AsyncTask<Void, Void, NativeByteBuffer> {

        protected NativeByteBuffer doInBackground(Void... voids) {
            try {
                URL downloadUrl;
                if (native_isTestBackend() != 0) {
                    downloadUrl = new URL("https://google.com/test/");
                } else {
                    downloadUrl = new URL("https://google.com");
                }
                URLConnection httpConnection = downloadUrl.openConnection();
                httpConnection.addRequestProperty("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 10_0 like Mac OS X) AppleWebKit/602.1.38 (KHTML, like Gecko) Version/10.0 Mobile/14A5297c Safari/602.1");
                httpConnection.addRequestProperty("Host", String.format(Locale.US, "dns-telegram%1$s.appspot.com", dnsConfigVersion == 0 ? "" : "" + dnsConfigVersion));
                httpConnection.setConnectTimeout(5000);
                httpConnection.setReadTimeout(5000);
                httpConnection.connect();
                InputStream httpConnectionStream = httpConnection.getInputStream();

                ByteArrayOutputStream outbuf = new ByteArrayOutputStream();

                byte[] data = new byte[1024 * 32];
                while (true) {
                    if (isCancelled()) {
                        break;
                    }
                    int read = httpConnectionStream.read(data);
                    if (read > 0) {
                        outbuf.write(data, 0, read);
                    } else if (read == -1) {
                        break;
                    } else {
                        break;
                    }
                }
                try {
                    if (httpConnectionStream != null) {
                        httpConnectionStream.close();
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                }
                byte[] bytes = Base64.decode(outbuf.toByteArray(), Base64.DEFAULT);
                NativeByteBuffer buffer = new NativeByteBuffer(bytes.length);
                buffer.writeBytes(bytes);
                return buffer;
            } catch (Throwable e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(NativeByteBuffer result) {
            if (result != null) {
                currentTask = null;
                native_applyDnsConfig(result.address);
            } else {
                DnsTxtLoadTask task = new DnsTxtLoadTask();
                task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null, null, null);
                currentTask = task;
            }
        }
    }
}
