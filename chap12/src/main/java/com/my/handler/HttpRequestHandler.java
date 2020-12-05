package com.my.handler;

import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedNioFile;

import java.io.File;
import java.io.RandomAccessFile;
import java.net.URL;

public class HttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest>
{
    private final String wsUri;
    private static final File INDEX;

    static {
        URL location = HttpRequestHandler.class.getClassLoader().getResource("index.html");
        try
        {
            String path = location.getPath().substring(1);
            System.out.println("The path : " +  path);
            path = !path.contains("file:") ? path : path.substring(5);
            INDEX = new File(path);
        }
        finally
        {

        }

    }

    public HttpRequestHandler (String wsUri)
    {
        this.wsUri = wsUri;
    }

    @Override
    protected void channelRead0 (ChannelHandlerContext ctx, FullHttpRequest request) throws Exception
    {
        if (wsUri.equalsIgnoreCase(request.uri()))
        {
            System.out.println("if ok request Uri " + request.uri());
            ctx.fireChannelRead(request.retain());
        }
        else
        {
            System.out.println("else ok request Uri " + request.uri());
            if (HttpUtil.is100ContinueExpected(request)) {
                send100Continue(ctx);
            }
            RandomAccessFile file = new RandomAccessFile(INDEX, "r");
            HttpResponse response = new DefaultHttpResponse(request.protocolVersion(), HttpResponseStatus.OK);
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8");
            boolean keepAlive = HttpUtil.isKeepAlive(request);
            if (keepAlive) {
                response.headers().set(HttpHeaderNames.CONTENT_LENGTH, file.length());
                response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderNames.KEEP_ALIVE);
            }
            ctx.write(response);
            if (ctx.pipeline().get(SslHandler.class) == null) {
                ctx.write(new DefaultFileRegion(
                        file.getChannel(), 0, file.length()));
            } else {
                ctx.write(new ChunkedNioFile(file.getChannel()));
            }
            ChannelFuture future = ctx.writeAndFlush(
                    LastHttpContent.EMPTY_LAST_CONTENT);
            if (!keepAlive) {
                future.addListener(ChannelFutureListener.CLOSE);
            }
        }
    }

    private void send100Continue (ChannelHandlerContext ctx)
    {
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE
        );

        ctx.writeAndFlush(response);
    }

    @Override
    public void exceptionCaught (ChannelHandlerContext ctx, Throwable cause) throws Exception
    {
        cause.printStackTrace();
        ctx.close();
    }
}
