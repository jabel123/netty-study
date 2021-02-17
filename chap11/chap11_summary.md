# 네티에서 제공하는 ChannelHandler와 코덱

네티는 다양한 일반적인 프로토콜을 위한 코덱과 핸들러를 제공함으로써 지루한 작업에 필요한 시간과 노력을 크게 절약할 수 있게 해준다. 이번 장에서는 SSL/TLS와 웹소켓 지원을  비롯해 이러한 도구와 장점에 대해 알아보고 데이터 압축을 통해 HTTP의 성능을 최대한 개선하는 방법도 배운다.

## SSL/TLS를 이용한 네티 애플리케이션 보안
오늘날 데이터 보안은 아주 중요한 문제이므로 모든 개발자가 이에 대응할 준비를 갖춰야 한다. 최소한 다른 프로토콜 위쪽에서 데이터 보안을 구현하는 SSL및 TLS와 같은 암호화 프로토콜에 대해 알아야 한다. 이러한 프로토콜은 모든 보안 웹사이트에 기본적으로 이용되지만 SMTPS(Secure SMTP) 메일 서버나 관계형 데이터베이스 시스템과 같이 HTTP기반이 아닌 애플리케이션에도 이용된다.

자바는 SSL/TLS를 지원하기 위해 javax.net.ssl패키지를 지원하며, 이 패키지의 SSLContext와 SSLEngine클래스를 이용하면 아주 직관적으로 암호화와 암호 해독을 구현할 수 있다. 네티는 내부적으로 SSLEngine을 통해 실제 작업을 하는 SslHandler라는 ChannelHandler구현을 통해 이 API를 활용한다.

> 네티의 OpenSSL/SSLEngine 구현  
네티는 OpenSSL 툴킷을 이용하는 SSLEngine 구현도 제공한다. 이 OpenSslEngine 클래스는 JDK가 제공하는 SSLEngine구현에 비해 성능이 우수하다.
Open SSL 라이브러리를 이용할 수 있는 경우 기본적으로 OpenSslEngine을 이용하도록 네티 애플리케이션(클라이언트와 서버)을 구성할 수 있다. 라이브러리가 없는 경우 네티는 JDK 구현을 이용한다. OpenSSL지원을 구성하는 방법에 대한 자세한 내용은 네티 설명서를 참고한다. 
SSL API와 데이터 흐름은 JDK의 SSLEngine과 네티의 OpenSslEngine중 어떤 것을 이용하더라도 동일하다.

다음 예제는 SslHandler를 ChannelPipeline에 추가하기 위해 ChannelInitializer를 이용하는 방법이 나온다. ChannelInitializer는 Channel이 등록되면 ChannelPipeline을 설정하는데 이용된다.

*SSL/TLS 지원 추가*
```
public class SslChannelInitializer extends ChannelInitializer<Channel> 
{
    private final SslContext context;
    private final boolean startTls;

    public SslChannelInitializer(SslContext context, boolean startTls)
    {
        this.context = context;
        this.startTls = startTls;
    }

    @Override
    protected void initChannel(Channel ch) throws Exception {
        SSLEngine engine = context.newEngine(ch.alloc());
        ch.pipeline().addFirst("ssl", new SslHandler(engine, startTls));
    }
}
````

*SslHandler의 메서드*
|이름|설명|
|---|---|
|setHandshakeTimeout(long, TimeUnit)</br>setHandshakeTimeoutMillis(long)</br>getHandshakeTimeoutMillis()|핸드셰이크 ChannelFuture에 작업 실패를 알릴 시간 만료 값을 설정하거나 가져온다.|
|setCloseNotifyTimeout(long, TimeUnit)</br>setCloseNotifyTimeoutMillis(long)</br>getCloseNotifyTimeoutMillis()|연결 해제 알림을 트리거하여 연결을 닫을 시간 만료 값을 설정하거나 가져온다. ChannelFuture에도 작업 실패를 알린다.|
|handshakeFuture()|핸드셰이크가 완료되면 알릴 ChannelFuture를 반환한다. 핸드셰이크가 이전에 실행된 경우 이전 핸드셰이크의 결과를 포함하는 ChannelFuture를 반환한다.|
|close()</br>close(ChannelPromis)</br>close(ChannelHandlerContext, ChannelPromise)|기반 SslEngine을 닫고 삭제하도록 요청하는 close_notify를 전송한다.|

## 네티 HTTP/HTTPS 애플리케이션 개발

HTTP/HTTPS는 가장 일반적인 프로토콜이며, 스마트폰이 널리 보급되고 대부분의 기업에서 모바일 접근이 가능한 웹사이트를 제공하면서 더 많은 곳에서 이용되고 있다. 이러한 프로토콜은 다른 방법으로도 이용된다. 여러 기업에서 비즈니스 파트너와의 통신을 위해 이용하는 웹서비스 api는 일반적으로 HTTP(S)에 기반을 두고 있다.

### HTTP 디코더, 인코더, 코덱

HTTP는 요청/응답 패턴에 기반을 두고 있다. 클라이언트가 HTTP 요청을 서버로 보내면 서버가 HTTP 응답을 클라이언트로 보낸다. 네티는 이 프로토콜을 이용하는 작업을 간소화할 수 있는 다양한 인코더와 디코더를 제공한다.

*HTTP 지원 추가*
```
public class HttpPipelineInitializer extends ChannelInitializer<Channel> {
    private final boolean client;

    public HttpPipelineInitializer(boolean client)
    {
        this.client = client;
    }

    @Override
    protected void initChannel(Channel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        if (client) {
            pipeline.addLast("decoder", new HttpResponseDecoder());
            pipeline.addLast("encoder", new HttpRequestEncoder());
        }
        else 
        {
            pipeline.addLast("decoder", new HttpRequestDecoder());
            pipeline.addLast("encoder", new HttpResponseEncoder());
        }
    }
}
```

### HTTP 메시지 집합체

이니셜라이저를 설치한 후에는 ChannelPipeline의 핸들러가 여러 HttpObject 메세지를 대상으로 작업할 수 있다.

그런데 HTTP 요청과 응답은 여러 파트로 구성될 수 있으므로 먼저 이러한 파트를 연결해 완성된 메시지로 만들어야 한다. 네티는 이러한 반복적인 작업을 줄이기 위해 메시지 파트를 FullHttpRequest와 FullHttpResponse 메시지로 병합하는 집계자를 제공한다. 이 방법을 통해 전체 메시지 내용을 볼 수 있다.

그런데 전체 메시지를 다음 ChannelInboundHandler로 전달할 수 있을 때까지 메시지 세그먼트를 버퍼에 저장해야 하므로 이 기능을 이용하는 데는 약간의 부담이 따른다. 대신 메시지 단편화에 대해서는 걱정하지 않아도 된다.

*HTTP 메시지 조각을 자동으로 집계*
```
public class HttpAggregatorInitializer extends ChannelInitializer<Channel> {
    private final boolean isClient;

    public HttpAggregatorInitializer(boolean isClient)
    {
        this.isClient = isClient;
    }

    @Override
    protected void initChannel(Channel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        if (isClient) {
            pipeline.addLast("codec", new HttpClientCodec());
        }
        else {
            pipeline.addLast("codec", new HttpServerCodec());
        }
        pipeline.addLast("aggregator", new HttpObjectAggregator(512 * 1024)); // 최대 메시지 크기를 512KB로 지정하고 HttpObjectAggregator를 파이프라인에 추가
    }
}
```

### HTTP 압축

HTTP를 이용할 때는 압축을 적용해 전송되는 데이의 크기를 최대한 줄이는 것이 바람직하다. 압축을 적용하면 CPU 사용률이 증가하는 단점이 있지만 일반적으로는 이익이며 특히 텍스트 데이터의 경우 효과가 크다. 

네티는 gzip과 deflate 인코딩을 지원하는 압축과 압축 해제를 위한 ChannelHandler 구현을 제공한다.

>HTTP 요청 헤더
클라이언트는 다음과 같은 헤더를 제공해 지원하는 암호화 모드를 알릴 수 있다.  
    GET /encrypted-area HTTP/1.1  
    HOST: www.example.com  
    Accept-Encoding: gzip, deflate  
그러나 서버는 전송하는 데이터를 압축할 책임이 있는 것은 아니다.

*HTTP 메시지의 자동 압축*
```
public class HttoCompressInitializer extends ChannelInitializer<Channel> {
    private final boolean isClient;

    public HttoCompressInitializer(boolean isClient)
    {
        this.isClient = isClient;
    }

    @Override
    protected void initChannel(Channel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        if (isClient) {
            pipeline.addLast("codec", new HttpClientCodec());
            pipeline.addLast("decompressor", new HttpContentDecompressor());
        }
        else {
            pipeline.addLast("codec", new HttpServerCodec());
            pipeline.addLast("compressor", new HttpContentCompressor());
        }        
    }
}
```


### HTTPS 이용
*HTTPS 이용*
```
public class HttpsCodecInitializer extends ChannelInitializer<Channel> {
    private final SslContext context;
    private final boolean isClient;

    public HttpsCodecInitializer(boolean isClient, SslContext context)
    {
        this.isClient = isClient;
        this.context = context;
    }

    @Override
    protected void initChannel(Channel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        SSLEngine engine = context.newEngine(ch.alloc());
        pipeline.addFirst("ssl", new SslHandler(engine));

        if (isClient) {
            pipeline.addLast("codec", new HttpClientCodec());            
        }
        else {
            pipeline.addLast("codec", new HttpServerCodec());
        }        
    }
}
```

### 웹소켓
네티가 HTTP 기반 애플리케이션 개발을 위해 제공하는 광범위한 툴킷에는 몇 가지 고급 기능도 포함돼 있다. 이번 절에서는 2011년 IETF(Internet Engineering Task Force)에서 표준화한 프로토콜인 웹 소켓에 대해 다룬다.

웹 소켓은 요청-응답 상호작용에 기반을 두는 HTTP 프로토콜을 이용하면서 실시간으로 정보를 게시해야 하는 오랜 문제를 해결한다. AJAX가 문제를 어느정도 해결하겠지만 여전히 데이터 흐름이 클라이언트 측의 요청에 의해 주도되는 한계가 있었다. 몇 가지 영리한 해결 방법이 나왔지만 결국에는 모두 확장성이 제한된 임시 방편 수준을 넘지 못했다.

웹 소켓 사양과 구현은 더 효과적인 해결책을 제시한다. 간단히 말해 웹소켓은 "양방향 트래픽을 위한 단일 TCP 연결을 지원하며 웹소켓 API와 결합해 웹페이지에서 원격 서버로의 양방향 통신이 필요한 경우 HTTP 폴링에 대한 대안을 제시할 수 있따.

즉, 웹소켓은 클라이언트와 서버 간의 진정한 양방향 데이터 교환을 구현한다. 내부 사항에 대해 자세하게 언급하지는 않겠지만 초기 구현에는 텍스트 데이터만 지원하는 제한이 있었다. 현재는 일반 소켓과 비슷하게 어떤 데이터라도 이용할 수 있다.

애플리케이션에 웹 소켓 지원을 추가하려면 파이프라인에 적절한 클라이언트 측 또는 서버 측 웹소켓 ChannelHandler를 추가하면 된다. 이 클래스는 웹소켓이 정의하는 프레임이라는 특수한 메시지 형식을 처리한다.

*WebSocketFrame 형식*
|이름|설명|
|---|---|
|BinaryWebSocketFrame|데이터 프레임 : 이진 데이터|
|TextWebSocketFrame|데이터 프렝미 : 텍스트 데이터|
|ContinuationWebSocketFrame| 데이터 프렝미: 이전 BinaryWebSocketFrame 또는 TextWebSocketFrame에 속하는 텍스트 또는 이진 데이터|
|CloseWebSocketFrame|제어 프레임 :CLOSE 요청이며, 닫기 상태 코드와 구문을 포함|
|PingWebSocketFrame|제어 프레임: PongWebSocketFrame 요청|
|PongWebSocketFrame|제어 프레임: PingWebSocketFrame 요청에 대한 응답|

```
public class WebSocketServerInitializer extends ChannelInitializer<Channel> 
{
    @Override
    protected void initChannel(Channel ch) throws Exception
    {
        ch.pipeline().addLast(
            new HttpServerCoder(),
            new HttpObjectAggregator(65536),
            new WebSocketServerProtocolHandler("/websocket"),
            new TextFrameHandler(),
            new BinaryFrameHandler(),
            new ContinuationFrameHandler());
    }

    public static final class TextFrameHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {
        @Override
        public void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) throws Exception {
            // 텍스트 프레임 처리
        }
    }

    public static final class BinaryFrameHandler extends SimpleChannelInboundHandler<BinaryWebSocketFrame> {
        @Override
        public void channelRead0(ChannelHandlerContext ctx, BinaryWebSocketFrame msg) throws Exception {
            // 이진 프레임 처리
        }
    }

    public static final class ContinuationFrameHandler extends SimpleChannelInboundHandler<ContinuationWebSocketFrame> {
        @Override
        public void channelRead0(ChannelHandlerContext ctx, ContinuationWebSocketFrame msg) throws Exception {
            // 지속 프레임 처리
        }
    }
}
```

### 유휴 연결과 시간 만료

지금까지 네티의 특별한 코덱과 핸들러를 이용해 HTTP/HTTPS와 웹소켓을 처리하는 방법을 주로 알아봤다. 네트워크 리소스가 효율적으로 관리되는 경우 이러한 기술을 바탕으로 웹 애플리케이션의 효율, 편이성, 보안성을 더 개선할 수 있따. 다음으로 이러한 조건을 충족하기 위한 연결 관리에 대해 알아보자.

리소스를 시기적절하게 해제하려면 유휴 연결을 감지하고 시간을 만료시키는 것이 중요하다. 이러한 유휴 연결과 관련된 작업은 일반적이므로 네티는 이 작업만을 위한 여러 ChannelHandler 구현을 제공한다.

*유휴 연결과 시간 만료를 위한 ChannelHandler*
|이름|설명|
|---|---|
|IdleStateHandler|연결이 너무 오랫동안 유휴 상태인 경우 IdleStateEvent를 생성한다. IdleStateEvent를 처리하려면 ChannelInboundHandler에서 userEventTriggered()를 재정의한다.|
|ReadTimeoutHandler|지정한 기간 동안 인바운드 데이터를 받지 못한 경우 ReadTimeoutException을 생성하고 Channel을 닫느다. ReadTimeoutException을 감지하려면 ChannelHandler에서 exceptionCaught()를 재정의한다.|
|WriteTImeoutHandler|지정한 기간동안 인바운드 데이터를 받지 못한 경우 WriteTimeoutException을 생성하고 Channel을 닫는다. WriteTimeoutException을 감지하려면 ChannelHandler에서 exceptionCaught()를 재정의한다.|


*하트비트 전송*
```
public class IdelStateHandlerInitializer extends ChannelInitializer<Channel>
{
    @Override
    protected void initChannel(Channel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(new IdleStateHandler(0, 0, 60, TimeUnit.SECONDS));
        pipeline.addLast(new HeartbeatHandler());
    }

    public static final class HeartbeatHandler extends ChannelStateHandlerAdapter {
        private static final ByteBuf HEARTBEAT_SEQUENCE = Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("HEARTBEAT", CharsetUtil.ISO_8859_1));
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception 
    {
        if (evt instanceof IdleStateEvent) {
            ctx.writeAndFlush(HEARTBEAT_SEQUENCE.duplicate()).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
        }
        else {
            super.userEventTriggered(ctx, evt);
        }
    }
}
```

## 구분 기호 및 길이 기반 프로토콜의 디코딩

### 구분 기호 프로토콜
구분 기호 메시지 프로토콜은 프레임이라고 하는 메시지나 메시지 세그먼트의 시작과 끝을 정의된 문자로 표시한다. SMTP, POP3, IMAP, 텔넷과 같이 공식적으로 RFC 문서로 정의된 여러 프로토콜이 이러한 구분 기호 프로토콜에 해당하며 기업 내부에서 특정 포맷을 정의해 이용하는 경우도 있다.

다음의 디코더를 이용하면 어떤 프로토콜이던지 관계없이 다양한 토큰 시퀀스로 구분되는 프레임을 추출하는 커스텀 디코더를 정의할 수 있다.

|이름|설명|
|---|---|
|DelimiterBasedFrameDecoder|사용자가 제공한 구분 기호를 이용해 프레임을 추출하는 범용 디코더|
|LineBasedFrameDecoder|행 종료 \n 또는 \r\n 문자로 구분된 프레임을 추출하는 디코더. 이 디코더는 DelimiterBasedFrameDecoder보다 빠르다.|

*행 구분 프레임의 처리*
```
public class LineBasedHanlderInitializer extends ChannelInitializer<Channel> {

    @Override
    protected void initChannel(Channel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(new LineBasedFrameDecoder(64 * 1024));
        pipeline.addLast(new FrameHandler());
    }

    public static final class FrameHandler extends SimpleChannelInboundHandler<ByteBuf>{

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
            // 프레임에서 추출한 데이터로 필요한 작업을 처리
        }
    }
}

```

### 길이 기반 프로톸로

길이 기반 프로토콜은 특수한 구문 문자로 끝을 표시하지 않고 프레임의 헤더 세그먼트에 프레임의 길이를 인코딩 하는 방법으로 프레임을 정의한다.

*길이 기반 프로토콜을 위한 디코더*
|이름|설명|
|---|---|
|FixedLengthFrameDecoder|생성자를 호출할 때 지정한 고정 크기의 프레임을 추출한다.|
|LEngthFieldBasedFrameDecoder|프레임 헤더의 필드에 인코딩된 길이 값을 기준으로 프레임을 추출한다. 필드의 오프셋과 길이는 생성자에서 지정한다.|

```
public class LengthBasedInitializer extends ChannelInitializer<Channel> {
    @Override
    protected void initChannel(Channel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(new LengthFieldBasedFrameDecoder(64 * 1024, 0, 8));
        pipeline.addLast(new FrameHandler());
    }

    private static final class FrameHandler extends SimpleChannelInboundHandler<ByteBuf>
    {

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
            // 프레임을 이용해 필요한 일을 함
        }
    }
}
```

## 대용량 데이터 기록

대용량 데이터를 기록하는 작업은 네트워크 포화의 가능성 때문에 비동기 프레임워크의 특수한 문제로 여겨지고 있따. 기록 작업은 논블로킹이므로 완료 시 반환되며 모든 데이터를 기록하지 않았더라도 ChannelFuture에 알림을 전달한다. 이 경우 기록을 중단하지 않으면 메모리가 부족해질 우려가 있다. 따라서 대용량 데이터를 기록할 때는 원격 피어와의 느린 연결 속도 때문에 메모리 해제가 지연될 가능성에 대비해야 한다.

실제 작업은 네티의 코어에서 수행되므로 애플리케이션에서는 네티 API 설명서에 "제로 카피 파일 전송을 지원하는 Channel을 통해 전송되는 파일의 영역"이라고 정의된 FileRegion 인터페이스의 구현을 이용하기만 하면 된다.

다음 예제는 제로 카피 기능을 이용해 파일의 내용을 전송하기 위해 FileInputStream에서 DefaultFileRegion을 생성하고 이를 Channel에 기록하는 방법이다.

*FileRegion을 이용한 파일 내용 전송*
```
FileInputStream in = new FileInputStream(file);
FileRegion region = new DefaultFileRegion(in.getChannel(), 0, file.length());
channel.writeAndFlush(region).addListener(
    new ChannelFutureListener() {
        @Override
        public void operationCopmlete(ChannelFuture future) thrwos Exception
        {
            if (!future.isSuccess())
            {
                Throwable cause = future.cause();
            }
        }
    }
)
```

데이터를 파일 시스템에서 사용자 메모리로 복사해야 하는 경우 메모리 소비를 최소화하면서 대용량 데이터 스트림을 비동기식으로 기록하도록 지원하는 ChunkedWriteHandler를 이용할 수 있다. ChunkInput<B> 인터페이스는 대용량 파일 전송에 가장 중요한 역할을 하며, 여기서 매개변수 B는 readChunk()메서드에서 반환하는 형식이다.

|이름|설명|
|---|---|
|ChunkedFile|플랫폼에서 제로 카피를 지원하지 않거나 데이터를 전송해야 할 때 이용하기 위해 파일에서 청크 단위로 데이터를 가져온다.|
|ChunkedNioFile|ChunkedFile과 비슷하지만 FileChannel을 이용한다.|
|ChunkedStream|InputStream에서 청크 단위로 내용을 전송한다.|
|ChunkedNioStream|ReadableByteChannel에서 청크 단위로 내용을 전송한다.|


*ChunkedStream을 이용한 파일 내용 전송*
```
public class ChunkedWriteHandlerInitializer extends ChannelInitializer<Channel> {
    private final File file;
    private final SslContext context;

    public ChunkedWriteHandlerInitializer(File file, SslContext context) {
        this.file = file;
        this.context = context;
    }

    @Override
    protected void initChannel(Channel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();

        SslHandler sslHandler = context.newHandler(UnpooledByteBufAllocator.DEFAULT);
        pipeline.addLast(sslHandler);
        pipeline.addLast(new ChunkedWriteHandler());
        pipeline.addLast(new WriteStreamHandler());
    }

    public final class WriteStreamHandler extends ChannelInboundHandlerAdapter
    {
        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            super.channelActive(ctx);
            ctx.writeAndFlush(new ChunkedStream(new FileInputStream(file)));
        }
    }
}
```

## 데이터 직렬화
JDK는 네트워크를 통해 전송하는 기본형 데이터 형식과 POJO의 그래프를 직렬화/역직렬화하기 위한 ObjectOutputStream과 ObjectInputStream을 제공한다. API는 복잡하지 않으며 java.io.Serializable을 구현하는 모든 객체에 적용할 수 있지만 그다지 효율적이지도 않은 단점이 있따.

### JDK 직렬화
애플리케이션이 ObjectOutputStream과 ObjectInputStream을 이용하는 피어와 상호작용해야 하고 호환성을 중요하게 여긴다면 JDK 직렬화가 올바른 선택이다.

*JDK 직렬화 코덱*
|이름|설명|
|---|---|
|CompatibleObjectDecoder|JDK 직렬화를 이용하는 비-네티 피어와의 상호작용을 위한 디코더|
|CompatibleObjectEncoder|JDK 직렬화를 이용하는 비-네티 피어와의 상호작용을 위한 인코더|
|ObejctDecoder|JDK 직렬화를 바탕으로 커스텀 직렬화를 디코딩에 이용하는 디코더로서, 외부 의존성이 없는 경우 속도 향상을 제공한다. 반면 외부 의존성이 있으면 다른 직렬화 구현을 이용하는 것이 좋다.|
|ObejctEncoder|JDK 직렬화를 바탕으로 커스텀 직렬화를 인코딩에 이용하는 인코더로서, 외부 의존성이 없는 경우 속도 향상을 제공한다. 반면 외부 의존성이 있으면 다른 직렬화 구현을 이용하는 것이 좋다.|

### JBoss 마셜링을 이용한 직렬화
외부 의존성을 이용할 수 있는 경우 JDK 직렬화보다 3배나 빠르고 크기도 작은 JBoss 마셜링을 이용하는 것이 좋다. JBoss 마셜링 홈페이지에서는 JBoss 마셜링을 다음과 같이 소개하고 있따.

> JBoss 마셜링은 JDK 직렬화 API의 여러 문제를 해결하고 대체하기 위한 직렬화 API로서, java.io.Serializable 및 관련 API에 대한 완전한 호환성을 유지하면서 팩터리 구성(익스터널라이저, 클래스/인스턴스 조회 테이블, 클래스 확인, 객체 대체 등)을 통해 다양한 플러그 방식의 튜닝 가능 매개변수와 추가 기능을 제공한다.

*JBoss 마셜링 코덱*
|이름|설명|
|---|---|
|CompatibleMarshallingDecoder</br>CompatibleMarshallingEncoder|JDK 직렬화를 이용하는 피어와 호환된다|
|MarshallingDecoder</br> MarshallingEncoder|JBoss 마셜링을 이용하는 피어와 호환된다. 이러한 클래스는 한쌍으로 이용해야 한다.|

```
public class MarshallingInitializer extends ChannelInitializer<Channel> {

    private final MarshallerProvider marshallerProvider;
    private final UnmarshallerProvider unmarshallerProvider;

    public MarshallingInitializer(MarshallerProvider marshallerProvider
            , UnmarshallerProvider unmarshallerProvider)
    {
        this.marshallerProvider = marshallerProvider;
        this.unmarshallerProvider = unmarshallerProvider;
    }

    @Override
    protected void initChannel(Channel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(new MarshallingDecoder(unmarshallerProvider));
        pipeline.addLast(new MarshallingEncoder(marshallerProvider));
        pipeline.addLast(new ObjectHandler());
    }

    public static final class ObjectHandler extends SimpleChannelInboundHandler<Serializable>
    {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, Serializable msg) throws Exception {
            // 필요한 작업을 수행
        }
    }
}
```

### 프로토콜 버퍼를 통한 직렬화

네티는 마지막으로 구글이 개발하고 현재는 오픈소스인 프로토콜 버퍼를 활용하는 코덱을 직렬화 솔루션으로 제공한다.

프로토콜 버펄는 구조화된 데이터를 작고 효율적으로 인코딩/디코딩하며, 여러 프로그래밍 언어를 위한 바인딩을 제공하므로 다중 언어 프로젝트에 적합하다.

*protobuf코덱*
|이름|설명|
|---|---|
|ProtobufDecoder|protobuf를 이용해 메시지를 디코딩한다.|
|ProtobufEncode|protobuf를 이용해 메시지를 인코딩한다.|
|ProtobufVarint32FrameDecoder|수신한 ByteBuf를 메시지의 구글 프로토콜 "Base 128 Varints" 정수 길이 필드의 값을 기준으로 동적으로 분할한다.|

