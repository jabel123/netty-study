package com.my.handler;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.ImmediateEventExecutor;

import java.net.InetSocketAddress;

public class ChatServer
{
    private final ChannelGroup channelGroup = new DefaultChannelGroup(ImmediateEventExecutor.INSTANCE);
    private final EventLoopGroup group = new NioEventLoopGroup();
    private Channel channel;

    public ChannelFuture start(InetSocketAddress address)
    {
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(group)
                .channel(NioServerSocketChannel.class)
                .childHandler(createInitializer(channelGroup));

        ChannelFuture future = bootstrap.bind(address);
        future.syncUninterruptibly();
        channel = future.channel();
        return future;
    }

    private ChannelInitializer<Channel> createInitializer (ChannelGroup channelGroup)
    {
        return new ChatServerInitializer(channelGroup);
    }

    private void destroy ()
    {
        if (channel != null)
        {
            channel.close();
        }
        channelGroup.close();
        group.shutdownGracefully();
    }

    public static void main (String[] args)
    {
        if (args.length != 1)
        {
            System.out.println("Please give port as argument");
            System.exit(1);
        }


        int port = Integer.parseInt(args[0]);
        final ChatServer endpoint = new ChatServer();
        System.out.println("The port is : " + args[0]);
        ChannelFuture future = endpoint.start(new InetSocketAddress(port));
        Runtime.getRuntime().addShutdownHook(new Thread(){
            @Override
            public void run ()
            {
                endpoint.destroy();
            }
        });
        future.channel().closeFuture().syncUninterruptibly();
    }
}