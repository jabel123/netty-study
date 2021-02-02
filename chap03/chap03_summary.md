# 네티 컴포넌트와 설계

넓게 보면, 네티는 우리가 기술과 아키텍처라고 광범위하게 이야기하는 두 관심 영역의 문제를 모두 해결한다.  
-  첫째, 자바 NIO 기반의 비동기식 이벤트 기반 구현을 이용해 고부하 조건에서도 애플리케이션 성능과 확장성을 최대한으로 보장한다.
- 네티는 애플리케이션 논리를 네트워크 레이어로부터 분리하는 다양한 설계 패턴을 활용해 코드 테스트와 용이성, 모듈성, 재사용성능 극대화해 개발을 간소화 한다.

## 1.3 Channel, EventLoop, ChannelFuture

- Channel : 소켓(socket)
- EventLoop : 제어 흐름, 멀티스레딩, 동시성 제어
- ChannelFuture : 비동기 알림

### 1.3.1 Channel 인터페이스

기본 입출력 작업(bind(), connect(), read(), write())은 기본 네트ㅋ워크 전송에서 제공하는 기본형을 이요한다. 네티의 Channel인터페이스는 Socket으로 직접 작업할 때의 복잡성을 크게 완화하는 API를 제공한다.
- EmbeddedChannel
- LocalServerChannel
- NioDatagramChannel
- NioSctpChannel
- NioSocketChannel

### 1.3.2 EventLoop 인터페이스

EventLoop는 연결의 수명주기 중 발생하는 이벤트를 처리하는 네티의 핵심 추상화를 정의한다.
- 한 EventLoopGroup은 하나 이상의 EventLoop를 포함한다.
- 한 EventLoop는 수명주기 동안 한 Thread로 바인딩된다.
- 한 EventLoop에서 처리되는 모든 입출력 이벤트는 해당 전용 Thread에서 처리된다.
- 한 Channel은 수명주기 동안 한 EventLoop에 등록할 수 있따.
- 한 EventLoop를 하나 이상의 Channel로 할당할 수 있다.

### 3.1.3 ChannelFuture 인터페이스

네티의 모든 입출력 작업은 비동기적이다. 즉, 작업이 즉시 반환되지 않을 수 있으므로 나중에 결과를 확인하는 방법이 필요하다. 이를 위해 네티는 ChannelFuture를 제공하며, 이 인터페이스의 addListener() 메서드는 작업이 완료되면 알림을 받을 ChannelFutureListener 하나를 등록한다.


## 3.2 ChannelHandler와 ChannelPipeline

### 3.2.1 ChannelHandler 인터페이스

애플리케이션 개발자의 관점에서 네티의 핵심 컴포넌트는 인바운드와 아웃바운드 데이터의 처리에 적용되는 모든 애플리케이션 논리의 컨테이너 역할을 하는 ChannelHandler이다. 이것이 가능한 이유는 ChannelHandler의 메서드가 네트워크 이벤트에 의해 트리거되기 때문이다. 실제로 ChannelHandler는 데이터를 다른 포맷으로 변환하거나 작업 중 발생한 예외를 처리하는 등 거의 모든 종류의 작업에 활용할 수 있다.

### 3.2.2 ChannelPipeline 인터페이스

ChannelPipeline은 ChannelHandler 체인을 위한 컨테이너를 제공하며, 체인 상에서 인바운드와 아웃바운드 이벤트를 전파하는 API를 정의한다. 채널이 생성되면 여기에 자동적으로 자체적인 ChannelPipeline이 할당된다. 

인바운드 핸들러와 아웃바운드 핸들러를 동일한 파이프라인에 설치할 수 있따.

> 인바운드와 아웃바운드 핸드러에 대해
각 메서드에 인수로 제공되는 ChannelHandlerContext를 이용해 이벤트를 현재 체인의 다음 핸들러로 전달할 수 있따. 흥미가 없는 이벤트는 무시하는 경우가 있으므로 네티는 이를 감안해 추상 기본 클래스인 ChannelInboundHandlerAdapter와 ChannelOutboundHandlerAdapter를 제공한다. 각 클래스는 ChannelHadlerContext의 해당 메서드를 호출해 이벤트를 단순히 다음 핸들러로 전달하는 메서드 구현을 제공한다. 그 다음에는 원하는 메서드를 재정의해 클래스를 확장할 수 있다.

네티에서 메시지를 보내는 데는 Channel에 직접 기록하거나 ChannelHandler와 연결된 ChannelHandler객체에 기록하는 두 가지 방법이 있다. 전자의 방법은 메시지가 ChannelPipeline의 뒤쪽에서 시작되며, 후자의 방법은 메시지가 ChannelPipeline의 앞쪽에서 시작된다.

### 3.2.4 인코더와 디코더

네티로 메시지를 전송하거나 수신할 떄는 데이터를 변환해야 한다. 인바운드 메시지는 바이트에서 다른 포맷으로 변환되는 디코딩을 거친다. 아웃바운드 메시지는 반대로 현재 포멧에서 바이트로 인코딩된다. 이러한 두 가지 변환이 필요한 이유는 네트워크 데이터는 반드시 연속된 바이트여야 하기 때문이다.

네티가 제공하는 모든 인코더/디코더 어댑터 클래스는 ChannelInboundHandler나 ChannelOutboundHandler를 구현한다.

인바운드 데이터의 경우 인바운드 Channel에서 읽는 각 메시지에 대해 호출되는 channelRead 메서드/이벤트를 재정의한다. 이 메서드는 제공된 디코더의 decode()메서드를 호출한 후 디코딩된 바이트를 파이프라인 다음 ChannelInboundHandler로 전달한다.

## 3.3 부트스트랩

네티의 부트스트랩 클래스는 프로세스를 지정된 포트로 바인딩하거나 프로세스를 지정된 호스트의 지정된 포트에서 실행중인 다른 호스트로 연결하는 등의 일을 하는 애플리케이션의 네트워크 레이어를 구성하는 컨테이너를 제공한다.

|범주|Bootstrap|ServerBootstrap|
|---|---|---|
|네트워크 기능| 원격 호스트와 포트로 연결| 로컬 포트로 바인딩|
|EventLoopGroup의 수| 1 | 2|

서버는 각기 다른 Channel의 두 집합을 필요로 한다.
1. 로컬 포트로 바인딩된 서버 자체의 수신 소켓을 나타내는 ServerChannel 하나를 포함한다.
2. 서버가 수락한 연결마다 하나씩 들어오는 클라이언트 연결을 처리하기 위해 생성된 모든 Channel을 포함한다.

