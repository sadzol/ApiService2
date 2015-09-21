package pl.rcponline.apiservice;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class EventsAdapter2 extends ArrayAdapter<Event>{//} implements Filterable{

    private final String TAG = "EventAdapter2";
    private final Object mLock = new Object();
    private final Context context;
    //private ItemsFilter mFilter;
    public List<Event> events;

    public EventsAdapter2(Context context, List<Event> m_events) {
        super(context, 0, m_events);
        this.events = m_events;
        this.context=context;
    }

    @Override
    public int getCount() {
        return events.size();
    }

    @Override
    public Event getItem(int position) {
        return events.get(position);
    }

    @Override
    public int getPosition(Event item) {
        return events.indexOf(item);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }
    @Override
    public View getView(int position, View view, ViewGroup parent) {

        if(view == null){
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.list_events,null);
        }

        Event e = events.get(position);
        if(e != null){
            TextView EventTime = (TextView) view.findViewById(R.id.tv_list_events_time);
            TextView EventName = (TextView) view.findViewById(R.id.tv_list_events_event);
            ImageView EventIcon     = (ImageView)     view.findViewById(R.id.iv_list_events_icon);
            TextView EventDesc = (TextView) view.findViewById(R.id.tv_list_events_desc);

            if(EventTime != null) {
                //EventTime.setText(String.valueOf(e.getDatetime()));
                EventTime.setText(getFormat(e.getDatetime()));
            }
            if(EventName != null) {
                //Tłumacznie ze stringa na resurce value
                //  Log.d(TAG,e.getTypeName());
//                int layoutID = getResources().getIdentifier("layout"+n, "id", getPackageName());
//            return (LinearLayout)findViewById(layoutID);

//                int eventIdName = getContext().getResources().getIdentifier("R.string."+e.getTypeName(),"string",getContext().getPackageName());
//            int eventIdName;
//            EventName.setText(getContext().getString(eventIdName));

//                Log.d(TAG,getContext().getString(Integer.parseInt(eventName)) );
                //Log.d(TAG, getContext().getString(getContext().getResources().getIdentifier(String.valueOf(e.getTypeName()), "string", Const.PACKAGE)));
                EventName.setText(getContext().getString(getContext().getResources().getIdentifier(String.valueOf(e.getTypeName()), "string", getContext().getPackageName())));
            }
            if(EventDesc != null){
                EventDesc.setText(e.getComment());
            }
            if(EventIcon != null){

                switch (e.getType()){
                    case 1:
                        EventIcon.setImageResource(R.drawable.info_play);
                        break;
                    case 2:
                        EventIcon.setImageResource(R.drawable.info_pause);
                        break;
                    case 3:
                        EventIcon.setImageResource(R.drawable.info_pause_green);
                        break;
                    case 4:
                        EventIcon.setImageResource(R.drawable.info_record);
                        break;
                    case 5:
                        EventIcon.setImageResource(R.drawable.info_record_green);
                        break;
                    case 6:
                        EventIcon.setImageResource(R.drawable.info_stop);
                        break;
                    default:
                        break;
                }

            }
        }

        return view;

    }
    private String getFormat(String dateEvent) {
        SimpleDateFormat _format = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        String dataEventFormat="";
        try {
            Date data = _format.parse(dateEvent);
            SimpleDateFormat _formatView = new SimpleDateFormat("dd-MM-yyyy HH:mm");
            dataEventFormat = _formatView.format(data);
            //dataEventFormat = data.toString();

        } catch (ParseException e) {
            e.printStackTrace();
        }

        return dataEventFormat;
    }

    //  FILTR http://www.mokasocial.com/2010/07/arrayadapte-filtering-and-you/

}
