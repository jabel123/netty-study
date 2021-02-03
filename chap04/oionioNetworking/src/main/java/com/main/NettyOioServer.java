package com.main;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.oio.OioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.oio.OioServerSocketChannel;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;

public class NettyOioServer {
    public void server(int port) throws InterruptedException {
        ByteBuf buf = Unpooled.unreleasableBuffer(
                Unpooled.copiedBuffer("Hi!\r\n", Charset.forName("UTF-8"))
        );
        EventLoopGroup group = new OioEventLoopGroup(); //OioEventLoopGroup을 이용해 블로킹 모드를 허용

        try {
            ServerBootstrap server = new ServerBootstrap();
            server.group(group)
                    .channel(OioServerSocketChannel.class)
                    .localAddress(new InetSocketAddress(port))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception { // 연결이 수락될 때마다 호출될 ChannelInitializer를 지정
                            ch.pipeline().addLast(new ChannelInboundHandlerAdapter(){ //이벤트를 가로채고 처리할 ChannelInboundHandlerAdapter를 추가
                                @Override
                                public void channelActive(ChannelHandlerContext ctx) throws Exception {
                                    // 클라이언트로 메시지를 출력하고 ChannelFutureListener를 추가해 메시지가 출력되면 연결을 닫음
                                    ctx.writeAndFlush(buf.duplicate()).addListener(ChannelFutureListener.CLOSE);
                                }

                            });
                        }
                    });
            ChannelFuture f = server.bind().sync(); // 서버를 바인딩해 연결을 수락
            f.channel().closeFuture().sync();
        } finally {
            group.shutdownGracefully().sync();
        }

    }
}
