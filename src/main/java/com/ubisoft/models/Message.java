package com.ubisoft.models;

import java.time.LocalDateTime;

public class Message {

    private String msg;
    private String sentBy;
    private LocalDateTime sentOn;

    public Message(String sentBy, String msg) {
        this.sentBy = sentBy;
        this.msg = msg;
        this.sentOn = LocalDateTime.now();
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public LocalDateTime getSentOn() {
        return sentOn;
    }

    public void setSentOn(LocalDateTime sentOn) {
        this.sentOn = sentOn;
    }

    public String getSentBy() {
        return sentBy;
    }

    public void setSentBy(String sentBy) {
        this.sentBy = sentBy;
    }
}
