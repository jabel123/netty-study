package com.practice.chap12;

import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedNioFile;

import java.io.File;
import java.io.RandomAccessFile;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

public class HttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest>
{
    private final String wsUri;
    private static final File INDEX;
    static {
//        URL location = HttpRequestHandler.class.getProtectionDomain().getCodeSource().getLocation();
        Path location = Paths.get("src/main/resources");
        try
        {
            String path = location.toAbsolutePath() + "/index.html";
            path = !path.contains("file:") ? path : path.substring(5);
            INDEX = new File(path);
        } catch (Exception e)
        {
            throw new IllegalStateException("Unable to locate index.html", e);
        }
    }

    public HttpRequestHandler (String wsUri)
    {
        this.wsUri = wsUri;
    }

    @Override
    protected void channelRead0 (
            ChannelHandlerContext ctx, FullHttpRequest request
    ) throws Exception
    {
        System.out.println(request.uri());

        if (wsUri.equalsIgnoreCase(request.uri()))
        {
            ctx.fireChannelRead(request.retain());
        }
        else
        {
            if (HttpUtil.is100ContinueExpected(request))
            {
                send100Continue(ctx);
            }
            RandomAccessFile file = new RandomAccessFile(INDEX, "r");
            HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
            response.headers().set(
                    HttpHeaderNames.CONTENT_TYPE,
                    "text/plain; charset=UTF-8"
            );

            boolean keepAlive = HttpUtil.isKeepAlive(request);
            if (keepAlive)
            {
                response.headers().set(HttpHeaderNames.CONTENT_LENGTH, file.length());
                response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
            }

            ctx.write(response);

            if (ctx.pipeline().get(SslHandler.class) == null)
            {
                ctx.write(new DefaultFileRegion(file.getChannel(), 0, file.length()));
            }
            else
            {
                ctx.write(new ChunkedNioFile(file.getChannel()));
            }
            ChannelFuture future = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);

            if (!keepAlive)
            {
                future.addListener(ChannelFutureListener.CLOSE);
            }
        }
    }

    private void send100Continue (ChannelHandlerContext ctx)
    {
        DefaultFullHttpResponse response
                = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE);
        ctx.writeAndFlush(response);
    }

    @Override
    public void exceptionCaught (ChannelHandlerContext ctx, Throwable cause) throws Exception
    {
        cause.printStackTrace();
        ctx.close();
    }
}
