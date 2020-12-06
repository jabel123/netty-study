package com.my;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class LogEventHandler extends SimpleChannelInboundHandler<LogEvent>
{
    @Override
    public void exceptionCaught (ChannelHandlerContext ctx, Throwable cause) throws Exception
    {
        cause.printStackTrace();
        ctx.close();
    }

    @Override
    protected void channelRead0 (ChannelHandlerContext ctx, LogEvent msg) throws Exception
    {
        System.out.println(msg);
    }
}
