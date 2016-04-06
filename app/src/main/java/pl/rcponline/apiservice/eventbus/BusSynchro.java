package pl.rcponline.apiservice.eventbus;

/**
 * Created by Sas on 2016-03-21.
 */
public class BusSynchro {
    public final String type;
    public final String message;

    public BusSynchro(String type){
        this.type = type;
        this.message = null;
    }

    public BusSynchro(String type, String message){
        this.type = type;
        this.message = message;
    }


}
