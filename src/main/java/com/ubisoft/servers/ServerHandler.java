package com.ubisoft.servers;

import com.ubisoft.services.ChatService;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class ServerHandler extends SimpleChannelInboundHandler<String> {

    private final ChatService chatService = new ChatService();

    @Override
    public void handlerAdded(ChannelHandlerContext channelHandlerContext) {
        new Thread(() -> chatService.addUser(channelHandlerContext)).start();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, String msg) {
        new Thread(() -> chatService.processMessage(channelHandlerContext, msg)).start();
    }


}
