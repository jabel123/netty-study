package com.practice.chap12;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;

public class TextWebSocketFrameHandler extends SimpleChannelInboundHandler<TextWebSocketFrame>
{
    private final ChannelGroup group;

    public TextWebSocketFrameHandler (ChannelGroup group)
    {
        this.group = group;
    }

    @Override
    public void userEventTriggered (ChannelHandlerContext ctx, Object evt) throws Exception
    {
        /**
         *  이벤트가 핸드셰이크 성공을 의미하는 경우 HTTP메시지는 더 이상 수신하지 않으므로 ChannelPipeline에서
         *  HttpRequestHandler를 제거
         */
        if (evt == WebSocketServerProtocolHandler.ServerHandshakeStateEvent.HANDSHAKE_TIMEOUT)
        {
            ctx.pipeline().remove(HttpRequestHandler.class);
            group.writeAndFlush(new TextWebSocketFrame("Client " + ctx.channel() + " joined"));
            group.add(ctx.channel());
        }
        else
        {
            super.userEventTriggered(ctx, evt);
        }
    }

    @Override
    protected void channelRead0 (
            ChannelHandlerContext ctx, TextWebSocketFrame msg
    ) throws Exception
    {
        group.writeAndFlush(msg.retain());
    }
}
