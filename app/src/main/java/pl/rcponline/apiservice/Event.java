package pl.rcponline.apiservice;

public class Event {

    private int id, type, source, status;
    private String location, gps, comment, datetime, error;

    public Event(){}
//
//    public Event(int typeId, int _sourceId, String _datetime, String _location, String _comment){
//
//        this.type = typeId;
//        source = _sourceId;
//        datetime = _datetime;
//        location = _location;
//        comment = _comment;
//    }

    public Event(int _id, int typeId, int _sourceId, String _datetime, String _location, String _gps, String _comment, int _status, String _error){

        id = _id;
        this.type = typeId;
        source = _sourceId;
        datetime = _datetime;
        location = _location;
        gps     = _gps;
        comment = _comment;
        status = _status;
        error = _error;
    }


    public int getId() {
        return id;
    }

    public int getType() {
        return type;
    }
    public String getTypeName(){
        int TypeIdArr = getType() - 1;
        return Const.EVENT_TYPE[TypeIdArr];
    }

    public int getSource() {
        return source;
    }

    public int getStatus() {
        return status;
    }
    public void setStatus(int status) {
        this.status = status;
    }
    public String getDatetime() {
        return datetime;
    }

    public String getLocation() {
        return location;
    }

    public String getGPS(){
        return  gps;
    }

    public String getComment() {
        return comment;
    }

    public String getError() {
        return error;
    }
}
