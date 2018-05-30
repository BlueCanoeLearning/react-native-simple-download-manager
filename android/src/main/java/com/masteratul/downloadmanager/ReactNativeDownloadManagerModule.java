package com.masteratul.downloadmanager;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.util.LongSparseArray;
import android.util.Log;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.bridge.Promise;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.io.File;
import java.io.FilenameFilter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class ReactNativeDownloadManagerModule extends ReactContextBaseJavaModule {
    private static final String TAG = "simple-download-manager";

    private Downloader downloader;
    private LongSparseArray<Callback> appDownloads;
    private long currentDownloadId = 0;

    BroadcastReceiver downloadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (currentDownloadId != 0) {
                WritableMap downloadStatus = downloader.checkDownloadStatus(currentDownloadId);
                sendUpdate(getReactApplicationContext(), "expansion-dl-status", downloadStatus);
            }

            // try {
            //     long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            //     if (appDownloads.indexOfKey(downloadId) >= 0) {
            //         WritableMap downloadStatus = downloader.checkDownloadStatus(downloadId);
            //         Callback downloadOnDoneCb = appDownloads.get(downloadId);

            //         if (downloadStatus.getString("status").equalsIgnoreCase("STATUS_SUCCESSFUL")) {
            //             downloadOnDoneCb.invoke(null, downloadStatus);
            //         } else {
            //             downloadOnDoneCb.invoke(downloadStatus, null);
            //         }
            //         appDownloads.remove(downloadId);
            //     }

            // } catch (Exception e) {
            //     Log.e("RN_DOWNLOAD_MANAGER", Log.getStackTraceString(e));
            // }
        }
    };

    public ReactNativeDownloadManagerModule(ReactApplicationContext reactContext) {
        super(reactContext);
        downloader = new Downloader(reactContext);
        appDownloads = new LongSparseArray<>();
        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        reactContext.registerReceiver(downloadReceiver, filter);
    }

    @Override
    public String getName() {
        return "ReactNativeDownloadManager";
    }

    @ReactMethod
    public void download(String url, ReadableMap headers, ReadableMap config, Callback onDone) {
        try {
            DownloadManager.Request request = downloader.createRequest(url, headers, config);
            long downloadId = downloader.queueDownload(request);
            appDownloads.put(downloadId, onDone);
        } catch (Exception e) {
            onDone.invoke(this.createResponse("", e.getMessage(), "STATUS_FAILED"), null);
        }
    }

    @ReactMethod
    public void queueDownload(String url, ReadableMap headers, ReadableMap config, Callback onStart) {
        try {
            DownloadManager.Request request = downloader.createRequest(url, headers, config);
            long downloadId = downloader.queueDownload(request);
            currentDownloadId = downloadId;
            WritableMap result = downloader.checkDownloadStatus(downloadId);
            onStart.invoke(null, result);
        } catch (Exception e) {
            onStart.invoke(this.createResponse("", e.getMessage(), "STATUS_FAILED"), null);
        }
    }

    @ReactMethod
    public void attachOnCompleteListener(String downloadId, Callback onComplete) {
        try {
            long dloadId = Long.parseLong(downloadId);
            appDownloads.put(dloadId, onComplete);
            WritableMap status = downloader.checkDownloadStatus(Long.parseLong(downloadId));
            ArrayList<String> alreadyDoneStatuses = new ArrayList<>(Arrays.asList("STATUS_SUCCESSFUL", "STATUS_FAILED"));
            String currentStatus = status.getString("status");
            if (alreadyDoneStatuses.contains(currentStatus)) {
                appDownloads.remove(dloadId);
                onComplete.invoke(null, status);
            }
        } catch (Exception e) {
            onComplete.invoke(e.getMessage(), null);
        }
    }

    @ReactMethod
    public void hasDownloadedExpansionFile(final String expansionVersion, final Promise promise) {
        try {
            File obbDir = this.getReactApplicationContext().getObbDir();
            String obbPath = obbDir.getAbsolutePath();
            // Log.i(TAG, "Obb Directory: " + obbPath);
            File[] expansionFiles = obbDir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.toLowerCase().startsWith("main." + expansionVersion) && name.toLowerCase().endsWith(".obb");
                }
            });

            if (expansionFiles == null) {
                promise.reject("NULL_EXPANSION_FILES", "obbDir.listFiles() returned null");
                return;
            }

            switch (expansionFiles.length) {
            case 0:
                promise.resolve(false);
                break;
            case 1:
                promise.resolve(true);
                break;
            default:
                promise.reject("MULTIPLE_EXPANSION_FILES", "More than one expansion file found. This should never happen.");
                break;
            }
        } catch (Exception ex) {
            promise.reject(ex);
        }
    }


    @ReactMethod
    public void cancel(String downloadId, Callback onCancel) {
        try {
            downloader.cancelDownload(Long.parseLong(downloadId));
            onCancel.invoke(null, downloadId);
        } catch (Exception e) {
            onCancel.invoke(e.getMessage(), null);
        }
    }

    @ReactMethod
    public void checkStatus(String downloadId, Callback onStatus) {
        try {
            WritableMap status = downloader.checkDownloadStatus(Long.parseLong(downloadId));
            onStatus.invoke(null, status);
        } catch (Exception e) {
            onStatus.invoke(e.getMessage(), null);
        }
    }

    private WritableMap createResponse(String downloadId, String reason, String status) {
        WritableMap wmap = new WritableNativeMap();
        wmap.putString("downloadId", downloadId);
        wmap.putString("reason", reason);
        wmap.putString("status", status);
        return wmap;
    }

    private void sendUpdate(ReactApplicationContext reactContext, String eventName, WritableMap params) {
        reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(eventName, params);
    }

}
