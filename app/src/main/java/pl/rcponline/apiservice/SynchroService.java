package pl.rcponline.apiservice;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;

import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pl.rcponline.apiservice.dao.DAO;
import pl.rcponline.apiservice.eventbus.BusSendingData;
import pl.rcponline.apiservice.utils.UtilsThread;

public class SynchroService extends IntentService {

    //TODO WAZNE jesli wykonujemy serwis tworzy osobne watki wtedy za kazdym razem jest nowy egzemplarz i jest ustawianiane domysne ustawiena np test="test0"
    //TODO !!! ALE jezeli wywolamy serwis a w tym czasie juz ten serwis dziala(pracuje w tle) tworzy sie kolejka i jest ona wykonywana w tym samym WATKU ! zmienne ani KONSTRUKTOR sie nie resetuja/wykonuja
    //TODO DZIALAMY na tym SAMYM OBIEKCIE !   trzeba by nadpisac funkcje onStartCommand

    private String TAG = "SERWIS_SS";
    private Context context;
    private AQuery aq;

    public SynchroService() {
        super("SynchroService");

        this.context = this;
        aq = new AQuery(context);

        UtilsThread.logThreadSignature("SERWIS_CONSTRUCTOR");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        UtilsThread.logThreadSignature("SERWIS_IN_SYNCHRO_SERVICE");

        String message = null;
        String type = "";
        String error = null;
        Bundle extras =  intent.getExtras();
        String from = extras.getString("from");
        if(from.equals("synchro")){
            String synchroType = extras.getString("synchroType","auto");
            if(synchroType.equals("manual")){
                type = "synchroManual";
            }else if(synchroType.equals("auto")){
                type = "synchroAuto";
            }
        }
        Log.d(TAG,type);
        if(isNetworkOn(context)){
            Log.d(TAG, "Network Local - ON");

/*
            if(type.equals("synchroManual")){
                EventBus.getDefault().post(new BusSendingData("startSendingData"));
            }
*/

            if (from.equals("synchro")) {
                String synchroType = extras.getString("synchroType","auto");
                DbAdapter db = new DbAdapter(context);
                Cursor c = db.getEventsWithStatus(0);
                List<Event> events = db.cursorToEvents(c);
                db.close();

                Log.d(TAG+"_SYNCHRO_TYPE",String.valueOf(synchroType));

                //Reczne synchronizuje-sciaga z serwera mimo ze nie ma nic do wyslania
                if(synchroType.equals("manual")){
                    EventBus.getDefault().post(new BusSendingData("startSendingData"));
                    aqSynchro(extras,events);

                    //Nie synchronizuje nie sciaga z serwera jesli nie ma nic do wyslania oszczzedzanie-optymalizacja
                }else{

                    //isOnine sprawdzamy po sprawdzeniu czy sa eventy 0
                    if(!events.isEmpty()){
                        aqSynchro(extras,events);
                    }else{
                        Log.d(TAG, "NIE MA EVENTOW DO WYSLANIA");
                    }
                }

            } else if (from.equals("event")) {
                EventBus.getDefault().post(new BusSendingData("startSendingData"));
                error = aqAddEvent(extras);
                //PO EventBus juz sie nic nie wykonuje
//                EventBus.getDefault().post(new BusSynchro("event", error));
            }

        }else{
            Log.d(TAG, "Network (WIFI/GSM) - OFF");
            error = "Network (WIFI/GSM) - OFF";
        }
        Log.d(TAG, String.valueOf(message));
//        EventBus.getDefault().post(new BusSynchro(type, error));
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        EventBus.getDefault().post(new BusSendingData("endSendingData"));
        super.onDestroy();
    }

    //ITS   WIFI AND GSM too
    private boolean isNetworkOn(Context context) {

        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return (netInfo != null && netInfo.isConnected());

    }
    private void aqSynchro(Bundle extras, List<Event> events){

        UtilsThread.logThreadSignature(TAG);

        //jeśli są to wysyłamy eventy ze statusem 0
        Gson g = new Gson();
        Type type = new TypeToken<List<Event>>() {}.getType();
        String eventsString = g.toJson(events, type);

        Log.d(TAG, eventsString);//LOG
        HashMap<String, Object> params = new HashMap<String, Object>();

        params.put(Const.LOGIN_API_KEY, extras.getString(Const.LOGIN_API_KEY));
        params.put(Const.PASSWORD_API_KEY, extras.getString(Const.PASSWORD_API_KEY));
        params.put(Const.EVENTS_API_KEY, eventsString);
        Log.d(TAG, params.toString());//LOG

        String url = Const.ADD_EVENTS_URL;
        //AQUERY
        AjaxCallback cb = new AjaxCallback();
        cb.url(url);
        cb.type(JSONObject.class);
        cb.params(params);
        aq.sync(cb);

        JSONObject json = (JSONObject) cb.getResult();
        AjaxStatus status = cb.getStatus();

        Log.d("SERWIS_CODE", String.valueOf(status.getCode()));
        Log.d("SERWIS_WYNIK", String.valueOf(json));

        //END AQUERY
        String message = "";
        if (json != null) {
            if (json.optBoolean("success") == true) {
                Log.d(TAG, "success=true");
                DAO.saveLastEventsFromServer(json, context);
            } else {
                Log.d(TAG, "success=false");
                message = json.optString("message");
            }

        } else {

            //Kiedy kod 500( Internal Server Error)
            if (status.getCode() == 500) {
                message = context.getString(R.string.error_500);

                //Błąd 405 (Cycle Network)
            } else if (status.getCode() == 405) {
                message = context.getString(R.string.error_405);

                //Błąd 404 (Not found)
            } else if (status.getCode() == 404) {
                message = context.getString(R.string.error_404);

                //500 lub 404
            } else {
                message = context.getString(R.string.error_unexpected);
            }
        }
        if (message != "") {
            Log.i(TAG, message);
        }

    }

    private String aqAddEvent(Bundle extras){

        String TAG = "SERWIS_ADD_EVENT";
        UtilsThread.logThreadSignature(TAG);

        Map<String,Object> params = new HashMap<String,Object>();
        params.put(Const.LOGIN_API_KEY, extras.getString(Const.LOGIN_API_KEY));
        params.put(Const.PASSWORD_API_KEY, extras.getString(Const.PASSWORD_API_KEY));
        params.put(Const.TYPE_ID_API_KEY, extras.getInt(Const.TYPE_ID_API_KEY));
        params.put(Const.SOURCE_ID_API_KEY, extras.getInt(Const.SOURCE_ID_API_KEY));
        params.put(Const.DATATIME_API_KEY, extras.getString(Const.DATATIME_API_KEY));
        params.put(Const.LOCATION_API_KEY, extras.getString(Const.LOCATION_API_KEY));
        params.put(Const.GPS_API_KEY,extras.getString(Const.GPS_API_KEY));
        params.put(Const.COMMENT_API_KEY, extras.getString(Const.COMMENT_API_KEY));

//        params.put(Const.IDENTIFICATOR_API_KEY, extras.getString(Const.IDENTIFICATOR_API_KEY));
//        params.put(Const.EMPLOYEE_ID_API_KEY, extras.getLong(Const.EMPLOYEE_ID_API_KEY));
//        params.put(Const.DEVICE_CODE_API_KEY, extras.getString(Const.DEVICE_CODE_API_KEY));

        long lastAddedEventId =  extras.getLong(Const.LAST_ADDED_EVENT_ID);

        String url = Const.ADD_EVENT_URL;

        Log.d(TAG, "API SEND: login=" + extras.getString(Const.LOGIN_API_KEY)
                + ", pass=" + extras.getString(Const.PASSWORD_API_KEY)
                + ", typeId=" + extras.getInt(Const.TYPE_ID_API_KEY)
                + ", sourceId=" + extras.getInt(Const.SOURCE_ID_API_KEY)
                + ", datatime=" + extras.getString(Const.DATATIME_API_KEY)
                + ", location=" + extras.getString(Const.LOCATION_API_KEY)
                + ", gps=" + extras.getString(Const.GPS_API_KEY));
//                + ", id=" + extras.getString(Const.IDENTIFICATOR_API_KEY));
//                + ", employeeId=" + extras.getLong(Const.EMPLOYEE_ID_API_KEY)
//                + ", device_code=" + extras.getString(Const.DEVICE_CODE_API_KEY));

        //TODO wysylane zdarzen 0 przed logowaniem zeby ich nie stracic its nesessery?
        AjaxCallback cb = new AjaxCallback();
        cb.url(url);
        cb.type(JSONObject.class);
        cb.params(params);

        aq.sync(cb);

        JSONObject json = (JSONObject) cb.getResult();
        AjaxStatus status = cb.getStatus();
        Log.d("SERWIS_CODE", String.valueOf(status.getCode()));
        Log.d("SERWIS_WYNIK", String.valueOf(json));

//        int isEventSend = 0;
        String message, error = null;
        if (json != null) {
            if (json.optBoolean("success") == true) {
                Log.i(TAG, "Succes-true");

                int isEventSend = 1;
                Log.d(TAG,"----"+String.valueOf(lastAddedEventId));

                DbAdapter db = new DbAdapter(context);
                db.updateEventStatus(lastAddedEventId, isEventSend);
                db.close();
                //message = "suc" + status.getCode() + json.toString() + json.optString("message");

            } else {
                Log.i(TAG, "Succes-false");
                message = json.optString("message");
                error = "BLAD! " + message;
            }
        } else {
            Log.i(TAG, "no json");

            message = String.valueOf(status.getCode())+" "+context.getString(R.string.error_unexpected);
            Log.d(TAG, message);
            //-101 to chyba ten problem
            //TODO Jesli -101 to ostani event status=2  ale co jesl pojawi sie kolejny event pracowniki a odpowiedz z poprzedniego przyjdzie dopiero po minutcie to wezmie ten 2 event.... hmm
            //NIE POKAZUJEMY BLEDU JESLI SERVER NIE ODPOWIADA
            //error = "BLAD! Polaczenie: " + status.getMessage();
        }

//        Log.d(TAG,"ZAPAMIETANY EVENT:"+String.valueOf(lastAddedEventId));
        String sendToUI = null;
        if(error != null){
            sendToUI = error;

            //Brak bledu komunikat o zapisaniu eventa
        }else{
            int resourceType = getResources().getIdentifier(String.valueOf(Const.EVENT_TYPE[extras.getInt(Const.TYPE_ID_API_KEY) - 1]), "string", getPackageName());
            sendToUI = context.getString(R.string.event_saved) + " " + context.getString(resourceType).toUpperCase();
        }

        return sendToUI;
    }

    private void test(){
        //        Map<String,Object> params = new HashMap<String,Object>();
//        params.put(Const.LOGIN_API_KEY, "sadzol@tlen.pl");
//        params.put(Const.PASSWORD_API_KEY, "sas");
//        String url = Const.LOGIN_URL;
////        String url = Const.URL_TEST;
//
////        Log.d(TAG, String.valueOf(events.size()));
//        //TODO wysylane zdarzen 0 przed logowaniem zeby ich nie stracic its nesessery?
//        AjaxCallback cb = new AjaxCallback();
//        cb.url(url);
//        cb.type(JSONObject.class);
//        cb.params(params);
//
//        aq.sync(cb);
//
//        JSONObject jo = (JSONObject) cb.getResult();
//        AjaxStatus status = cb.getStatus();
//        Log.d("SERWIS_CODE",String.valueOf(status.getCode()));
//        Log.d("SERWIS_WYNIK",String.valueOf(jo));
    }
}
