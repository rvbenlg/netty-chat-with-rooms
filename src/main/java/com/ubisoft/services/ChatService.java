package com.ubisoft.services;

import com.ubisoft.constants.Values;
import com.ubisoft.models.Message;
import com.ubisoft.models.User;
import io.netty.channel.ChannelHandlerContext;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class ChatService {

    private static final HashMap<String, List<User>> chatRooms = new HashMap<>();

    static {
        initializeChatRooms();
    }

    public synchronized void addUser(ChannelHandlerContext ctx) {
        User user = new User(generateUsername(), ctx.channel());
        ctx.channel().writeAndFlush(String.format(Values.HELLO_USER, user.getUsername()));
        chatRooms.get(Values.CHAT_ROOMS[0].toUpperCase()).add(user);
    }

    public void processMessage(ChannelHandlerContext context, String msg) {
        if (isCommand(msg)) {
            processCommand(context, msg);
        } else if(getRoomFromContext(context).equalsIgnoreCase(Values.CHAT_ROOMS[0])){
            notInARoom(context);
        } else {
            sendMessage(context, msg);
        }
    }

    private void processCommand(ChannelHandlerContext context, String msg) {
        if (msg.startsWith(Values.COMMAND_NICK)) {
            changeUsername(context, msg);
        } else if (msg.equals(Values.COMMAND_LIST)) {
            printRoomNames(context);
        } else if (msg.startsWith(Values.COMMAND_JOIN)) {
            joinRoom(context, msg);
        } else if (msg.equals(Values.COMMAND_EXIT)) {
            disconnect(context);
        } else {
            notRecognizedCommand(context, msg);
        }
    }

    private void changeUsername(ChannelHandlerContext context, String msg) {
        String newUsername = msg.replaceAll(Values.COMMAND_NICK, "").trim();
        User sender = getUserFromContext(context);
        if (alreadyInUse(newUsername)) {
            sender.getChannel().writeAndFlush(String.format(Values.USERNAME_ALREADY_BEING_USED, newUsername));
        } else {
            sender.setUsername(newUsername);
            sender.getChannel().writeAndFlush(String.format(Values.HELLO_USER, sender.getUsername()));
        }
    }

    private void printRoomNames(ChannelHandlerContext context) {
        User sender = getUserFromContext(context);
        StringBuilder toPrint = new StringBuilder();
        for (int i = 1; i < Values.CHAT_ROOMS.length; i++) {
            String room = Values.CHAT_ROOMS[i];
            toPrint.append(room).append("\n");
        }
        sender.getChannel().writeAndFlush(toPrint);
    }

    private void joinRoom(ChannelHandlerContext context, String msg) {
        String newRoom = msg.replaceAll(Values.COMMAND_JOIN, "").toUpperCase();
        String oldRoom = getRoomFromContext(context);
        User sender = getUserFromContext(context);
        if (oldRoom.equalsIgnoreCase(newRoom)) {
            sender.getChannel().writeAndFlush(Values.USER_ALREADY_IN_ROOM);
        } else if (chatRooms.containsKey(newRoom)) {
            chatRooms.get(oldRoom).remove(sender);
            chatRooms.get(newRoom).add(sender);
            welcomeNewUser(newRoom, sender);
        } else {
            sender.getChannel().writeAndFlush(Values.ROOM_NOT_EXIST);
        }
    }

    private void welcomeNewUser(String room, User newUser) {
        List<User> roomUsers = chatRooms.get(room.toUpperCase());
        String roomName = Arrays.asList(Values.CHAT_ROOMS).stream().filter(s -> s.equalsIgnoreCase(room)).findFirst().get();
        newUser.getChannel().writeAndFlush(String.format(Values.WELCOME_USER, roomName, roomUsers.size() - 1));
        showPreviousMessages(roomUsers, newUser);
        roomUsers.stream()
                .filter(user -> !user.getChannel().equals(newUser.getChannel()))
                .forEach(user -> user.getChannel().writeAndFlush(String.format(Values.USER_JOINED, newUser.getUsername())));
    }

    private void showPreviousMessages(List<User> roomUsers, User newUser) {
        List<Message> messages = roomUsers.stream().flatMap(user -> user.getMessages().stream()).collect(Collectors.toList());
        messages.sort(Comparator.comparing(Message::getSentOn));
        if (messages.size() > 5) {
            messages = messages.subList(messages.size() - 5, messages.size());
        }
        messages.forEach(message -> newUser.getChannel().writeAndFlush(message.getSentBy() + ": " + message.getMsg() + "\n"));
    }

    private void disconnect(ChannelHandlerContext context) {
        String room = getRoomFromContext(context);
        User whoLeft = getUserFromContext(context);
        chatRooms.get(room).remove(whoLeft);
        context.close();
        if(!room.equalsIgnoreCase(Values.CHAT_ROOMS[0])) {
            List<User> toSayGoodbye = chatRooms.get(room).stream()
                    .filter(user -> !user.getChannel().equals(context.channel()))
                    .collect(Collectors.toList());
            toSayGoodbye.forEach(user -> user.getChannel().writeAndFlush(String.format(Values.USER_LEFT, whoLeft.getUsername())));
        }
    }

    private void notRecognizedCommand(ChannelHandlerContext context, String msg) {
        context.channel().writeAndFlush(String.format(Values.COMMAND_NOT_RECOGNIZED, msg));
    }

    private void notInARoom(ChannelHandlerContext context) {
        context.channel().writeAndFlush(Values.NOT_IN_A_ROOM);
    }

    private void sendMessage(ChannelHandlerContext context, String msg) {
        User sender = getUserFromContext(context);
        if (isSpamming(sender)) {
            context.channel().writeAndFlush(Values.SPAM_NOT_ALLOWED);
        } else {
            List<User> receivers = chatRooms.get(getRoomFromContext(context)).stream()
                    .filter(user -> !user.getChannel().equals(sender.getChannel()))
                    .collect(Collectors.toList());
            sender.getMessages().add(new Message(sender.getUsername(), msg));
            receivers.stream().map(User::getChannel).forEach(channel -> channel.writeAndFlush(sender.getUsername() + ": " + msg + "\n"));
        }
    }

    private boolean isSpamming(User sender) {
        return sender.getMessages().stream().filter(message -> message.getSentOn().isAfter(LocalDateTime.now().minusMinutes(30))).count() > 30;
    }

    private String getRoomFromContext(ChannelHandlerContext context) {
        return chatRooms.keySet().stream().filter(key -> chatRooms.get(key).stream().map(User::getChannel).collect(Collectors.toList()).contains(context.channel())).findFirst().get();
    }

    private User getUserFromContext(ChannelHandlerContext context) {
        return chatRooms.values().stream().flatMap(Collection::stream).filter(user -> user.getChannel().equals(context.channel())).findFirst().get();
    }

    private boolean isCommand(String msg) {
        return msg.startsWith("/");
    }


    private static void initializeChatRooms() {
        String[] roomNames = Values.CHAT_ROOMS;
        for (String roomName : roomNames) {
            chatRooms.put(roomName.toUpperCase(), new ArrayList<>());
        }
    }

    private String generateUsername() {
        String[] usernames = Values.CHAT_USERNAMES;
        String username;
        do {
            int position = (int) (Math.random() * usernames.length);
            username = usernames[position];
        } while (alreadyInUse(username));
        return username;
    }

    private boolean alreadyInUse(String username) {
        return chatRooms.values().stream().anyMatch(users -> users.stream().map(User::getUsername).collect(Collectors.toList()).contains(username));
    }
}
