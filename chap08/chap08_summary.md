# 부트스트랩

부트스트랩이란 애플리케이션을 실행하도록 구성하는 과정으로서, 특히 네트워크 애플리케이션의 경우 ㄷ이 정의처럼 단순하지 않은 단계를 거칠 수 있따.

네티는 애플리케이션 아키텍처에 대한 접근 방식과 일관되게 애플리케이션이 클라이언트 또는 서버든 관계없이 네트워크 레이어로부터 격리하는 방식을 지원한다. 곧 살펴보겠지만 모든 프레임워크 컴포넌트는 백그라운드에서 연결되고 활성화된다. 

부트스트랩은 지금까지 우리가 조립하던 퍼즐의 남은 조각으로서, 이 조각을 제자리에 맞추고 나면 네티 애플리케이션이 완성된다.

## 부트스트랩 클래스

네티의 부트스트랩 클래스 계층은 추상부모클래스 하나와 구상 부트스트랩 하위 클래스 두개로 구성된다.

서버는 부모 채널을 위해 클라이언트로부터 연결을 수락하고 통신하기 위해 자식채널을 생성하는 반면, 크라이언트는 모든 네트워크 상호작용을 위해 부모가 아닌 단일 채널을 필요로 하는 경우가 많다.

## 비연결 프로토콜과 클라이언트 부트스트랩

Bootstrap은 비연결 프로토콜을 이용하는 애플리케이션이나 클라이언트에 이용된다.

### 클라이언트 부트스트랩

*클라이언트 부트스트랩*
```
EventLoopGroup group = new NioEventLoopGroupo();
Bootstrap bootstrap = new Bootstrap();
bootstrap.group(group)
    .channel(NioSocketChannel.class)
    .handle(new SimpleChannelInboundHandler<ByteBuf>() {
        @Override
        public void channelRead0(
            ChannelHandlerContext channelHandlerContext,
            ByteBuf byteBUf) throws Exception {
                System.out.println("Received data");
            }
        )
    });
ChannelFuture future = bootstrap.connect(new InetSocketAddress("www.maiiing.com", 80));
future.addListener(new ChannelFutureListener() {
    @Override
    public void operationComplete(ChannelFuture channelFuture) throws Exception 
    {
        if (channelFuture.isSuccess()) {
            System.out.println("Connection established");
        }
        else {
            System.err.println("Conenction failed");
            channelFuture.cause().printStackTrace();
        }
    }
});

### Channel과 EventLoopGroup의 호환성

NioEventLoopGroup 및 OioSocketChannel과 같은 서로 다른 접두사를 가진 컴포넌트를 혼합할 수 없다.

## 서버 부트스트랩

ServerBootstrap 클래스에는 Bootstrap클래스에는 없는 childHandler(), childAttr(), childOption()메서드가 있다. 이러한 메서드는 서버 애플리케이션에서 자주 이용되는 작업을 지원한다. ServerChannel 구현은 수락된 연결을 나타내는 자식 Channel을 생성한느 역할을 한다. 즉, ServerChanel을 부트스트랩하는 ServerBootstrap은 수락된 Channel의 ChannelConfig 멤버에 설정을 적용하는 과정을 간소화하는 이러한 메서드를 제공한다.

*서버 부트스트랩*
```
NioEventLoopGroup group = new NioEventLoopGroup();
ServerBootstrap bootstrap = new ServerBootstrap();
bootstrap.group(group)
    .channel(NioServerSocketChannel.class)
    .childHandler(new SimpleChannelInboundHandler<ByteBuf>() {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, ByteBuf byteBuf) throws Exception {
            System.out.println("Received data");
        }
    });
    ChannelFuture future = bootstrap.bind(new InetSocketAddress(8080));
    future.addListener(new ChannelFutureListener() {
        @Override 
        public void operationComplete(ChannelFuture channelFuture) {
            if (channelFuture.isSuccess()) {
                System.out.println("Server bound");
            }
            else {
                channelFuture.cause().printStackTrace();
            }
        }
    });

## 채널에서 클라이언트 부트스트랩

서버가 다른 세번쨰 시스템에 대해 클라이언트로서 작동해야 하는 클라이언트 요청을 처리한다고 가정해보자. 프락시 서버 등의 애플리케이션을 웹 서비스나 데이터베이스와 같은 기존 기업 시스템에 통합해야 하는 경우 이러한 상황이 발생할 수 있따. 이러한 경우 ServerChannel에서 클라이언트 Channel을 부트스트랩 해야한다.

*서버 부트스트랩*
```
ServerBootstrap bootstrap = new ServerBootstrap();
bootstrap.group(new NioEventLoopGroup(), new NioEventLoopGroup())
    .channel(NioServerSocketChannel.class)
    .childHandler(
            new SimpleChannelInboundHandler<ByteBuf>() {
                ChannelFuture connectFuture;
                @Override
                public void channelActive(ChannelHandlerCOntext ctx)
                {
                    Bootstrap bootstrap = new Bootstrap();
                    bootstrap.channel(NioSocketChannel.class).handler(
                        new SimpleChannelInboundHandler<ByteBuf>() {
                            @Override
                            protected void channelRead0(
                                ChannelHandlerContext ctx, ByteBuf in
                            ) throws Exception {
                                System.out.println("Received data");
                            }
                        }
                    );

                    @Override
                    protected void channelRead0(
                        ChannelHandlerContext ctx, ByteBuf in
                    ) throws Exception {
                        if (connectionFuture.isDone()) {
                            //데이터를 이용해 필요한 일을 함.
                        }                    
                    }
                }

            }
    );
    
    ChannelFuture future = bootstrap.bind(new InetSocketAddress(8080));
    future.addListener(new ChannelFutureListener() {
        @Override
        public void operationComplete(ChannelFuture channelFuture) throws Exception
        {
            if (channelFuture.isSuccess()) {
                System.out.println("Server bound");
            }
            else {
                System.err.println("Bind attempt failed");
            }
        }
    });
```

~~

## 종료

부트스트랩은 애플리케이션을 시작하는 과정이지만 결국에는 애플리케이션을 정상적으로 종료해야 한다. 물론 애플리케이션을 종료할 떄 JVM이 모든 작업을 대신 처리하게 해도 되지만 이 경우 리소스를 깔끔하게 정리하는 것을 의미하는 정상 종료라고 말할 수 없다. 네티 애플리케이션을 종료하는데 그리 복잡한 기술이 필요한 것은 아니지만 몇 가지 염두에 둘 사항이 있다.

무엇보다 EventLoopGroup을 종료해 대기중인 이벤트와 작업을 모두 처리한 다음 모든 활성 스레드를 해제해야 한다. 이를 위해서는 EventLoopGroup.shutdownGracefully()를 호출해야 하며, 이 메서드는 종료가 완료되면 알림을 받을 Future를 반환한다. shutdownGracefully()는 비동기식으로 작업하므로 완료될 때까지 진행을 블로킹하거나 반환된 FUture로 완료 알림을 받을 리스너를 등록해야 한다.

**정상종료**
```
EventLoopGroup group = new NioEventLoopGroup();
Bootstrap bootstrap = new Bootstrap();
bootstrap.group(group)
    .channel(NioSocketChannel.class);

...
Future<?> future = group.shutdownGracefully();

future.syncUninterruptibly();