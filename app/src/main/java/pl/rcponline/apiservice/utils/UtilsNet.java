package pl.rcponline.apiservice.utils;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import java.io.IOException;

public class UtilsNet {

    private static final String TAG = "UtilsNet";
    public static boolean isSynchroNow = false;
//    private static Context context;

    public static boolean isOnline(Context context) {

        Runtime runtime = Runtime.getRuntime();
        try {

            Log.d(TAG, "START isOnline");
            sendingData("startSendingData",context);
            UtilsThread.logThreadSignature("SERWIS_IsOnline");

            Process ipProcess = runtime.exec("/system/bin/ping -c 1 8.8.8.8");
            int     exitValue = ipProcess.waitFor();
            return (exitValue == 0);

        } catch (IOException e){
            e.printStackTrace();
        }catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
            sendingData("endSendingData",context);
        }

        return false;
    }

    private static void sendingData(String typeSendingData, Context context){

        Intent intentLocalBroadcast = new Intent("localBroadcast");
        intentLocalBroadcast.putExtra("type", typeSendingData);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intentLocalBroadcast);
    }


}
