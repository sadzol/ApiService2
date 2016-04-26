package pl.rcponline.apiservice;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
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
import com.squareup.leakcanary.LeakCanary;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import pl.rcponline.apiservice.eventbus.BusSendingData;
import pl.rcponline.apiservice.eventbus.BusSynchro;

public class MainActivity extends Activity implements View.OnClickListener, ConnectionCallbacks, OnConnectionFailedListener, LocationListener {

    private static final String TAG = "MAIN_ACTIVITY";
    private boolean oneEventTypeAtTime = true;
    private int previousLastEvent = 0;
    // variable to track event time
    private long mLastClickTime = 0;

    String login, password, location, comment, data;
    int isEventSend, typeId, lastEvenTypeId;
    View lasViewEvent;
    SessionManager session;
    ProgressDialog pd;
//    Context context;

    ImageButton btStart, btFinish, btBreakStart, btBreakFinish, btTempStart, btTempFinish;

    ImageView imSynchro, ivStartOff, ivFinishOff, ivBreakOff, ivTempOff;
    LinearLayout llDatatime, llLastEvent, llEventButtonNew;
    RelativeLayout rlBreak, rlPayExit;
    EditText etComment;
    TextView tvAddress;
    ProgressBar progressBar;

    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 1000;
    private final static int REQUEST_CHECK_SETTINGS = 1001;
    private Location mLastLocation;

    // Google client to interact with Google API
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private Location currentBestLocation = null;

    // Location updates intervals in sec
    private static int UPDATE_INTERVAL = 1000; // 1000 = 1 sec
    private static int FATEST_INTERVAL = 1000; // 1000 = 1 sec
    private static int DISPLACEMENT = 1; // 1 meters
    private static int MY_PERMISSION_REQUEST_LOCATION = 0;
    ////////////////////////////////////////////////////////////////////////////////

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Tool to detect error(especially leaks)
        if (Const.DEVELOPER_MODE) {
            LeakCanary.install(getApplication());

            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()   // or .detectAll() for all detectable problems
                    .penaltyLog()
                    .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
//                    .penaltyDeath()
                    .build());
        }

        //Ustawiamy ustawienia domyślnymi warościami z pliku prefernecji (false-tylko raz)
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        //TODO TEZ TWORZYC TU WYSYWALNIE EVENTOW JESLI MAJA STATUS 0   SYNCHRO ....

        //Jesli user nie zalogowany przenies do strony logowania zamykajac ta aktywnosc
        session = new SessionManager(getApplicationContext());
        if (session.checkLogin()) {
            finish();
        } else {
            login = session.getLogin();
            password = session.getPassword();
        }

        //Wył. KLAWIATURE do czasu az pole tekstownie nie zostanie wybrane    (Disabled software keyboard in android until TextEdit is chosen)
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        //Loc -  First we need to check availability of play services
        if (checkPlayServices()) {
            Log.d(TAG, "checkPlayService - OK");
            buildGoogleApiClient();
            createLocationRequest();
        } else {
            Log.d(TAG, "checkPlayService - NO");
        }


        //Inicjajca przyciskow
        btStart = (ImageButton) findViewById(R.id.bt_start);
        btFinish = (ImageButton) findViewById(R.id.bt_finish);
        btBreakStart = (ImageButton) findViewById(R.id.bt_break_start);
        btBreakFinish = (ImageButton) findViewById(R.id.bt_break_finish);
        btTempStart = (ImageButton) findViewById(R.id.bt_temp_start);
        btTempFinish = (ImageButton) findViewById(R.id.bt_temp_finish);

        ivStartOff = (ImageView) findViewById(R.id.iv_start_off);
        ivFinishOff = (ImageView) findViewById(R.id.iv_finish_off);
        ivBreakOff = (ImageView) findViewById(R.id.iv_break_off);
        ivTempOff = (ImageView) findViewById(R.id.iv_temp_off);
        llDatatime = (LinearLayout) findViewById(R.id.ll_datatime);
        imSynchro = (ImageView) findViewById(R.id.im_synchronized);

        //Potrzebne dla ustawien widocznosci
        etComment = (EditText) findViewById(R.id.et_event_comment);
        llLastEvent = (LinearLayout) findViewById(R.id.ll_last_events);
        rlBreak = (RelativeLayout) findViewById(R.id.ll_pause);
        rlPayExit = (RelativeLayout) findViewById(R.id.ll_record);
        llEventButtonNew = (LinearLayout) findViewById(R.id.event_buttons_new);

        tvAddress = (TextView) findViewById(R.id.tv_address);

        btStart.setOnClickListener(this);
        btFinish.setOnClickListener(this);
        btBreakStart.setOnClickListener(this);
        btBreakFinish.setOnClickListener(this);
        btTempStart.setOnClickListener(this);
        btTempFinish.setOnClickListener(this);
        imSynchro.setOnClickListener(this);

        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        lastEvenTypeId = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getInt(Const.LAST_EVENT_TYPE_ID, 6);

        //progres bar w przypadku zwiekszenia dokladnosci lokalizacji zeby miec czas na wyszukanie polozenia i zlapanie sieci wi-fi
        pd = new ProgressDialog(new ContextThemeWrapper(this, android.R.style.Theme_Holo_Dialog));
        pd.setCancelable(false);
        pd.setIndeterminate(true);
        pd.setTitle("");
        pd.setMessage(getString(R.string.searching));
    }

    @Override
    protected void onStart() {
        EventBus.getDefault().register(this);

        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
        super.onStart();
    }

    @Override
    //onResume po odwroceniu urzadzenia
    protected void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();

        session.setIsButtonLock(false);
        oneEventTypeAtTime = true;
        previousLastEvent = 0;

        // Resuming the periodic location updates
        if (mGoogleApiClient.isConnected()) {
            startLocationUpdates();
        } else {
            //Toast.makeText(this,"NOstartLocationUpdates",Toast.LENGTH_SHORT).show();
        }

        setButtons();
        viewLastEvents();

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        if (sp.getBoolean("location", false)) {
            tvAddress.setVisibility(View.VISIBLE);
        } else {
            tvAddress.setVisibility(View.GONE);
        }
        if (sp.getBoolean("comment", false)) {
            etComment.setVisibility(View.VISIBLE);
        } else {
            etComment.setVisibility(View.GONE);
        }
        if (sp.getBoolean("last_events", false)) {
            llLastEvent.setVisibility(View.VISIBLE);
        } else {
            llLastEvent.setVisibility(View.GONE);
        }
        if (sp.getBoolean("break", false)) {
            rlBreak.setVisibility(View.VISIBLE);
        } else {
            rlBreak.setVisibility(View.GONE);
        }
        if (sp.getBoolean("pay_exit", false)) {
            rlPayExit.setVisibility(View.VISIBLE);
        } else {
            rlPayExit.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();
    }

    // TESTOWAC DISCONETC LOCATION ON PAUSE http://blog.teamtreehouse.com/beginners-guide-location-android
    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");
        if (mGoogleApiClient.isConnected()) {
            stopLocationUpdates();
            mGoogleApiClient.disconnect();
        }
        Intent synchroIntent = new Intent(getApplicationContext(), SynchroService.class);
        synchroIntent.putExtra("from", "synchro");
        synchroIntent.putExtra("synchroType", "auto");
        synchroIntent.putExtra(Const.LOGIN_API_KEY, session.getLogin());
        synchroIntent.putExtra(Const.PASSWORD_API_KEY, session.getPassword());
        startService(synchroIntent);

        EventBus.getDefault().unregister(this);

        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        //Przechwytuje opcje z górnego menu po prawej
        switch (item.getItemId()) {
            case R.id.menu_settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;

            case R.id.menu_info:
                FragmentManager manager = getFragmentManager();
                InfoFragment dcf = new InfoFragment();
                dcf.show(manager, "info");
                return true;

            case R.id.menu_logout:
                session.logout();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    //sprawdza czy masz w ustawianich wymuszona najlepsza lokalizacje
    boolean isBestLocationRequired() {
        SharedPreferences sh = PreferenceManager.getDefaultSharedPreferences(this);
        return sh.getBoolean("location", false);
    }

    @Override
    public void onClick(View v) {
        // Preventing multiple clicks, using threshold of 1 second
        if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
            return;
        }
        mLastClickTime = SystemClock.elapsedRealtime();

        lasViewEvent = v;

        Log.d("Kol","1 - onClick");
        if (v.getId() == R.id.im_synchronized) {
            Log.d(TAG, "in synchro");
            Intent synchroIntent = new Intent(getApplicationContext(), SynchroService.class);
            synchroIntent.putExtra("from", "synchro");
            synchroIntent.putExtra("synchroType", "manual");
            synchroIntent.putExtra(Const.LOGIN_API_KEY, session.getLogin());
            synchroIntent.putExtra(Const.PASSWORD_API_KEY, session.getPassword());
            startService(synchroIntent);

//            synchronizedWithServer();

//        } else if (isBestLocationRequired()) {
//            viewDialogLocation();

        } else {
            if (isBestLocationRequired()) {
                Log.d("Kol","2 - isBestLocation?");
                viewDialogLocation();
            }else {

                Log.d("Kol","3  - startEvent");
                startEvent();
            }
        }

    }

    private void startEvent() {
        View v = lasViewEvent;
        switch (v.getId()) {
            case R.id.bt_start:
                lastEvenTypeId = 1;
                break;
            case R.id.bt_finish:
                lastEvenTypeId = 6;
                break;
            case R.id.bt_break_start:
                lastEvenTypeId = 2;
                break;
            case R.id.bt_break_finish:
                lastEvenTypeId = 3;
                break;
            case R.id.bt_temp_start:
                lastEvenTypeId = 4;
                break;
            case R.id.bt_temp_finish:
                lastEvenTypeId = 5;
                break;
            default:
                lastEvenTypeId = 0;
                break;
        }
        if(lastEvenTypeId == 0)
            return;

        Log.d(TAG, "EVENTQ " + lastEvenTypeId);

        if (SendEvent()) {
            Log.d("EVENTQ","sendEvent TRUE");
            //Dodaj ostani event do preferencji
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            SharedPreferences.Editor editor = sp.edit();
            editor.putInt(Const.LAST_EVENT_TYPE_ID, lastEvenTypeId);
            editor.commit();

            setButtons();
            viewLastEvents();

//            try {
//                Thread.sleep(2000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
        }else{
            Log.d("BLAD-EVENTQ","SendEvent=false, event=>"+String.valueOf(oneEventTypeAtTime+", eventId=>"+String.valueOf(lastEvenTypeId)));
        }

        oneEventTypeAtTime = true;
    }


    private boolean SendEvent() {
        Log.d("EVENTQ-SENDEVENT IN",String.valueOf(oneEventTypeAtTime));
        //przeciwdziala kliknieciu 2x szybko na ten sam przycisk i zdublowaniu eventa do bazy
//        if((previousLastEvent == lastEvenTypeId) || (oneEventTypeAtTime == false))
//        if (oneEventTypeAtTime == false)
//            return false;
//
////        previousLastEvent = lastEvenTypeId;
//////        Log.d("BLAD-EVENTQ","event=>"+String.valueOf(oneEventTypeAtTime)+", eventId=>"+String.valueOf(lastEvenTypeId));
//        oneEventTypeAtTime = false;

//        setButtons();

        //TODO SPRAWDZIC CZY JEST polaczenie jesli nie to nie uruchamiac aq.ajax
        typeId = lastEvenTypeId;
        String url = Const.ADD_EVENT_URL;
        data = getDateTime();
        comment = etComment.getText().toString();
        isEventSend = 0;

        location = getString(R.string.location_disabled);

        //jezeli ma uprawnienia do  lokalizacji
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if (mLastLocation != null) {
                location = mLastLocation.getLatitude() + ";" + mLastLocation.getLongitude();
            }

        }else{
            //TODO POPROSIC O UPRAWNIENIA
            Toast.makeText(getApplicationContext(),"Musisz nadać uprawnienia do lokalizacji",Toast.LENGTH_LONG).show();
            return  false;
//            Log.d(TAG,"OUTTTTTTTTTTTTTTTT");
        }
//
//        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
//        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

//        WifiManager wifi = (WifiManager)getSystemService(Context.WIFI_SERVICE);
//        if (wifi.isWifiEnabled()){
//
//        }
        //jesli jest ustawiona doklada lokalizacja a Internet i Lokalizacja nie zdazyly sie jeszcze uruchomic (wymaga to 1s,2s) to wyskauje okno ("grajace na czas")na wyczkanie.
        if (isBestLocationRequired()) {
//            Log.d(TAG,"first");
            if( mLastLocation == null){
//            if ((mLastLocation == null) || (!wifi.isWifiEnabled())){
                DialogFragment df = new EventNoReadyDialog();
                df.show(getFragmentManager(), "tag");
                return false;
            }
        }


//        if (networkInfo != null && networkInfo.isConnected()) {

        long lastAddedEventId = saveEventToLocalDatabase(typeId, data, location, comment, isEventSend, "");

        Intent intentService = new Intent(getApplicationContext(), SynchroService.class);
        intentService.putExtra("from","event")
                .putExtra(Const.LOGIN_API_KEY, session.getLogin())
                .putExtra(Const.PASSWORD_API_KEY, session.getPassword())
                .putExtra(Const.TYPE_ID_API_KEY, typeId)
                .putExtra(Const.SOURCE_ID_API_KEY, Const.SOURCE_ID)
                .putExtra(Const.DATATIME_API_KEY,data)
                .putExtra(Const.LOCATION_API_KEY,"")
                .putExtra(Const.GPS_API_KEY,location)
                .putExtra(Const.COMMENT_API_KEY, comment)
                .putExtra(Const.LAST_ADDED_EVENT_ID, lastAddedEventId);

        startService(intentService);

        int resourceType = getResources().getIdentifier(String.valueOf(Const.EVENT_TYPE[typeId - 1]), "string", getPackageName());
        String msg = getApplicationContext().getString(R.string.event_saved) + " " + getApplicationContext().getString(resourceType).toUpperCase();
        Toast.makeText(getApplicationContext(),msg,Toast.LENGTH_SHORT).show();

        return true;

    }


    private long saveEventToLocalDatabase(int typeId, String data, String gps, String comment, int isEventSend, String error) {
        Event event = new Event(0, typeId, Const.SOURCE_ID, data, "", gps, comment, isEventSend, error);
        DbAdapter db = new DbAdapter(getApplicationContext());
        long lastAddedEventId = db.insertEvent(event);
        db.close();

        return lastAddedEventId;
    }

    private String getDateTime() {
        SimpleDateFormat _format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String currentDataTimeStrong = _format.format(new Date());
        return currentDataTimeStrong;
    }

    /**
     * WYSWIETLANIE LISTY OSTATNICH EVENTOW
     */
    public void viewLastEvents() {

        //Pobieram ostatnie(6) eventy z bazy
        DbAdapter db = new DbAdapter(getApplicationContext());
        List<Event> lastEvents = db.cursorToEvents(db.getLastEvents());
        db.close();

        //Dodaje adapter do ListView
        ListView lv = (ListView) findViewById(R.id.lv_last_events);
        EventsAdapter adapter = new EventsAdapter(getApplicationContext(), lastEvents);
        adapter.notifyDataSetChanged(); //aktualizacja danych
        lv.setAdapter(adapter);

        //Wył. KLAWIATURE do czasu az pole tekstownie nie zostanie wybrane    (Disabled software keyboard in android until TextEdit is chosen)
        // hide virtual keyboard
        etComment.setText("");
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(etComment.getWindowToken(), 0);
        etComment.clearFocus();

        oneEventTypeAtTime = true;
    }

    /**
     * ZMIANA STANU PRZYCISKÓW
     */
    private void setButtons() {
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
        DbAdapter dbAdapter = new DbAdapter(getApplicationContext());
        Event event = dbAdapter.getLastEvent();
        dbAdapter.close();

        if (isCurrentButtonIsOk(event)) {
            session.setLastEventTypeId(event.getType());
        } else {
            session.setLastEventTypeId(6);
        }


        switch (session.getLastEventTypeId()) {
            //FINISH
            case 6:
                btStart.setVisibility(View.VISIBLE);
                btStart.setEnabled(true);

                ivBreakOff.setVisibility(View.VISIBLE);
                ivTempOff.setVisibility(View.VISIBLE);
                ivFinishOff.setVisibility(View.VISIBLE);
                llDatatime.setBackgroundResource(R.drawable.gradient_red);
                break;

            //BREAK START
            case 2:
                btBreakFinish.setVisibility(View.VISIBLE);
                btBreakFinish.setEnabled(true);

                ivStartOff.setVisibility(View.VISIBLE);
                ivFinishOff.setVisibility(View.VISIBLE);
                ivTempOff.setVisibility(View.VISIBLE);
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

                ivStartOff.setVisibility(View.VISIBLE);
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
                llDatatime.setBackgroundResource(R.drawable.gradient_green);
                break;
        }
    }



    @SuppressLint("ValidFragment")
    public class EventNoReadyDialog extends DialogFragment {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Log.d("EVENTQ","dialodfragment");
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

    private void dialogWait() {

        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {

            @Override
            protected void onPreExecute() {
                pd.setMessage(getString(R.string.searching));
                pd.show();
            }

            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    Log.d(TAG, "w1");
                    Thread.sleep(3000);
                    Log.d(TAG, "w2");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                if (pd != null) {
                    pd.dismiss();
                }
                Log.d(TAG, "w3");
//                SendEvent();
            }

        };
        task.execute((Void[]) null);
    }

    /**
     * Jesli pracownik zapimni odbic wyjscie...
     * Jesli mamy ustawiona prace w nocy  -> nie zmieniamy wyswietlania osatniego eventa
     * Jesli mamy ustawiona prace dniowa  -> zmianiamy dostepnosc przycisku na PLAY
     */
    private boolean isCurrentButtonIsOk(Event event) {

        //Czy jest wg. event pracownika czy event jest pusty(nie ma go)
        Log.d("BLAD", String.valueOf(event.getId()));
        if (event.getId() != 0) {

            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
            if (sp.getBoolean("night_work", true)) {//nie wiem czemu odwrotnie

                SimpleDateFormat _formatFrom = new SimpleDateFormat("yyyy-MM-dd");
                SimpleDateFormat _formatTo = new SimpleDateFormat("yyyyMMdd");
                String currentDate = _formatTo.format(new Date());

                try {
                    Date eventDate = _formatFrom.parse(event.getDatetime());
                    String eventDateString = _formatTo.format(eventDate);

                    if (eventDateString.compareTo(currentDate) < 0) {
//                        Log.d("BUTTON_", "event < now");
                        return false;
                    } else {
//                        Log.d("BUTTON_", "event > now");
                        return true;
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                    return true;
                }
            } else {
//                Log.d("BUTTON_", "NIGHT WORK TRUE- noc");
                //praca nocna eventy zostaja zawsze jak sa
                return true;
            }

        } else {
            Log.d("BLAD", "Nie ma eventa id=0");
            return false;
        }

    }

    //####################################################################
    // GEOKODOWANIE GPS => ADDRESS
    //####################################################################
    public static Address getAddressForLocation(Context context, Location location) {
        try {
//            Log.d("ADDRESS-lat,lng",String.valueOf(location.getLatitude())+" , "+String.valueOf(location.getLongitude()));
            Geocoder geocoder = new Geocoder(context);
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);

//            Log.d("ADDRESS",addresses.toString());

            if (addresses != null && addresses.size() > 0) {
                return addresses.get(0);
            } else {
                return null;
            }
        } catch (IOException e) {
            return null;
        }
    }

    public String getStreetNameForAddress(Address address) {
        String streetName = address.getAddressLine(1) + ", " + address.getAddressLine(0);
        if (streetName == null) {
            streetName = address.getThoroughfare();
        }
        return streetName;
    }

    //####################################################################
    // LOCATION
    //####################################################################

    /**
     * Creating google api client object
     */
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
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
     * Method to verify google play services on the device
     */
    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this, PLAY_SERVICES_RESOLUTION_REQUEST).show();
                Log.d(TAG, "checkPlayServices : ConnectionResult.FALSE: isUserRecoverableError :" + resultCode);
            } else {
                Log.d(TAG, "checkPlayServices: This device is not supported.");
                Toast.makeText(getApplicationContext(), "This device is not supported.", Toast.LENGTH_LONG).show();
                finish();
            }
            return false;

        } else {
            Log.d(TAG, "checkPlayServices: ConnectionResult.TRUE");
            return true;
        }
    }


    /**
     * Stopping location updates
     */
    protected void stopLocationUpdates() {
//        Toast.makeText(getApplicationContext(),"stopLocationUpdates", Toast.LENGTH_LONG).show();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);

        }else{
            //TODO POPROSIC O UPRAWNIENIA
            Toast.makeText(getApplicationContext(),"Musisz nadać uprawnienia do lokalizacji",Toast.LENGTH_LONG).show();
//            Log.d(TAG,"OUTTTTTTTTTTTTTTTT");
        }
    }

    /**
     * Google api callback methods
     */
    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.d(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
//        Toast.makeText(getApplicationContext(),"Connection failed: ConnectionResult.getErrorCode() = "+ result.getErrorCode(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnected(Bundle arg0) {
        startLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int arg0) {
        Log.d(TAG,"Conncetion suspended");
        mGoogleApiClient.connect();
    }

    @Override
    public void onLocationChanged(Location location) {
//        Toast.makeText(getApplicationContext(),"LocationUpdates", Toast.LENGTH_LONG).show();

        mLastLocation = location;
        Address address = getAddressForLocation(getApplicationContext(), location);

        if (address instanceof Address) {

            String street = getStreetNameForAddress(address);
            Log.d("MAIN_ACTIVITY_ADDRESS", street);
            if (street != null && !street.isEmpty() && !street.equals("null"))
                tvAddress.setText(street);

        } else {
            tvAddress.setText(location.getLatitude() + ":" + location.getLongitude());
        }
    }

    /**
     * Starting the location updates
     */
    protected void startLocationUpdates() {
//        Toast.makeText(getApplicationContext(),"startLocation", Toast.LENGTH_LONG).show();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);

        }else{
            //TODO poprosic o UPRAWNIENIA
            Toast.makeText(getApplicationContext(),"Musisz nadać uprawnienia do lokalizacji",Toast.LENGTH_LONG).show();
        }

    }

    /**
     * Okno dialogowy wyswietlajace przycisk do wl. lokalizacji
     */
    protected void viewDialogLocation() {
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
//                final LocationSettingsStates status = result.getLocationSettingsStates();
                String txt = "";
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        //ustawienia lokalizacji sa wystarczajace, mozna wyzwolic zdarzenie
                        Log.d(TAG, "EVENTQ - sett -succes");
                        //TODO EVENT BUS
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        //super.onActivityResult(requestCode, resultCode, data);
        final LocationSettingsStates states = LocationSettingsStates.fromIntent(data);
        String txt = "";
        String s = Integer.toString(resultCode);
        Log.d(TAG, "EVENTQ -requestCode" + String.valueOf(requestCode));
        Log.d(TAG, "EVENTQ -resultCode" + String.valueOf(resultCode));
        switch (requestCode) {
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:

                        dialogWait();
                        //uzytkownik zmienil ustawienia lokalizacji na wymagane
                          Log.d(TAG, "EVENTQ - sett -ok");

//                        startEvent();
                        break;
                    case Activity.RESULT_CANCELED:
                        //uzytkownik zapytany o wl. lokalizacji odpowiedzial nie
                        Log.d(TAG, "EVENTQ - sett -cancel");

                        //Toast.makeText(this,getString(R.string.setting_location_on),Toast.LENGTH_LONG).show();
                        break;
                }
                break;
            case PLAY_SERVICES_RESOLUTION_REQUEST:
                if (resultCode == RESULT_CANCELED) {
                    Toast.makeText(this, "Google Play Services must be installed.", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Services must be installed");
                    finish();
                }
                break;
        }

    }
    //####################################################################
    // Better Location
    //####################################################################

    /**
     * Determines whether one Location reading is better than the current Location fix
     *
     * @param location            The new Location that you want to evaluate
     * @param currentBestLocation The current Location fix, to which you want to compare the new one
     */
    protected boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }
        int TWO_MINUTES = 1000 * 60 * 2;
        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }

    /** Checks whether two providers are the same */
    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }

    //####################################################################
    // BUS EVENT
    //####################################################################

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onBusSendingData(BusSendingData event) {
        String sendingData = event.sendingData;
        Log.d(TAG + "_BUS", "in onBusSendingData");

        if (sendingData.equals("startSendingData")) {
            imSynchro.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);

        } else if (sendingData.equals("endSendingData")) {
            imSynchro.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);

            updateEventView();
        }

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onBusSynchro(BusSynchro event){
        String type = event.type;
        Log.d(TAG + "_BUS", "in onBusSynchro");

        if (type.equals("synchroManual")) {
            if (event.message != null) {
                Toast.makeText(getApplicationContext(), event.message, Toast.LENGTH_SHORT).show();
            }
        }

        if (type.equals("event")) {
            Log.d(TAG + "_BUS", "in onBusSynchro event" + event.message);
            if (event.message != null) {
                Toast.makeText(getApplicationContext(), event.message, Toast.LENGTH_SHORT).show();
            }
        }

    }

    private void updateEventView(){
        setButtons();
        viewLastEvents();
    }
}
