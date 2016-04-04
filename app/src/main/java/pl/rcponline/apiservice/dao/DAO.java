package pl.rcponline.apiservice.dao;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import pl.rcponline.apiservice.DbAdapter;
import pl.rcponline.apiservice.Event;

public class DAO {

    private static final String TAG = "DAO";

    public static void saveLastEventsFromServer(JSONObject json, final Context context){

        String message = "";
        ArrayList<Event> eventsServer = new ArrayList<Event>();
        Gson gson = new Gson();
        try {
            //message = "suc" + status.getCode() + json.toString() + json.optString("message");

            JsonParser parser = new JsonParser();
            if (parser.parse(json.getString("events")).isJsonArray()) {
                JsonArray jsonArrayEvents = parser.parse(json.getString("events")).getAsJsonArray();
                Log.d(TAG, "is array");

                for (int i = 0; i < jsonArrayEvents.size(); i++) {
                    Event eve = gson.fromJson(jsonArrayEvents.get(i).getAsString(), Event.class);
                    //Log.d(TAG, eve.getDatetime());
                    eventsServer.add(eve);
                }

            } else {
                Log.d(TAG, "is no array");
                Event eve = gson.fromJson(json.getString("events"), Event.class);
                if (eve instanceof Event) {
                    eventsServer.add(eve);
                }
            }

            //wyczyscic baze i dodac eventy
            if (!eventsServer.isEmpty()) {

                DbAdapter db2 = new DbAdapter(context);
                db2.deleteTable();
                for (int i = 0; i < eventsServer.size(); i++) {
                    db2.insertEvent(eventsServer.get(i));
                }
                db2.close();

            }
            //Log.d("EVENT", json.getString("events"));
        } catch (JSONException e1) {
            e1.printStackTrace();
            message = "Blad w przetwarzaniu JSON";
//            Log.d(TAG,message);
            //TODO co z tymi informacjami ERORRAMI zrobic???
        }

        Log.d(TAG, message);
    }
}
