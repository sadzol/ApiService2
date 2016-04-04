package pl.rcponline.apiservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.util.Log;
import android.widget.Toast;

import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import pl.rcponline.apiservice.dao.DAO;

public class OnlineListener extends BroadcastReceiver {

    private final static String TAG = "ONLINE_Listener";
    private static boolean firstConnect = true;

    @Override
    public void onReceive(final Context context, Intent intent) {

        //todo Czy wywoła sie ta intentcja także po wejsciu w strefę online wifi ?  (zarazem musiałoby być i wyjściu z zasiegu sieci)

        if (!intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION) &&
                !intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION) &&
                !intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            return;
        }

        ConnectivityManager cm = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE));
        if (cm == null) {
            return;
        }

        // Sprawdza czy jesteśmy online
        if (cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected()) {
            //TODO powinno sie tez pojawic kiedy wszedl w siec wi-fi
            Log.d(TAG, "wl neta");
            Log.d(TAG, "ONLINE");

            if (firstConnect) {
                DbAdapter db = new DbAdapter(context);
                Cursor c = db.getEventsWithStatus(0);
                List<Event> events = db.cursorToEvents(c);
                db.close();
                //jeśli nie ma niewysłanych eventów to konczymy
                if (events.isEmpty()) {
                    Log.d(TAG, "NIE MA EVENTOW DO WYSLANIA");
                    return;
                } else {
                    Log.d(TAG, String.valueOf(events.size()));
                }

                //jeśli są to wysyłamy eventy ze statusem 0
                Gson g = new Gson();
                Type type = new TypeToken<List<Event>>() {}.getType();
                String eventsString = g.toJson(events, type);

                HashMap<String, Object> eventsJSONObject = new HashMap<String, Object>();
                SessionManager session = new SessionManager(context);

                eventsJSONObject.put(Const.LOGIN_API_KEY, session.getLogin());
                eventsJSONObject.put(Const.PASSWORD_API_KEY, session.getPassword());
                eventsJSONObject.put(Const.EVENTS_API_KEY, eventsString);

                AQuery aq = new AQuery(context);
                String url = Const.ADD_EVENTS_URL;
                aq.ajax(url, eventsJSONObject, JSONObject.class, new AjaxCallback<JSONObject>() {
                    @Override
                    public void callback(String url, JSONObject json, AjaxStatus status) {
                        //super.callback(url, object, status);
                        String message = "";

                        if (json != null) {
                            Log.d(TAG,"JSON NO NULL");
                            if (json.optBoolean("success") == true) {

                                DAO.saveLastEventsFromServer(json, context);
//                                Gson gson = new Gson();
//                                try {
//                                    //message = "suc" + status.getCode() + json.toString() + json.optString("message");
//
//                                    JsonParser parser = new JsonParser();
//                                    if (parser.parse(json.getString("events")).isJsonArray()) {
//                                        JsonArray jsonArrayEvents = parser.parse(json.getString("events")).getAsJsonArray();
//                                        //Log.d("EVENT", "is array");
//
//                                        for (int i = 0; i < jsonArrayEvents.size(); i++) {
//                                            Event eve = gson.fromJson(jsonArrayEvents.get(i).getAsString(), Event.class);
//                                            Log.d("API", eve.getDatetime());
//                                            eventsServer.add(eve);
//                                        }
//
//                                    } else {
//                                        //Log.d("EVENT", "is no array");
//                                        Event eve = gson.fromJson(json.getString("events"), Event.class);
//                                        if (eve instanceof Event) {
//                                            eventsServer.add(eve);
//                                        }
//                                    }
//
//                                    //wyczyscic baze i dodac eventy
//                                    if (!eventsServer.isEmpty()) {
//
//                                        DbAdapter db2 = new DbAdapter(context);
//                                        db2.deleteTable();
//                                        for (int i = 0; i < eventsServer.size(); i++) {
//                                            db2.insertEvent(eventsServer.get(i));
//                                        }
//                                        db2.close();
//
//                                    }
//                                    //Log.d("EVENT", json.getString("events"));
//                                } catch (JSONException e1) {
//                                    e1.printStackTrace();
//                                    message = "Blad w przetwarzaniu JSON";
//                                    //TODO co z tymi informacjami ERORRAMI zrobic???
//                                }
                            } else {
                                //success fail (cos z serwerem)
                                //Log.i(TAG, "Success-false");
                                message = "Success-false - SerwerRCP: " + status.getCode() + ", " + status.getError() + ", " + json.toString() + json.optString("message");
                                //error = "SerwerRCP: " + status.getCode() + ", " + status.getError() + ", " + status.getMessage();
                            }

                        } else {
                            Log.d(TAG,"JSON IS NULL");
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

                        Log.i(TAG, message);
                    }
                });
                firstConnect = false;
            }
        } else {
            firstConnect = true;
            ///Toast.makeText(context, "OFFLINE",Toast.LENGTH_SHORT).show();
        }

    }
}
