package com.an.downloadapp;

public class EventBusEvent {

    private String mMsg;
    private Object obj;
    public EventBusEvent(String msg) {
        mMsg = msg;
    }

    public EventBusEvent(String msg, Object obj) {
        mMsg = msg;
        this.obj = obj;
    }

    public String getMsg(){
        return mMsg;
    }

    public Object getObj() {
        return obj;
    }

    public void setObj(Object obj) {
        this.obj = obj;
    }

}
