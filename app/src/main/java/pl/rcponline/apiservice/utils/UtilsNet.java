package pl.rcponline.apiservice.utils;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;

import pl.rcponline.nfc.Const;
import pl.rcponline.nfc.R;
import pl.rcponline.nfc.SessionManager;
import pl.rcponline.nfc.dao.DAO;
import pl.rcponline.nfc.dao.EventDAO;
import pl.rcponline.nfc.model.Event;

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

    public static boolean synchoWithServer(final Context context, boolean isSynchroObligatory) {


//        context = mContext;
        EventDAO eventDAO = new EventDAO(context);
        List<Event> events = eventDAO.getEventsWithStatus(0);

        /*
        * zdarzenia sa i nieobowiazkowy   => wysyla
        * zdarzenia brak i nieobowiazkowy => niewysyla
        * zdarzenia sa i obowiazkowy      => wysyla
        * zadarzenia brak i obowiazkowy   => wysyla
         */
        if (!events.isEmpty() || isSynchroObligatory) {

            //jesli sa to wysylamy eventy ze statusem 0
            Gson g = new Gson();
            Type type = new TypeToken<List<Event>>() {}.getType();
            String eventsString = g.toJson(events, type);
            Log.d(TAG, eventsString);
            HashMap<String, Object> eventsJSONObject = new HashMap<String, Object>();
            SessionManager session = new SessionManager(context);

            eventsJSONObject.put(Const.LOGIN_API_KEY, session.getLogin());
            eventsJSONObject.put(Const.PASSWORD_API_KEY, session.getPassword());
            eventsJSONObject.put(Const.EVENTS_API_KEY, eventsString);
            Log.d(TAG, eventsJSONObject.toString());

            AQuery aq = new AQuery(context);
            String url = Const.ADD_EVENTS_URL;

            aq.ajax(url, eventsJSONObject, JSONObject.class, new AjaxCallback<JSONObject>() {
                @Override
                public void callback(String url, JSONObject json, AjaxStatus status) {
                    String message = "";

                    if (json != null) {
                        if (json.optBoolean("success") == true) {
                            Log.d(TAG, "success=true");
                            DAO.saveAllDataFromServer(json, context);
                        } else {
                            Log.d(TAG, "success=false");
                            message = json.optString("message");
                        }

                    } else {
                        //TODO co z tymi errorami zrobic???

                        //Kiedy kod 500( Internal Server Error)
                        if (status.getCode() == 500) {
                            message = context.getString(R.string.error_500);

                            //Błąd 404 (Not found)
                        } else if (status.getCode() == 404) {
                            message = context.getString(R.string.error_404);

                            //500 lub 404
                        } else {
                            message = context.getString(R.string.error_unexpected);
                        }
                    }
                    if (message != "") {
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                    }


                    //Synchro END
                    isSynchroNow = false;

                    Log.d(TAG, "_MSG=" + message);
                    Log.d(TAG, "_SYNCH=" + String.valueOf(isSynchroNow));
                }
            });
        }
        return true;
    }
}
