package pl.rcponline.apiservice.utils;

import android.util.Log;

/**
 * Created by Sas on 2016-02-02.
 */
public class UtilsThread {

    public static long getThreadId(){
        Thread t = Thread.currentThread();
        return t.getId();
    }

    public static String getThreadSignature(){
        Thread t = Thread.currentThread();
        long l = t.getId();
        String name = t.getName();
        long p = t.getPriority();
        String gname = t.getThreadGroup().getName();
        return (name
                + ":(id)" + l
                + ":(priorytet)" + p
                + ":(grupa)" + gname);

    }

    public static void logThreadSignature(String tag){
        Log.d(tag, getThreadSignature());
    }

    public static void sleepForInSecs(int secs){
        try{
            Thread.sleep(secs * 1000);
        }catch (InterruptedException x){
            throw new RuntimeException("przerwano",x);
        }
    }
}
