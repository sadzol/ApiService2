package pl.rcponline.apiservice;

public class Const {

    public static final String ENIVORMENT   = "";
//  public static final String ENIVORMENT   = "/app_dev.php";
  public static final String MAIN_URL     = "http://panel.rcponline.pl";//
//  public static final String MAIN_URL     = "http://dev-panel.rcponline.pl";//
//    public static final String MAIN_URL     = "http://192.168.2.102";//rcp.lh  home
//  public static final String MAIN_URL   = "http://192.168.8.114"; //biuro
//  public static final String MAIN_URL   = "http://rcp.lh"; //mietka
    public static final String LOGIN_URL    = MAIN_URL+ENIVORMENT+"/api/loginApp";
    public static final String ADD_EVENT_URL  = MAIN_URL+ENIVORMENT+"/api/addEvent";
    public static final String ADD_EVENTS_URL = MAIN_URL+ENIVORMENT+"/api/addEvents";
    public static final String URL_TEST     = MAIN_URL+ENIVORMENT+"api/sT";
    public static final String PACKAGE      = "pl.rcponline.apiservice";

    public static final String LOGIN_API_KEY    = "login";
    public static final String PASSWORD_API_KEY = "password";
    public static final String TYPE_ID_API_KEY  = "type_id";
    public static final String SOURCE_ID_API_KEY = "source_id";
    public static final String DATATIME_API_KEY = "datetime";
    public static final String LOCATION_API_KEY = "location";
    public static final String GPS_API_KEY      = "gps";
    public static final String COMMENT_API_KEY  = "comment";
    public static final String EVENTS_API_KEY   = "events";

    public static final Integer SOURCE_ID   = 6;//application-smartphone
    public static final String PREF_LOGIN   = "pl.rcponline.login";
    public static final String PREF_PASS    = "pl.rcponline.password";
    public static final String PREF_IS_USER_LOGGED = "pl.rcponline.is_user_logged";
    public static final String PREF_MESSAGE_AFTER_EVENT = "pl.rcponline.message_after_event";
    
    public static final String IS_REQUIRED_LOCATION = "pl.rcponline.is_required_location";
    public static final String LAST_EVENT_TYPE_ID   = "pl.rcponline.last_event_type_id";
    public static final String PREF_EMPLOYEE_LAST_EVENT_TYPE_ID = "pl.rcponline.employee_last_event_type_id";

    public static final Integer TIME_INTERVAL = 1000 * 60 * 2;

    public static final String[] EVENT_TYPE = {"work_start","break_start","break_finish","temp_out_start","temp_out_finish","work_finish"};


}
