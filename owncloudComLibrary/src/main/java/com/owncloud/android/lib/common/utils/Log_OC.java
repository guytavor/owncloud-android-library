package com.owncloud.android.lib.common.utils;

import android.util.Log;

import com.owncloud.android.lib.BuildConfig;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class Log_OC {
    private static final String SIMPLE_DATE_FORMAT = "yyyy/MM/dd HH:mm:ss";
    private static final String LOG_FOLDER_NAME = "log";
    private static final long MAX_FILE_SIZE = 2000000; // 2MB

    private static String mOwncloudDataFolderLog = "owncloud_log";

    private static File mLogFile;
    private static File mFolder;
    private static BufferedWriter mBuf;

    private static String[] mLogFileNames = {
            "currentLog" + BuildConfig.BUILD_TYPE + ".txt",
            "olderLog" + BuildConfig.BUILD_TYPE + ".txt"
    };

    private static boolean isMaxFileSizeReached = false;
    private static boolean isEnabled = false;

    public static void setLogDataFolder(String logFolder) {
        mOwncloudDataFolderLog = logFolder;
    }

    public static void i(String TAG, String message) {
        Log.i(TAG, message);
        appendLog("I: " + TAG + " : " + message);
    }

    public static void d(String TAG, String message) {
        Log.d(TAG, message);
        appendLog("D: " + TAG + " : " + message);
    }

    public static void d(String TAG, String message, Exception e) {
        Log.d(TAG, message, e);
        appendLog("D: " + TAG + " : " + message + " Exception : " + e.getStackTrace());
    }

    public static void e(String TAG, String message) {
        Log.e(TAG, message);
        appendLog("E: " + TAG + " : " + message);
    }

    public static void e(String TAG, String message, Throwable e) {
        Log.e(TAG, message, e);
        appendLog("E: " + TAG + " : " + message + " Exception : " + e.getStackTrace());
    }

    public static void v(String TAG, String message) {
        Log.v(TAG, message);
        appendLog("V: " + TAG + " : " + message);
    }

    public static void w(String TAG, String message) {
        Log.w(TAG, message);
        appendLog("W: " + TAG + " : " + message);
    }

    /**
     * Start doing logging
     *
     * @param storagePath : directory for keeping logs
     */
    synchronized public static void startLogging(String storagePath) {
        String logPath = storagePath + File.separator + mOwncloudDataFolderLog + File.separator + LOG_FOLDER_NAME;
        mFolder = new File(logPath);
        mLogFile = new File(mFolder + File.separator + mLogFileNames[0]);

        boolean isFileCreated = false;

        if (!mFolder.exists()) {
            mFolder.mkdirs();
            isFileCreated = true;
            Log.d("LOG_OC", "Log file created");
        }

        try {

            // Create the current log file if does not exist
            mLogFile.createNewFile();
            mBuf = new BufferedWriter(new FileWriter(mLogFile, true));
            isEnabled = true;

            if (isFileCreated) {
                appendPhoneInfo();
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (mBuf != null) {
                try {
                    mBuf.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    synchronized public static void stopLogging() {
        try {
            if (mBuf != null) {
                mBuf.close();
            }
            isEnabled = false;

            mLogFile = null;
            mFolder = null;
            mBuf = null;
            isMaxFileSizeReached = false;

        } catch (IOException e) {
            // Because we are stopping logging, we only log to Android console.
            Log.e("OC_Log", "Closing log file failed: ", e);
        } catch (Exception e) {
            // This catch should never fire because we do null check on mBuf.
            // But just for the sake of stability let's log this odd situation.
            // Because we are stopping logging, we only log to Android console.
            Log.e("OC_Log", "Stopping logging failed: ", e);
        }
    }

    /**
     * Delete history logging
     */
    public static void deleteHistoryLogging() {
        File folderLogs = new File(mFolder + File.separator);
        if (folderLogs.isDirectory()) {
            String[] myFiles = folderLogs.list();
            for (String fileName : myFiles) {
                File fileInFolder = new File(folderLogs, fileName);
                Log_OC.d("delete file", fileInFolder.getAbsoluteFile() + " " + fileInFolder.delete());
            }
        }
    }

    /**
     * Append the info of the device
     */
    private static void appendPhoneInfo() {
        appendLog("Model : " + android.os.Build.MODEL);
        appendLog("Brand : " + android.os.Build.BRAND);
        appendLog("Product : " + android.os.Build.PRODUCT);
        appendLog("Device : " + android.os.Build.DEVICE);
        appendLog("Version-Codename : " + android.os.Build.VERSION.CODENAME);
        appendLog("Version-Release : " + android.os.Build.VERSION.RELEASE);
    }

    /**
     * Append to the log file the info passed
     *
     * @param text : text for adding to the log file
     */
    synchronized private static void appendLog(String text) {

        if (isEnabled) {

            if (isMaxFileSizeReached) {

                // Move current log file info to another file (old logs)
                File olderFile = new File(mFolder + File.separator + mLogFileNames[1]);
                if (mLogFile.exists()) {
                    mLogFile.renameTo(olderFile);
                }

                // Construct a new file for current log info
                mLogFile = new File(mFolder + File.separator + mLogFileNames[0]);
                isMaxFileSizeReached = false;
            }

            String timeStamp = new SimpleDateFormat(SIMPLE_DATE_FORMAT, Locale.ENGLISH).format(Calendar.getInstance().getTime());

            try {
                mBuf = new BufferedWriter(new FileWriter(mLogFile, true));
                mBuf.newLine();
                mBuf.write(timeStamp + " " + text);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    mBuf.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            // Check if current log file size is bigger than the max file size defined
            if (mLogFile.length() > MAX_FILE_SIZE) {
                isMaxFileSizeReached = true;
            }
        }
    }

    public static String[] getLogFileNames() {
        return mLogFileNames;
    }
}
