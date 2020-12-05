package com.my.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;

public class TextWebSocketFrameHandler extends SimpleChannelInboundHandler<TextWebSocketFrame>
{
    private final ChannelGroup group;

    public TextWebSocketFrameHandler(ChannelGroup group)
    {
        this.group = group;
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx,
                                   Object evt) throws Exception
    {
        if (evt == WebSocketServerProtocolHandler.ServerHandshakeStateEvent.HANDSHAKE_COMPLETE)
        {
            ctx.pipeline().remove(HttpRequestHandler.class);
            group.add(ctx.channel());
            group.writeAndFlush(new TextWebSocketFrame("Client " + ctx.channel() + " joined"));
        } else
        {
            super.userEventTriggered(ctx, evt);
        }
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) throws Exception
    {
        System.out.println("msg : " + msg.text() + '\t' + msg);
        group.writeAndFlush(msg.retain());
    }
}
