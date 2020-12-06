package com.my;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;

import java.net.InetSocketAddress;

public class LogEventMonitor
{
    private final EventLoopGroup group;
    private final Bootstrap bootstrap;

    public LogEventMonitor (InetSocketAddress address)
    {
        group = new NioEventLoopGroup();
        bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioDatagramChannel.class)
                .option(ChannelOption.SO_BROADCAST, true)
                .handler(new ChannelInitializer<Channel>()
                {
                    @Override
                    protected void initChannel (Channel ch) throws Exception
                    {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new LogEventDecoder());
                        pipeline.addLast(new LogEventHandler());
                    }
                })
                .localAddress(address);
    }
    public Channel bind() throws Exception
    {
        return bootstrap.bind().sync().channel();
    }
    public void stop()
    {
        group.shutdownGracefully();
    }

    public static void main (String[] args) throws Exception
    {
        if (args.length != 1)
        {
            throw new IllegalArgumentException();
        }

        LogEventMonitor monitor = new LogEventMonitor(
                new InetSocketAddress(Integer.parseInt(args[0]))
        );
        try
        {
            Channel channel = monitor.bind();
            System.out.println("LogMonitor runnging");
            channel.closeFuture().sync();
        }
        finally
        {
            monitor.stop();
        }

    }


}
