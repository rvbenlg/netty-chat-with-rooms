package com.ubisoft.clients;

import com.ubisoft.constants.Values;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Client {


    public static void main(String[] args) throws Exception {
        new Client().run();
    }

    public void run() throws Exception{
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = new Bootstrap()
                    .group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ClientInitializer());

            Channel channel = bootstrap.connect(Values.HTTP_HOST, Values.HTTP_PORT).sync().channel();
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            while(true) {
                channel.writeAndFlush(in.readLine() + "\r\n");
            }
        } finally {
            group.shutdownGracefully();
        }
    }
}
