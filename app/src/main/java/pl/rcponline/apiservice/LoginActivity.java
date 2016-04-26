package pl.rcponline.apiservice;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import pl.rcponline.apiservice.dao.DAO;

public class LoginActivity extends Activity {

    EditText etLogin,etPassword;
    private final String TAG = "LOGIN_ACTIVITY";
    SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        session = new SessionManager(getApplicationContext());

        etLogin     = (EditText) findViewById(R.id.et_log);
        etPassword  = (EditText) findViewById(R.id.et_password);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.login, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    public void signUp(View view) {
        final String login = etLogin.getText().toString().trim().replace(" ","");
        final String password = etPassword.getText().toString().trim().replace(" ","");
        String url = Const.LOGIN_URL;

        Map<String,Object> params = new HashMap<String,Object>();
        params.put(Const.LOGIN_API_KEY,login);
        params.put(Const.PASSWORD_API_KEY,password);
        Log.d(TAG, "Send Login: "+login+":"+password);

        ProgressDialog dialog = new ProgressDialog(this,ProgressDialog.THEME_HOLO_DARK);
        dialog.setIndeterminate(true);
        dialog.setCancelable(true);
        dialog.setInverseBackgroundForced(false);
        dialog.setCanceledOnTouchOutside(true);
        dialog.setMessage(getString(R.string.login));

        AQuery aq = new AQuery(getApplicationContext());
        aq.progress(dialog).ajax(url,params, JSONObject.class,new AjaxCallback<JSONObject>(){

            @Override
            public void callback(String url, JSONObject json, AjaxStatus status) {
                    if(json != null){
                        Log.d(TAG,"JSON NO NULL");
                        if(json.optBoolean("success") == true){

                            Log.d(TAG, "Success Login: "+login+":"+password);
                            SessionManager session = new SessionManager(getApplicationContext());
                            session.createSession(login, password);

                            DAO.saveLastEventsFromServer(json,getApplicationContext());

                            Intent intent = new Intent(LoginActivity.this,MainActivity.class);
                            startActivity(intent);
                            finish();

                        }else {
                            Log.d(TAG,"fail");
                            Toast.makeText(getApplicationContext(), getString(R.string.bad_login_or_pass), Toast.LENGTH_LONG).show();
                        }

                    }else{
                        Log.d(TAG,"JSON IS NULL");
                        String error;
                        //Kiedy kod 500( Internal Server Error)
                        if (status.getCode() == 500) {
                            error = getString(R.string.error_500);

                            //Błąd 404 (Not found)
                        } else if (status.getCode() == 404) {
                            error = getString(R.string.error_404);

                            //Blad -101 Moze oznaczac: Serwer nie odpowiada
                        } else if(status.getCode() == -101 ){
                            error = getString(R.string.error_offline);

                            //500 lub 404
                        }else{
                            error = getString(R.string.error_unexpected);
                        }
                        Log.d(TAG, error);

                        Toast.makeText(getApplicationContext(), error, Toast.LENGTH_LONG).show();

                    }

                DbAdapter db3 = new DbAdapter(getApplicationContext());
                int type = db3.getLastEventType();
                db3.close();

                //Dodaj ostani event do preferencji
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                SharedPreferences.Editor editor = sp.edit();
                editor.putInt(Const.LAST_EVENT_TYPE_ID, type);
                editor.commit();
            }
        });
    }
}
