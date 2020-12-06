package com.my;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;

import java.io.File;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

public class LogEventBroadcaster
{
    private final EventLoopGroup group;
    private final Bootstrap bootstrap;
    private final File file;

    public LogEventBroadcaster (InetSocketAddress address, File file)
    {
        group = new NioEventLoopGroup();
        bootstrap = new Bootstrap();
        bootstrap.group(group).channel(NioDatagramChannel.class)
                .option(ChannelOption.SO_BROADCAST, true)
                .handler(new LogEventEncoder(address));
        this.file = file;
    }

    public void run() throws Exception
    {
        Channel channel = bootstrap.bind(0).sync().channel();
        long pointer = 0;
        for (;;)
        {
            long len = file.length();
            if (len < pointer)
            {
                // 파일이 재설정됨
                pointer = len;
            }
            else if (len > pointer)
            {
                // 콘텐츠가 추가됨
                RandomAccessFile raf = new RandomAccessFile(file, "r");
                raf.seek(pointer);
                String line;
                while ((line = raf.readLine()) != null)
                {
                    channel.writeAndFlush(new LogEvent(null, -1, file.getAbsolutePath(), line));
                }
                pointer = raf.getFilePointer();
                raf.close();
            }

            try
            {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e)
            {
                Thread.interrupted();
                break;
            }
        }
    }

    public void stop()
    {
        group.shutdownGracefully();
    }

    public static void main (String[] args) throws Exception
    {
        if (args.length != 2)
        {
            throw new IllegalArgumentException();
        }
        LogEventBroadcaster broadcaster = new LogEventBroadcaster(
                new InetSocketAddress("255.255.255.255", Integer.parseInt(args[0])), new File(args[1])
        );
        try
        {
            broadcaster.run();
        } finally
        {
            broadcaster.stop();
        }
    }
}
