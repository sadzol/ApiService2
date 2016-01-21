package pl.rcponline.apiservice;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import java.util.HashMap;

public class SessionManager {

    //private static final String TAG = "SESSION_MANAGER";
    private static final String PREF_NAME = "RCP_PREF_SESSION";
    //SharedPreference Mode
    private int PREF_MODE = 0;

    private SharedPreferences pref;
    private SharedPreferences.Editor editor;
    private Context context;

    public SessionManager(Context context){
        this.context = context;
        pref = context.getSharedPreferences(PREF_NAME, PREF_MODE);
        editor = pref.edit();
    }

    public void createSession(String login ,String pass){

        editor.putString(Const.PREF_LOGIN, login);
        editor.putString(Const.PREF_PASS, pass);
        editor.putBoolean(Const.PREF_IS_USER_LOGGED, true);

        editor.apply();
    }

    public String getLogin(){
        return pref.getString(Const.PREF_LOGIN,"");
    }
    public String getPassword(){
        return pref.getString(Const.PREF_PASS,"");
    }

    public void setLastEventTypeId(int typeId){
        editor.putInt(Const.PREF_EMPLOYEE_LAST_EVENT_TYPE_ID, typeId);
        editor.apply();
    }
    public Integer getLastEventTypeId(){
        return pref.getInt(Const.PREF_EMPLOYEE_LAST_EVENT_TYPE_ID, 6);
    }

    public void addMessage(String message){
        editor.putString(Const.PREF_MESSAGE_AFTER_EVENT, message);
        editor.apply();
    }

    //sprawdza czy zalogowany jesli nie przenosci do Login Activity
    public boolean checkLogin(){
        if(!isUserLoggedIn()){

            goToLoginActivity();
            return true;
        }
        return false;

    }

    public void logout(){

        editor.putString(Const.PREF_LOGIN, "");
        editor.putString(Const.PREF_PASS, "");
        editor.putBoolean(Const.PREF_IS_USER_LOGGED, false);

        editor.apply();
        //commit jest przestarzale (synchorniczne ) blokuje caly watek dopoki nie zapisze
        //editor.commit();

        goToLoginActivity();

    }

    public boolean isUserLoggedIn(){
        return pref.getBoolean(Const.PREF_IS_USER_LOGGED, false);
    }

    private void goToLoginActivity(){
        //Po wylogowaniu przekierowanie na LoginActivity
        Intent intent = new Intent(context, LoginActivity.class);

        //Zamykanie wszystkich innych aktywnosci z aplikacji skoro nastąpiło wylogowanie
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        //Dodaj nowa Flage na poczatek nowej Activity
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        //Start Login Activity
        context.startActivity(intent);
    }

    //Zmagazynowane dane
    public HashMap<String, String> getData(){

        HashMap<String, String> data = new HashMap<String, String>();
        data.put(Const.PREF_LOGIN, pref.getString(Const.PREF_LOGIN, null));
        data.put(Const.PREF_PASS, pref.getString(Const.PREF_PASS, null));
        data.put(Const.PREF_MESSAGE_AFTER_EVENT, pref.getString(Const.PREF_MESSAGE_AFTER_EVENT, null));

        return data;
    }
}
