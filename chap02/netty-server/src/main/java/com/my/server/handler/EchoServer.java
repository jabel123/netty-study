package com.my.server.handler;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.net.InetSocketAddress;

public class EchoServer
{
    private final int port;

    public EchoServer(int port)
    {
        this.port = port;
    }

    public static void main(String[] args) throws InterruptedException
    {
        if (args.length != 1)
        {
            System.err.println(
                    "Usage: " + EchoServer.class.getSimpleName()
                    + "<port>"
            );
        }
        int port = Integer.parseInt(args[0]);
        new EchoServer(port).start();
    }

    private void start() throws InterruptedException
    {
        final EchoServerHandler serverHandler = new EchoServerHandler();
        EventLoopGroup group = new NioEventLoopGroup(); // EventLoopGroup을 생성
        
        try
        {
            ServerBootstrap b = new ServerBootstrap(); // ServerBootstrap을 생성
            b.group(group)
                    .channel(NioServerSocketChannel.class) // NIO 전송 채널을 이용하도록 설정
                    .localAddress(new InetSocketAddress(port)) //지정된 포트를 이용해 소켓 주소를 설정
                    .childHandler(new ChannelInitializer<SocketChannel>() // EchoServerHandler 하나를 채널의 ChannelPipeLine에 추가
                    {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception
                        {
                            ch.pipeline().addLast(serverHandler); // EchoSErverHandler는 @Sharable이므로 동일한 항목을 이용할 수 있음
                        }
                    });
            ChannelFuture f = b.bind().sync(); // 서버를 비동기식으로 바인딩, sync()는 바인딩이 완료되기를 대기
            f.channel().closeFuture().sync(); // 채널의 CloseFuture를 얻고 완료될 때까지 현재 스레드를 블로킹
        }
        finally
        {
            group.shutdownGracefully().sync();
        }


    }
}
