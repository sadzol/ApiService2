package pl.rcponline.apiservice;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.location.Location;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

public class MainActivity extends Activity implements View.OnClickListener,ConnectionCallbacks,OnConnectionFailedListener,LocationListener {


    private static final String TAG = "MAIN_ACTIVITY";
    private static final String JSON = "JSON";
    //private final String TAG_SESSION = "SESSION";
    private final String TAG_LOC = "LOCATION";

    //TextView dataTime;
    AQuery aq;
    String login, password, location, comment, data;
    int isEventSend, typeId,lastEvenTypeId;
    View lasViewEvent;
    // SESSION MANAGER CLASS
    SessionManager session;
    ProgressDialog pd;
    Context context;

    ImageButton btStart, btFinish, btBreakStart, btBreakFinish, btTempStart, btTempFinish;

    ImageView imSynchro,ivStartOff,ivFinishOff,ivBreakOff,ivTempOff;
    LinearLayout llDatatime,llLastEvent;
    RelativeLayout rlBreak, rlPayExit;
    EditText etComment;

    ///////////////LOCATION///////////////////////////////////////////////////////
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 1000;
    private final static int REQUEST_CHECK_SETTINGS = 1001;
    private Location mLastLocation;

    // Google client to interact with Google API
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;

    // Location updates intervals in sec
    private static int UPDATE_INTERVAL = 1000; // 1 sec
    private static int FATEST_INTERVAL = 1000; // 1 sec
    private static int DISPLACEMENT = 1; // 1 meters
    ////////////////////////////////////////////////////////////////////////////////


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Ustawiamy ustawienia domyślnymi warościami z pliku prefernecji (false-tylko raz)
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        //TODO TEZ TWORZYC TU WYSYWALNIE EVENTOW JESLI MAJA STATUS 0

        //Jesli user nie zalogowany przenies do strony logowania zamykajac ta aktywnosc
        session = new SessionManager(getApplicationContext());
        if (session.checkLogin()) {
            finish();
        }else{
            login = session.getLogin();
            password = session.getPassword();
        }

        //Wył. KLAWIATURE do czasu az pole tekstownie nie zostanie wybrane    (Disabled software keyboard in android until TextEdit is chosen)
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        ///////////LOC///////// First we need to check availability of play services
        if (checkPlayServices()) {
            //Toast.makeText(this,"IsPlayService",Toast.LENGTH_SHORT).show();
            // Building the GoogleApi client
            Log.d(TAG,"checkPlayService - OK");
            buildGoogleApiClient();
            createLocationRequest();
        }else{
            Log.d(TAG,"checkPlayService - NO");
        }
        //////////////////////////////////////////////////
        context = this;

        //Polaczenie z baza
        //dbAdapter = new DbAdapter(getApplicationContext());

        //Zegar w czasie rzeczywistym
        Runnable myRunnableThread = new CountDownRunner();
        Thread myThread = new Thread(myRunnableThread);
        myThread.start();

        //AQuery
        aq = new AQuery(getApplicationContext());

        //Inicjajca przyciskow
        btStart         = (ImageButton) findViewById(R.id.bt_start);
        btFinish        = (ImageButton) findViewById(R.id.bt_finish);
        btBreakStart    = (ImageButton) findViewById(R.id.bt_break_start);
        btBreakFinish   = (ImageButton) findViewById(R.id.bt_break_finish);
        btTempStart     = (ImageButton) findViewById(R.id.bt_temp_start);
        btTempFinish    = (ImageButton) findViewById(R.id.bt_temp_finish);

        ivStartOff = (ImageView) findViewById(R.id.iv_start_off);
        ivFinishOff= (ImageView) findViewById(R.id.iv_finish_off);
        ivBreakOff = (ImageView) findViewById(R.id.iv_break_off);
        ivTempOff  = (ImageView) findViewById(R.id.iv_temp_off);
//        btStart = (Button) findViewById(R.id.bt_start);
//        btFinish = (Button) findViewById(R.id.bt_finish);
//        btBreakStart = (Button) findViewById(R.id.bt_break_start);
//        btBreakFinish = (Button) findViewById(R.id.bt_break_finish);
//        btTempStart = (Button) findViewById(R.id.bt_temp_start);
//        btTempFinish = (Button) findViewById(R.id.bt_temp_finish);
        llDatatime = (LinearLayout)findViewById(R.id.ll_datatime);
        imSynchro = (ImageView) findViewById(R.id.im_synchronized);

        //Potrzebne dla ustawien widocznosci
        etComment = (EditText) findViewById(R.id.et_event_comment);
        llLastEvent =(LinearLayout) findViewById(R.id.ll_last_events);
        rlBreak = (RelativeLayout) findViewById(R.id.ll_pause);
        rlPayExit= (RelativeLayout) findViewById(R.id.ll_record);

        btStart.setOnClickListener(this);
        btFinish.setOnClickListener(this);
        btBreakStart.setOnClickListener(this);
        btBreakFinish.setOnClickListener(this);
        btTempStart.setOnClickListener(this);
        btTempFinish.setOnClickListener(this);
        imSynchro.setOnClickListener(this);

        lastEvenTypeId = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getInt(Const.LAST_EVENT_TYPE_ID, 6);
        Log.i(TAG, Integer.toString(lastEvenTypeId));

        //progres bar w przypadku zwiekszenia dokladnosci lokalizacji zeby miec czas na wyszukanie polozenia i zlapanie sieci wi-fi
        pd = new ProgressDialog(new ContextThemeWrapper(this, android.R.style.Theme_Holo_Dialog));
        pd.setCancelable(false);
        pd.setIndeterminate(true);
        pd.setTitle("");
        pd.setMessage(getString(R.string.searching));
        //dialogWait();
    }

    @Override
    protected void onStart() {

        super.onStart();
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }
    @Override
    protected void onStop() {
        super.onStop();

        if (mGoogleApiClient.isConnected()) {
            stopLocationUpdates();
            mGoogleApiClient.disconnect();
        }
        Log.d(TAG,"onStop");
    }

    @Override
    //onResume po odwroceniu urzadzenia
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        //checkPlayServices();


        // Resuming the periodic location updates
        if (mGoogleApiClient.isConnected()){
            //Toast.makeText(this,"StartLocationUpdates",Toast.LENGTH_SHORT).show();
            startLocationUpdates();
        }else{
            //Toast.makeText(this,"NOstartLocationUpdates",Toast.LENGTH_SHORT).show();
        }
        //locManager.requestLocationUpdates(locProvider, Const.TIME_INTERVAL, 2, locRcp);
        //todo ustawic przyciski i odswiezyc eventy
        setButtons();
        viewLastEvents();

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        if(sp.getBoolean("comment",false)){
            etComment.setVisibility(View.VISIBLE);
        }else {
            etComment.setVisibility(View.GONE);
        }
        if(sp.getBoolean("last_events",false)){
            llLastEvent.setVisibility(View.VISIBLE);
        }else{
            llLastEvent.setVisibility(View.GONE);
        }
        if(sp.getBoolean("break",false)){
            rlBreak.setVisibility(View.VISIBLE);
        }else{
            rlBreak.setVisibility(View.GONE);
        }
        if(sp.getBoolean("pay_exit",false)){
            rlPayExit.setVisibility(View.VISIBLE);
        }else {
            rlPayExit.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){

        //Przechwytuje opcje z górnego menu po prawej
        switch (item.getItemId()){
            case R.id.menu_settings:
                Intent intent  = new Intent(this,SettingsActivity.class);
                startActivity(intent);
                return true;
            case R.id.menu_logout:

                //todo logout moze service
                session.logout();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    //sprawdza czy masz w ustawianich wymuszona najlepsza lokalizacje
    boolean isBestLocationRequired(){
        SharedPreferences sh = PreferenceManager.getDefaultSharedPreferences(this);
        return sh.getBoolean("location",false);
    }

    @Override
    public void onClick(View v) {
        lasViewEvent = v;
//        Log.d(TAG,"cos");
//        Log.d(TAG,String.valueOf(v.getId()));
//        Log.d(TAG,String.valueOf(R.id.im_synchronized));
        if(v.getId() == R.id.im_synchronized){
            Log.d(TAG,"in");
            synchronizedWithServer();

        }else if(isBestLocationRequired()){
            viewDialogLocation();
        }else {
            startEvent();
        }


    }
    private void startEvent(){
        View v = lasViewEvent;
        switch (v.getId()) {
            case R.id.bt_start:
                lastEvenTypeId = 1;
                //SendEvent(1);
                break;
            case R.id.bt_finish:
                lastEvenTypeId = 6;
                //SendEvent(6);
                break;
            case R.id.bt_break_start:
                lastEvenTypeId = 2;
                //SendEvent(2);
                break;
            case R.id.bt_break_finish:
                lastEvenTypeId = 3;
                //SendEvent(3);
                break;
            case R.id.bt_temp_start:
                lastEvenTypeId = 4;
                //SendEvent(4);
                break;
            case R.id.bt_temp_finish:
                lastEvenTypeId = 5;
                //SendEvent(5);
                break;
        }
        Log.d(TAG, "EVENTQ " + lastEvenTypeId);

        if(SendEvent()) {

            //Dodaj ostani event do preferencji
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            SharedPreferences.Editor editor = sp.edit();
            editor.putInt(Const.LAST_EVENT_TYPE_ID, lastEvenTypeId);
            editor.commit();
        }
    }

    //To umiecic w EVENT.java-nie moge bo po wykonianu  aq.ajax nie bede mial wplywu na UI, a w mainActivity jest wpylyw na modyfikacje UI
    private boolean SendEvent() {

        //TODO SPRAWDZIC CZY JEST polaczenie jesli nie to nie uruchamiac aq.ajax
        typeId = lastEvenTypeId;//_typeId;
        String url = Const.ADD_EVENT_URL;
        data = getDateTime();
        comment = etComment.getText().toString();
        isEventSend = 0;

        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        location = getString(R.string.location_disabled);
        if(mLastLocation != null) {
            location = mLastLocation.getLatitude() + ";" + mLastLocation.getLongitude();
        }

        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

        //jesli jest ustawiona doklada lokalizacja a Internet i Lokalizacja nie zdazyly sie jeszcze uruchomic (wymaga to 1s,2s) to wyskauje okno ("grajace na czas")na wyczkanie.
        if(isBestLocationRequired()) {
            Log.d(TAG,"first");
            //if( (!(networkInfo != null && networkInfo.isConnected())) ||  (mLastLocation == null)){
            if( mLastLocation == null){
                Log.d(TAG, "w4");

                    DialogFragment df = new EventNoReadyDialog();
                    df.show(getFragmentManager(), "tag");
                    return false;
            }
        }
        if (networkInfo != null && networkInfo.isConnected()) {

            Log.d(TAG,"INTERNET-ON");
            //location = gpsTracker.getLatitude()+";"+gpsTracker.getLongitude();

            Map<String, Object> params = new HashMap<String, Object>();
            params.put(Const.LOGIN_API_KEY, login);
            params.put(Const.PASSWORD_API_KEY, password);
            //params.put(Const.LOGIN_API_KEY, "sadzol@tlen.pl");
            //params.put(Const.PASSWORD_API_KEY, "sas");
            params.put(Const.TYPE_ID_API_KEY, typeId);
            params.put(Const.SOURCE_ID_API_KEY, Const.SOURCE_ID);
            params.put(Const.DATATIME_API_KEY, data);
            params.put(Const.LOCATION_API_KEY, "");
            params.put(Const.GPS_API_KEY, location);
            params.put(Const.COMMENT_API_KEY, comment);

            ProgressDialog dialog = new ProgressDialog(this,ProgressDialog.THEME_HOLO_DARK);
            dialog.setIndeterminate(true);
            dialog.setCancelable(true);
            dialog.setInverseBackgroundForced(false);
            dialog.setCanceledOnTouchOutside(true);
            dialog.setMessage(getString(R.string.please_wait));

            aq.progress(dialog).ajax(url, params, JSONObject.class, new AjaxCallback<JSONObject>() {
                @Override
                public void callback(String url, JSONObject json, AjaxStatus status) {
                    String message, error = "";
                    if (json != null) {
                        if (json.optBoolean("success") == true) {
                            Log.i(TAG, "Succes");
                            isEventSend = 1;
                            //message = "suc" + status.getCode() + json.toString() + json.optString("message");

                        } else {
                            Log.i(TAG, "Succes-false");
                            //message = "no" + status.getCode() + json.toString() + json.optString("message");
                            error = "SerwerRCP: " + status.getCode() + ", " + status.getError() + ", " + status.getMessage();
                        }
                    } else {
                        Log.i(TAG, "no json");
                        //message = "Error:" + getString(R.string.no_connection) + "( " + status.getCode() + " )";// + json.optString("login");
                        error = "Poczaczenie: " + status.getCode() + ", " + status.getError() + ", " + status.getMessage();
                        Toast.makeText(getApplicationContext(), getString(R.string.no_connection), Toast.LENGTH_LONG).show();
                    }

                    saveEventToLocalDatabase(typeId,data,location,comment,isEventSend,error);
                    setButtons();
                    viewLastEvents();
                }
            });

        } else {
            //WYŁ. Internet z karty  DANE MOBILNE OFF
            Log.d(TAG,"INTERNET-OFF");
            saveEventToLocalDatabase(typeId, data, location,comment,isEventSend,"");

            setButtons();
            viewLastEvents();
        }
        return true;
    }

    private void saveEventToLocalDatabase(int typeId,String data, String gps, String comment, int isEventSend, String error){
        Event event = new Event(0, typeId, Const.SOURCE_ID, data, "", gps, comment, isEventSend, error);
        DbAdapter dbAdapter = new DbAdapter(getApplicationContext());
        dbAdapter.insertEvent(event);
        dbAdapter.close();
    }

    private String getDateTime() {
        SimpleDateFormat _format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String currentDataTimeStrong = _format.format(new Date());
        return currentDataTimeStrong;
    }

    /**
     * ZEGAR
     */
    public void doWork() {
        runOnUiThread(new Runnable() {
            public void run() {
                try {
                    TextView txtCurrentTime = (TextView) findViewById(R.id.tv_time);
                    Date dt = new Date();
                    //int hours = dt.getHours();
                    //int minutes = dt.getMinutes();
                    //int seconds = dt.getSeconds();
                    //String curTime = hours + ":" + minutes + ":" + seconds;
                    SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
                    String curTime = df.format(dt.getTime());
                    //String curTime = String.valueOf(dt.getTime());
                    txtCurrentTime.setText(curTime);
                } catch (Exception e) {
                    Log.d(TAG, e.toString());
                }
            }
        });
    }

    class CountDownRunner implements Runnable {
        // @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    doWork();
                    Thread.sleep(1000); // Pause of 1 Second
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    Log.d(TAG,e.toString());
                }
            }
        }
    }

    /**
     * WYSWIETLANIE LISTY OSTATNICH EVENTOW
     */
    public void  viewLastEvents() {

        //Pobieram ostatnie(6) eventy z bazy
        DbAdapter db = new DbAdapter(context);
        List<Event> lastEvents = db.cursorToEvents(db.getLastEvents());

        //Dodaje adapter do ListView
        ListView lv = (ListView) findViewById(R.id.lv_last_events);
        EventsAdapter2 adapter = new EventsAdapter2(this, lastEvents);
        adapter.notifyDataSetChanged(); //aktualizacja danych
        lv.setAdapter(adapter);

    }

    /**
     * ZMIANA STANU PRZYCISKÓW
     */
    private void setButtons() {
//        imgView .setVisibility(View.VISIBLE);
//        imgView .setVisibility(View.INVISIBLE);
//        imgView .setVisibility(View.GONE);
        btStart.setEnabled(false);
        btFinish.setEnabled(false);
        btBreakStart.setEnabled(false);
        btBreakFinish.setEnabled(false);
        btTempStart.setEnabled(false);
        btTempFinish.setEnabled(false);

        btStart.setVisibility(View.INVISIBLE);
        btFinish.setVisibility(View.INVISIBLE);
        btBreakStart.setVisibility(View.INVISIBLE);
        btBreakFinish.setVisibility(View.INVISIBLE);
        btTempStart.setVisibility(View.INVISIBLE);
        btTempFinish.setVisibility(View.INVISIBLE);

        ivStartOff.setVisibility(View.INVISIBLE);
        ivBreakOff.setVisibility(View.INVISIBLE);
        ivFinishOff.setVisibility(View.INVISIBLE);
        ivTempOff.setVisibility(View.INVISIBLE);
        //setButtonVisible(btStart);
        //setButtonVisible(btFinish);

        switch (lastEvenTypeId) {
            //FINISH
            case 6:
                btStart.setVisibility(View.VISIBLE);
                btStart.setEnabled(true);

                ivBreakOff.setVisibility(View.VISIBLE);
                ivTempOff.setVisibility(View.VISIBLE);
                ivFinishOff.setVisibility(View.VISIBLE);
                //setButtonVisible(btBreakStart);
                //setButtonVisible(btTempStart);

                //setButtonEnabled(btStart);
                //btStart.setBackgroundResource(R.drawable.gradient3_green);
                llDatatime.setBackgroundResource(R.drawable.gradient_red);
                break;

            //BREAK START
            case 2:
                btBreakFinish.setVisibility(View.VISIBLE);
                btBreakFinish.setEnabled(true);

                ivStartOff.setVisibility(View.VISIBLE);
                ivFinishOff.setVisibility(View.VISIBLE);
                ivTempOff.setVisibility(View.VISIBLE);
                //setButtonVisible(btBreakFinish);
                //setButtonVisible(btTempStart);

                //setButtonEnabled(btBreakFinish);
                //btBreakFinish.setBackgroundResource(R.drawable.gradient3_green);
                llDatatime.setBackgroundResource(R.drawable.gradient_blue);

                break;

            //TEMP START
            case 4:

                btTempFinish.setVisibility(View.VISIBLE);
                btTempFinish.setEnabled(true);
                //setButtonVisible(btBreakStart);
                //setButtonVisible(btTempFinish);

                ivStartOff.setVisibility(View.VISIBLE);
                ivBreakOff.setVisibility(View.VISIBLE);
                ivFinishOff.setVisibility(View.VISIBLE);
                //setButtonEnabled(btTempFinish);
                //btTempFinish.setBackgroundResource(R.drawable.gradient3_green);
                llDatatime.setBackgroundResource(R.drawable.gradient_orange);
                break;

            //BREAK FINISH
            case 3:

            //TEMP FINISH
            case 5:

            //START
            case 1:
                btTempStart.setVisibility(View.VISIBLE);
                btTempStart.setEnabled(true);
                btBreakStart.setVisibility(View.VISIBLE);
                btBreakStart.setEnabled(true);
                btFinish.setVisibility(View.VISIBLE);
                btFinish.setEnabled(true);

                //setButtonVisible(btBreakStart);
                //setButtonVisible(btTempStart);
                ivStartOff.setVisibility(View.VISIBLE);
                //setButtonEnabled(btBreakStart);
                //setButtonEnabled(btTempStart);
                //setButtonEnabled(btFinish);

//                btBreakStart.setBackgroundResource(R.drawable.gradient3_blue);
//                btTempStart.setBackgroundResource(R.drawable.gradient3_orange);
//                btFinish.setBackgroundResource(R.drawable.gradient3_red);
                llDatatime.setBackgroundResource(R.drawable.gradient_green);
                break;
            default:

                btTempStart.setVisibility(View.VISIBLE);
                btTempStart.setEnabled(true);
                btBreakStart.setVisibility(View.VISIBLE);
                btBreakStart.setEnabled(true);
                btFinish.setVisibility(View.VISIBLE);
                btFinish.setEnabled(true);

                ivBreakOff.setVisibility(View.VISIBLE);
                ivFinishOff.setVisibility(View.VISIBLE);
                ivTempOff.setVisibility(View.VISIBLE);
//                setButtonVisible(btBreakStart);
//                setButtonVisible(btTempStart);
//
//                setButtonEnabled(btBreakStart);
//                setButtonEnabled(btTempStart);
//                setButtonEnabled(btFinish);
//
//                btBreakStart.setBackgroundResource(R.drawable.gradient3_blue);
//                btTempStart.setBackgroundResource(R.drawable.gradient3_orange);
//                btFinish.setBackgroundResource(R.drawable.gradient3_red);
                llDatatime.setBackgroundResource(R.drawable.gradient_green);
                break;
        }
    }
    private void setButtonsOld() {

        btStart.setEnabled(false);
        btFinish.setEnabled(false);
        btBreakStart.setEnabled(false);
        btBreakFinish.setEnabled(false);
        btTempStart.setEnabled(false);
        btTempFinish.setEnabled(false);

        btBreakStart.setVisibility(View.GONE);
        btBreakFinish.setVisibility(View.GONE);
        btTempStart.setVisibility(View.GONE);
        btTempFinish.setVisibility(View.GONE);

        setButtonVisible(btStart);
        setButtonVisible(btFinish);

        switch (lastEvenTypeId) {
            //FINISH
            case 6:
                setButtonVisible(btBreakStart);
                setButtonVisible(btTempStart);

                setButtonEnabled(btStart);
                btStart.setBackgroundResource(R.drawable.gradient3_green);
                llDatatime.setBackgroundResource(R.drawable.gradient_red);
                break;

            //BREAK START
            case 2:
                setButtonVisible(btBreakFinish);
                setButtonVisible(btTempStart);

                setButtonEnabled(btBreakFinish);
                btBreakFinish.setBackgroundResource(R.drawable.gradient3_green);
                llDatatime.setBackgroundResource(R.drawable.gradient_blue);

                break;

            //TEMP START
            case 4:

                setButtonVisible(btBreakStart);
                setButtonVisible(btTempFinish);

                setButtonEnabled(btTempFinish);
                btTempFinish.setBackgroundResource(R.drawable.gradient3_green);
                llDatatime.setBackgroundResource(R.drawable.gradient_orange);
                break;

            //BREAK FINISH
            case 3:
                //TEMP FINISH
            case 5:
                //START
            case 1:
                setButtonVisible(btBreakStart);
                setButtonVisible(btTempStart);

                setButtonEnabled(btBreakStart);
                setButtonEnabled(btTempStart);
                setButtonEnabled(btFinish);

                btBreakStart.setBackgroundResource(R.drawable.gradient3_blue);
                btTempStart.setBackgroundResource(R.drawable.gradient3_orange);
                btFinish.setBackgroundResource(R.drawable.gradient3_red);
                llDatatime.setBackgroundResource(R.drawable.gradient_green);
                break;
            default:
                setButtonVisible(btBreakStart);
                setButtonVisible(btTempStart);

                setButtonEnabled(btBreakStart);
                setButtonEnabled(btTempStart);
                setButtonEnabled(btFinish);

                btBreakStart.setBackgroundResource(R.drawable.gradient3_blue);
                btTempStart.setBackgroundResource(R.drawable.gradient3_orange);
                btFinish.setBackgroundResource(R.drawable.gradient3_red);
                llDatatime.setBackgroundResource(R.drawable.gradient_green);
                break;
        }
    }

    private void setButtonVisible(ImageButton button) {
//        button.setVisibility(View.VISIBLE);
//        button.setBackgroundResource(R.drawable.gradient3_grey);
        //button.setTextColor(Color.parseColor("#9c9c9c"));
    }

    private void setButtonEnabled(ImageButton button) {
//        button.setEnabled(true);
        //button.setTextColor(Color.WHITE);
    }


    /////////LOCATION//////////////////

    /**
     * Creating google api client object
     */
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();
    }

    /**
     * Creating location request object
     */
    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest()
                .setInterval(UPDATE_INTERVAL)
                .setFastestInterval(FATEST_INTERVAL)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setSmallestDisplacement(DISPLACEMENT);
    }

    /**
     * Okno dialogowy wyswietlajace przycisk do wl. lokalizacji
     */
    protected void viewDialogLocation(){
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setFastestInterval(0)
                .setInterval(0)
                .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);


        LocationSettingsRequest builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest)
                .setAlwaysShow(true)
                .build();

        PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder);
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult result) {
                final Status status = result.getStatus();
                final LocationSettingsStates state = result.getLocationSettingsStates();
                String txt = "";
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        //ustawienia lokalizacji sa wystarczajace, mozna wyzwolic zdarzenie
                        Log.d(TAG, "EVENTQ - sett -succes");
                        startEvent();

                        break;

                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        //ustawienia lokalizacji nie sa wystarczajace, ale moga byc poprawione przez okno opcji
                        try {
                            Log.d(TAG, "EVENTQ - sett -niewystarczajace");
                            //wyswietla okno dialogowe ktore zwraca wynik w metodzie onActivityResult()
                            status.startResolutionForResult(MainActivity.this, REQUEST_CHECK_SETTINGS);
                        } catch (IntentSender.SendIntentException e) {
                            // Ignore the error.
                            Toast.makeText(getApplicationContext(), "Błąd z oknem lokalizacji", Toast.LENGTH_LONG).show();
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        //ustawienia lokalizacji nie wystarczajace, ale nie mozna ich zmienic, okno dialogowe sie nie wyswietla
                        Toast.makeText(getApplicationContext(), "Ustawienia lokazlizacji mało dokładne, nie można zmienić", Toast.LENGTH_LONG).show();
                        break;
                }
            }
        });
    }

    /**
     * Odpowiedz na wynik z okna dialogowego odpowiedzialnego za ustawienia lokalizacji
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //super.onActivityResult(requestCode, resultCode, data);
        final LocationSettingsStates states = LocationSettingsStates.fromIntent(data);
        String txt = "";
        String s= Integer.toString(resultCode);
        Log.d(TAG,"EVENTQ "+s);
        switch (requestCode) {
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:

                        //uzytkownik zmienil ustawienia lokalizacji na wymagane
                        Log.d(TAG, "EVENTQ - sett -ok");
                        //startEvent();
                        break;
                    case Activity.RESULT_CANCELED:
                        //uzytkownik zapytany o wl. lokalizacji odpowiedzial nie
                        Log.d(TAG, "EVENTQ - sett -cancel");

                        //Toast.makeText(this,getString(R.string.setting_location_on),Toast.LENGTH_LONG).show();
                        break;
                    default:
                        break;
                }
                break;
            case PLAY_SERVICES_RESOLUTION_REQUEST:
                if (resultCode == RESULT_CANCELED) {
                    Toast.makeText(this, "Google Play Services must be installed.",Toast.LENGTH_SHORT).show();
                    Log.d(TAG,"Services must be installed");
                    finish();
                }
                break;
        }

    }

    /**
     * Method to verify google play services on the device
     */
    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,PLAY_SERVICES_RESOLUTION_REQUEST).show();
                Log.d(TAG, "checkPlayServices : ConnectionResult.FALSE: isUserRecoverableError :"+resultCode);
            } else {
                Log.d(TAG,"checkPlayServices: This device is not supported.");
                Toast.makeText(getApplicationContext(), "This device is not supported.", Toast.LENGTH_LONG).show();
                finish();
            }
            return false;

        }else{
            Log.d(TAG,"checkPlayServices: ConnectionResult.TRUE");
            return true;
        }
    }

    /**
     * Starting the location updates
     */
    protected void startLocationUpdates() {
        //Toast.makeText(getApplicationContext(),"startLocationUpdates", Toast.LENGTH_LONG).show();
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    /**
     * Stopping location updates
     */
    protected void stopLocationUpdates() {
        //Toast.makeText(getApplicationContext(),"stopLocationUpdates", Toast.LENGTH_LONG).show();
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    /**
     * Google api callback methods
     */
    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = "+ result.getErrorCode());
//        Toast.makeText(getApplicationContext(),"Connection failed: ConnectionResult.getErrorCode() = "+ result.getErrorCode(), Toast.LENGTH_LONG).show();
    }

    @Override
    public void onConnected(Bundle arg0) {
       //Toast.makeText(getApplicationContext(),"onConnection", Toast.LENGTH_LONG).show();
       startLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int arg0) {
        //mGoogleApiClient.connect();
    }

    @Override
    public void onLocationChanged(Location location) {
        // Assign the new location
        mLastLocation = location;
//        Toast.makeText(getApplicationContext(), "Location changed!", Toast.LENGTH_SHORT).show();

    }

    @SuppressLint("ValidFragment")
    public class EventNoReadyDialog extends DialogFragment{

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(R.string.event_dialog_title)
                    .setPositiveButton(R.string.event_dialog_yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogWait();

                        }
                    })
                    .setNegativeButton(R.string.event_dialog_no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    });

            return builder.create();

        }
    }

    private void dialogWait(){

        AsyncTask<Void,Void,Void> task = new AsyncTask<Void, Void, Void>() {

            @Override
            protected void onPreExecute() {
                pd.show();
            }

            @Override
            protected Void doInBackground(Void... voids) {
                try{
                    //do
                    Log.d(TAG,"w1");
                    Thread.sleep(3000);
                    Log.d(TAG,"w2");
                }catch (InterruptedException e){
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                if(pd != null){
                    pd.dismiss();
                }
                Log.d(TAG,"w3");
                SendEvent();
            }
        };
        task.execute((Void[])null);
    }

    private void synchronizedWithServer() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if(cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected()) {

            DbAdapter db = new DbAdapter(context);
            Cursor c = db.getEventWithStatus(0);

            List<Event> events = db.cursorToEvents(c);
            db.close();
            //jeśli nie ma niewysłanych eventów to konczymy
//            if (events.isEmpty()) {
//                Log.d(TAG, "NIE MA EVENTOW DO WYSLANIA");
//                return;
//            } else {
//                Log.d(TAG, String.valueOf(events.size()));
//            }
            //jeśli są to wysyłamy eventy ze statusem 0
            Gson g = new Gson();
            Type type = new TypeToken<List<Event>>() {}.getType();
            final String eventsString = g.toJson(events, type);

            HashMap<String, Object> eventsJSONObject = new HashMap<String, Object>();
            SessionManager session = new SessionManager(context);

            eventsJSONObject.put(Const.LOGIN_API_KEY, session.getLogin());
            eventsJSONObject.put(Const.PASSWORD_API_KEY, session.getPassword());
            eventsJSONObject.put(Const.EVENTS_API_KEY, eventsString);

            AQuery aq = new AQuery(context);
            String url = Const.ADD_EVENTS_URL;

            ProgressDialog dialog = new ProgressDialog(this,ProgressDialog.THEME_HOLO_DARK);
            dialog.setIndeterminate(true);
            dialog.setCancelable(true);
            dialog.setInverseBackgroundForced(false);
            dialog.setCanceledOnTouchOutside(true);
            dialog.setMessage(getString(R.string.synchronized_with_server));
            Log.d("DATABASE", "przed synch");

            aq.progress(dialog).ajax(url, eventsJSONObject, JSONObject.class, new AjaxCallback<JSONObject>() {
                @Override
                public void callback(String url, JSONObject json, AjaxStatus status) {
                    //super.callback(url, object, status);
                    String message = "";
                    ArrayList<Event> eventsServer = new ArrayList<Event>();

                    Log.d("DATABASE", "synch");
                    if (json != null) {
                        Log.d("DATABASE", "!=null");

                        if (json.optBoolean("success") == true) {
                            Gson gson = new Gson();
                            try {
                                //message = "suc" + status.getCode() + json.toString() + json.optString("message");

                                JsonParser parser = new JsonParser();
                                if (parser.parse(json.getString("events")).isJsonArray()) {
                                    JsonArray jsonArrayEvents = parser.parse(json.getString("events")).getAsJsonArray();
                                    Log.d("DATABASE", "is array");

                                    for (int i = 0; i < jsonArrayEvents.size(); i++) {
                                        Event eve = gson.fromJson(jsonArrayEvents.get(i).getAsString(), Event.class);
                                        Log.d("DATABASE", eve.getDatetime());
                                        eventsServer.add(eve);
                                    }

                                } else {
                                    Log.d("DATABASE", "is no array");
                                    Event eve = gson.fromJson(json.getString("events"), Event.class);
                                    if (eve instanceof Event) {
                                        eventsServer.add(eve);
                                    }
                                }

                                //wyczyscic baze i dodac eventy
                                if (!eventsServer.isEmpty()) {
                                    Log.d("DATABASE", "NO empty");
                                    DbAdapter db2 = new DbAdapter(context);
                                    db2.deleteTable();
                                    Log.d("DATABASE",String.valueOf(eventsServer.size()));
                                    for (int i = 0; i < eventsServer.size(); i++) {
//                                        Gson g = new Gson();
//                                        Type type = new TypeToken<List<Event>>() {}.getType();
//                                        String eventsString = g.toJson(eventsServer.get(i), type);
//                                        Log.d("DATABASE",eventsString);
                                        Log.d("DATABASE",String.valueOf(i));
                                        db2.insertEvent(eventsServer.get(i));
                                    }
                                    db2.close();

                                }
                                //Log.d("DATABASE", json.getString("events"));
                            } catch (JSONException e1) {
                                e1.printStackTrace();
                                message = "Blad w przetwarzaniu JSON";
                                //TODO co z tymi informacjami ERORRAMI zrobic???
                            }
                        } else {
                            //success fail (cos z serwerem)
                            //Log.i(TAG, "Success-false");
                            message = "Success-false - SerwerRCP: " + status.getCode() + ", " + status.getError() + ", " + json.toString() + json.optString("message");
                            //error = "SerwerRCP: " + status.getCode() + ", " + status.getError() + ", " + status.getMessage();
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

                    Log.i(TAG, "error: " + message);
                    Log.d("DATABASE", "po synch");
                    DbAdapter db3 = new DbAdapter(context);
                    int type = db3.getLastEventType();
                    db3.close();

                    lastEvenTypeId = type;
                    setButtons();
                    viewLastEvents();
                }
            });


        }else{
            Toast.makeText(this,getString(R.string.synchronized_off), Toast.LENGTH_LONG).show();
        }
    }
}
