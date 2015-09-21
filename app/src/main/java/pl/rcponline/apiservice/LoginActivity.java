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
    private final String TAG = "LOGIN";
    AQuery aq;
    SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        session = new SessionManager(getApplicationContext());

        aq = new AQuery(this);
        etLogin     = (EditText) findViewById(R.id.et_log);
        etPassword  = (EditText) findViewById(R.id.et_password);
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.login, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    public void signUp(View view) {
        Log.d(TAG, "In");
        final String login = etLogin.getText().toString().trim();
        final String password = etPassword.getText().toString().trim();
        String url = Const.LOGIN_URL;

        Map<String,Object> params = new HashMap<String,Object>();
        params.put(Const.LOGIN_API_KEY,login);
        params.put(Const.PASSWORD_API_KEY,password);

        ProgressDialog dialog = new ProgressDialog(this,ProgressDialog.THEME_HOLO_DARK);
        dialog.setIndeterminate(true);
        dialog.setCancelable(true);
        dialog.setInverseBackgroundForced(false);
        dialog.setCanceledOnTouchOutside(true);
        dialog.setMessage(getString(R.string.login));

        aq.progress(dialog).ajax(url,params, JSONObject.class,new AjaxCallback<JSONObject>(){

            @Override
            public void callback(String url, JSONObject json, AjaxStatus status) {
                    if(json != null){
                        Log.d(TAG,"JSON NO NULL");
                        if(json.optBoolean("success") == true){
                            Log.d(TAG,"success");

                            session.createSession(login, password);
                            DAO.saveLastEventsFromServer(json,getApplicationContext());
                            /*SharedPreferences sh = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                            SharedPreferences.Editor editor = sh.edit();
                            editor.putString(Const.PREF_LOGIN, login );
                            editor.putString(Const.PREF_PASS, password);
                            //editor.putString(Const.IS_REQUIRED_LOCATION);
                            editor.commit();

                            setResult(RESULT_OK);*/
                            Intent intent = new Intent(LoginActivity.this,MainActivity.class);
                            startActivity(intent);
                            finish();

                        }else {
                            Log.d(TAG,"fail");
                            Toast.makeText(getApplicationContext(), getString(R.string.bad_login_or_pass), Toast.LENGTH_LONG).show();
                        }

                        //Toast.makeText(getApplicationContext(), status.getCode() + ":" + json.optString("user"), Toast.LENGTH_LONG).show();
                    }else{
                        Log.d(TAG,"JSON IS NULL");
                        String error;
//                        Toast.makeText(getApplicationContext(),String.valueOf(status.getCode())).show();
                        if(status.getCode() == -101 ){
                            error = getString(R.string.error_offline);
                        }else{
                            error = getString(R.string.error_unexpected);
                        }
                        Toast.makeText(getApplicationContext(),error,Toast.LENGTH_LONG).show();
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
