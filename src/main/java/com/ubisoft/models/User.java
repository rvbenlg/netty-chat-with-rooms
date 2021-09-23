package com.ubisoft.models;

import io.netty.channel.Channel;

import java.util.ArrayList;
import java.util.List;

public class User {

    private String username;
    private Channel channel;
    private String chatRoom;
    private List<Message> messages;

    public User(String username, Channel channel) {
        this.username = username;
        this.channel = channel;
        this.messages = new ArrayList<>();
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public String getChatRoom() {
        return chatRoom;
    }

    public void setChatRoom(String chatRoom) {
        this.chatRoom = chatRoom;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }
}
