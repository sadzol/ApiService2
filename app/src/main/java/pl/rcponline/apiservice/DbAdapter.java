package pl.rcponline.apiservice;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DbAdapter {

    private static final int DATABASE_VESRION = 4;
    private static final String DATABASE_NAME = "Rcp.db";
    //liczba wyswietlonych ostatnich eventow
    private static final String NUMBER_LAST_EVENTS = "6";
    private final String TAG = "DATABASE";

    //zmienna do przechowywania bazy
    private SQLiteDatabase db;
    private Context context;
    //helper do otwierania i aktualizowania bazy
    private DbHelper dbH;

    public DbAdapter(Context _context){
        context = _context;
        dbH = new DbHelper(_context, DATABASE_NAME, null, DATABASE_VESRION);
    }


    public static abstract class EventsInfo implements BaseColumns {

        public static final String TABLE_NAME = "events",
                //KEY_ID = "id",
                KEY_TYPE_ID = "type_id",
                KEY_SOURCE_ID = "source_id",
                KEY_DATETIME = "datetime",
                KEY_LOCATION = "location",
                KEY_GPS     = "gps",
                KEY_COMMENT = "comment",
                KEY_STATUS = "status";
    }

    private static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS " + EventsInfo.TABLE_NAME + "(" +
                EventsInfo._ID          +   " INTEGER PRIMARY KEY AUTOINCREMENT," +
                EventsInfo.KEY_TYPE_ID  +   " INTEGER,"+
                EventsInfo.KEY_SOURCE_ID+   " INTEGER,"+
                EventsInfo.KEY_DATETIME +   " DATETIME,"+
                EventsInfo.KEY_LOCATION +   " TEXT,"+
                EventsInfo.KEY_GPS      +   " TEXT,"+
                EventsInfo.KEY_COMMENT  +   " TEXT,"+
                EventsInfo.KEY_STATUS   +   " INTEGER DEFAULT 0);";

    private static final String DELETE_TABLE = "DROP TABLE IF EXISTS "+ EventsInfo.TABLE_NAME;

    //Otwieramy polaczenie z baza
    /*public DbAdapter open(){
        db = dbH.getWritableDatabase();
        return this;
    }*/


    //Zamykamy polaczenie z baza
    public void close(){
        db.close();
    }

    private static class DbHelper extends SQLiteOpenHelper{
        private final String TAG_DB_HELPER = "DBHelper";

        public DbHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
            super(context, name, factory, version);
        }

        @Override
        public void onCreate(SQLiteDatabase sqLiteDatabase) {
            sqLiteDatabase.execSQL(CREATE_TABLE);
            Log.i(TAG_DB_HELPER, "Table created");
        }

        @Override
        public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVer, int newVer) {
            sqLiteDatabase.execSQL(DELETE_TABLE);
            onCreate(sqLiteDatabase);

            Log.w(TAG_DB_HELPER, "Aktualizacja bazy z wersji " + oldVer + " do " + newVer + ". Wszystkie dane zostaną usunięte.");
        }
    }


    //METODY
    public long insertEvent(Event _event) {
        db = dbH.getWritableDatabase();

        Log.i(TAG, "Add Event: " +
                        "type: " + _event.getType() +
                        ", source: " + _event.getSource() +
                        ", datetime: " + _event.getDatetime() +
                        ", location: " + _event.getLocation() +
                        ", gps: " + _event.getGPS() +
                        ", comment: " + _event.getComment() +
                        ", status: " + _event.getStatus()
        );


        ContentValues cv = new ContentValues();
        cv.put(EventsInfo.KEY_TYPE_ID,  _event.getType());
        cv.put(EventsInfo.KEY_SOURCE_ID,_event.getSource());
        cv.put(EventsInfo.KEY_DATETIME, _event.getDatetime().toString());
        cv.put(EventsInfo.KEY_LOCATION, _event.getLocation());
        cv.put(EventsInfo.KEY_GPS, _event.getGPS());
        cv.put(EventsInfo.KEY_COMMENT,  _event.getComment());
        cv.put(EventsInfo.KEY_STATUS,   _event.getStatus());
        long result = db.insert(EventsInfo.TABLE_NAME, null, cv);
        Log.d("DATABASE","result: "+String.valueOf(result));
        return result;
    }

    public boolean updateEvent(long _index, Event _event){
        db = dbH.getWritableDatabase();

        String where = EventsInfo._ID + "=" + _index;
        ContentValues cv = new ContentValues();
        cv.put(EventsInfo.KEY_TYPE_ID,  _event.getType());
        cv.put(EventsInfo.KEY_SOURCE_ID,_event.getSource());
        cv.put(EventsInfo.KEY_DATETIME, _event.getDatetime().toString());
        cv.put(EventsInfo.KEY_LOCATION, _event.getLocation());
        cv.put(EventsInfo.KEY_GPS, _event.getGPS());
        cv.put(EventsInfo.KEY_COMMENT,  _event.getComment());
        cv.put(EventsInfo.KEY_STATUS,   _event.getStatus());

        return db.update(EventsInfo.TABLE_NAME,cv,where,null) > 0;

    }

    public Cursor getEventWithStatus(int status) {

        db = dbH.getReadableDatabase();
        //List<Event> events = new ArrayList<Event>();
        Cursor c = db.rawQuery("SELECT * FROM " + EventsInfo.TABLE_NAME + " WHERE status = ?", new String[]{Integer.toString(status)});
        return c;
    }

    public Cursor getLastEvents(){

        db = dbH.getReadableDatabase();

        //List<Event> events = new ArrayList<Event>();
        Cursor c = db.rawQuery("SELECT * FROM " + EventsInfo.TABLE_NAME + " ORDER BY "+ EventsInfo.KEY_DATETIME +" DESC  LIMIT 0,"+NUMBER_LAST_EVENTS ,null);//new String[]{numberEvents});
        return  c;
    }

    public int getLastEventType(){
        db = dbH.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + EventsInfo.TABLE_NAME + " ORDER BY "+ EventsInfo.KEY_DATETIME +" DESC LIMIT 0,1",null);
        int typeId = 0;
        if(c.moveToFirst()){
                typeId = c.getInt(c.getColumnIndex(EventsInfo.KEY_TYPE_ID));
        }
        c.close();
        return typeId;
    }

    public List<Event> cursorToEvents(Cursor c){
        List<Event> events = new ArrayList<Event>();
        if(c.moveToFirst()){

            do{
                    Event event = new Event(
                    c.getInt(c.getColumnIndex(EventsInfo._ID)),
                    c.getInt(c.getColumnIndex(EventsInfo.KEY_TYPE_ID)),
                    c.getInt(c.getColumnIndex(EventsInfo.KEY_SOURCE_ID)),
                    c.getString(c.getColumnIndex(EventsInfo.KEY_DATETIME)),
                    c.getString(c.getColumnIndex(EventsInfo.KEY_LOCATION)),
                    c.getString(c.getColumnIndex(EventsInfo.KEY_GPS)),
                    c.getString(c.getColumnIndex(EventsInfo.KEY_COMMENT)),
                    c.getInt(c.getColumnIndex(EventsInfo.KEY_STATUS)),
                    null);

                /*Event event = new Event(
                        c.getInt(0),
                        c.getInt(1),
                        c.getInt(2),
                        getDateTime(c.getString(3)),
                        c.getString(4),
                        c.getString(5),
                        c.getString(6),
                        c.getInt(7),
                        null
                );*/
                events.add(event);
            }while(c.moveToNext());
        }
        c.close();
        return events;

    }
    private String getDateTime(String dateTime) {

        String eventDate = new SimpleDateFormat("dd-MM-yyyy HH:mm").format(dateTime);
        /*try {
            Date eventDate = new SimpleDateFormat("dd-MM-yyyy HH:mm").parse(dateTime);

        } catch (ParseException e) {
            Log.i("Error data", "Problem z data");
            //e.printStackTrace()
        }*/
        return eventDate;
    }

    public void deleteTable(){
        db = dbH.getWritableDatabase();
        db.delete(EventsInfo.TABLE_NAME,null,null);

        Log.d(TAG, "Wyczysznono tabele Events");

    }

}
