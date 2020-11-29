package com.chap03.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.oio.OioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.oio.OioServerSocketChannel;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;

public class NettyOioServer
{
    public void serve(int port) throws InterruptedException
    {
        final ByteBuf byteBuf = Unpooled.unreleasableBuffer(
                Unpooled.copiedBuffer("Hi!\r\n", Charset.forName("UTF-8"))
        );

        EventLoopGroup group = new OioEventLoopGroup();

        try
        {
            ServerBootstrap b = new ServerBootstrap(); // ServerBootstrap 을 생성
            b.group(group)
                    .channel(OioServerSocketChannel.class) // OioEventLoopGroup을 이용해 블로킹 모드를 허용
                    .localAddress(new InetSocketAddress(port))
                    .childHandler(new ChannelInitializer<SocketChannel>() // 연결이 수락될 떄마다 호출될 ChannelInitializer를 저장
                    {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception
                        {
                            ch.pipeline().addLast(
                                    new ChannelInboundHandlerAdapter(){ // 이벤트를 가로채고 처리할 ChannelInboundHandlerAdapter를 추가.
                                        @Override
                                        public void channelActive(ChannelHandlerContext ctx) throws Exception
                                        {
                                            ctx.writeAndFlush(byteBuf.duplicate())
                                                    .addListener(ChannelFutureListener.CLOSE);
                                        }
                                    }
                            );
                        }
                    });
            ChannelFuture f = b.bind().sync();
            f.channel().closeFuture().sync();
        }
        finally
        {
            group.shutdownGracefully().sync();
        }
    }
}
