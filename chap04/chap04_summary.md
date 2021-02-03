# 전송

네트워크를 통해 전송되는 데이터는 모두 동일한 형식(바이트)이다. 바이트가 전송되는 구체적인 방법은 데이터 전송의 기본 메커니즘을 추상화하도록 도와주는 개념인 네트워크 전송에 의해 좌우된다. 사용자는 이러한 세부 사항에 신경 쓸 필요가 없고, 바이트를 안정적으로 전송 및 수신하는 방법만 알면 된다.

자바를 이용한 네트워크 프로그래밍에 경험이 있다면 실무에서는 애초 예상했던 것보다 더 많은 동시 연결을 지원해야 하는 상황이 종종 발생한다는 것을 알고 있을 것이다. 이와 비슷하게, 블로킹 전송을 논블로킹 전송으로 전환하려고 해봤다면 두 네트워크 API의 큰 차이점 때문에 쉽지 않다는 것도 알 수 있다. 

반면 네티는 모든 전송 구현에 공통 API를 기반 레이어로 활용하므로 JDK를 이용할 때보다 이러한 변환이 훨씬 간단하다. 

## 4.1 사례 연구: 전송 마이그레이션

연결을 수락한 다음 클라이언트로 "HI!"라는 메시지를 전송하고 연결을 닫는 간단한 애플리케이션을 작성하면서 이 챕터가 시작된다.

### 4.1.1 네티없이 OIO와 NIO이용
네티없이 사용할 경우 ServerSocket을 사용하는 코드와 Selector, Channel조합으로 서버를 구현하는 경우의 코드가 너무 상이하다.

### 4.1.2 네티있이 OIO와 NIO이용
네티로 작성할 경우 OIO, NIO를 바꿀 경우 약간의 Channel관련 클래스만 수정하면 끝남.

즉, 어떤 전송방식을 선택하든 네티는 모든 전송의 구현에 동일한 API를 노출하므로 전송을 변경해도 코드는 거의 영향을 받지 않는다. 즉, 모든 구현이 Channel, ChannelPipeline, ChannelHandler 인터페이스를 기준으로 정의된다.

## 4.2 전송 API

Channel인터페이스는 모든 입출력 작업에 이용되므로 전송 API의 핵심이라고 할 수 있다. 
Channel에는 다음의 정보가 할당된다.

- ChannelConfig : Channel에 대한 모든 구성을 포함하며, 임시 변경을 지원한다. 특정한 전송에 고유 설정이 필요할 떄는 ChannelConfig의 하위 형식을 구현할 수도 있다.
- ChannelPipeline : 인바운드와 아웃바운드 데이터와 이벤트에 적용될 ChannelHandler 인스턴스를 모두 포함한다. 이러한 ChannelHandler는 애플리케이션에서 상태 변경과 데이터 처리를 위한 논리를 구성한다
  - ChannelHandler의 일반적인 용도
    - 데이터를 한 포맷에서 다른 포맷으로 변환
    - 예외에 대한 알림 제공
    - Channel의 활성화 또는 비활성화에 대한 알림 제공
    - Channel을 EventLoop에 등록할 때 또는 등록 해제할 때 알림 제공
    - 사용자 정의 이벤트에 대한 알림 제공

**Channel의 메서드**
|메서드 이름|설명|
|---|---|
|eventLoop | Channel에 할당된 EventLoop를 반환한다.|
|pipeline | Channel에 할당된 ChannelPipeline를 반환한다.|
|isActive|Channel이 활성 상태일 떄 true를 반환한다. 활성의 의미는 기본 전송이 무엇인지에 따라 달라진다. 예를들어, Socket 전송은 원격 피어로 연결되면 활성 상태이지만, Datagram 전송은 열리면 활성 상태다.|
|localAddress|로컬 SocketAddress를 반환한다.|
|remoteAddress|원격 SocketAddress를 반환한다.|
|write|데이터를 원격 피어로 출력한다. 이 데이터는 ChannelPipeline으로 전달되며 플러시되기 전까지 큐에 저장된다.|
|flush|기반 전송(예: Socket)으로 이전에 출력된 데이터를 플러시한다.|
|writeAndFlush|write()와 flush()를 모두 호출하는 편의 메서드|

## 4.3 포함된 전송

네티는 바로 사용할 수 있는 여러 전송을 기본 제공한다. 다만 이러한 전송이 모든 프로토콜을 지원하는 것은 아니므로 애플리케이션에서 이용하는 프로토콜과 호환되는 전송을 선택해야 한다. 

***네티가 제공하는 전송***
|이름|패키지|설명|
|---|---|---|
|NIO|io.netty.channel.socket.nio|java.nio.channels 패키지를 기반으로 이용(셀렉터 기반 방식)|
|Epoll|io.netty.channel.epoll|epoll()과 논블로킹 입출력을 위해 JNI를 이용한다. 이 전송은 SO_REQUEST와 마찬가지로 리눅스에서만 이용할 수 있으며, NIO전송보다 빠르다.|
|OIO|io.netty.channel.socket.oio|java.net 패키지를 기반으로 이용(블로킹 스트림 이용)
|로컬|io.netty.channel.local|VM에서 파이프를 통해 통신하는데 이용되는 로컬 전송|
|임베디드|io.netty.channel.embedded|실제 네트워크 기반 전송 없이 ChannelHandler를 이용할 수 있게 해주는 임베디드 전송. 이 전송은 ChannelHandler 구현을 테스트하는데 아주 유용하다.|

## 4.4 전송 사례

***전송과 네트워크 프로토콜 지원***
|전송|TCP|UDP|SCTP|UDT|
|---|---|---|---|---|
|NIO|지원|지원|지원|지원|
|Epoll|지원|지원|NA|NA|
|OIO|지원|지원|지원|지원|
