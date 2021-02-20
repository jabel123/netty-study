# 웹소켓

이전에 고안된 방식들이 만족스러운 결과를 제공하지 못한 것에서도 확인할 수 있듯이 HTTP의 요청/응답 설계에는 상당히 문제가 많다.

또한 시간형 웹 서비스의 의미론을 정식으로 정의하기 위한 학술적 논의도 이뤄지고 있지만 아직 합의된 정의는 나오지 않고 있다. 따라서 현재로서는 위키피디아에 나온 다음의 정의를 참고한다.

> 실시간 웹이란 사용자 ㄸ는 사용자의 소프트웨어가 주기적으로 업데이트를 확인하지 않고도 저자가 정보를 게시하는 즉시 정보를 받을 수 있는 기술과 방식을 적용한 네트워크 웹을 의미한다.

아직 실시간 웹은 시기상조라고 할 수 있지만, 기본 개념을 바탕으로 거의 즉시 정보에 접근할 수 있게 하는 서비스가 선보이고 있다. 

## 웹소켓 소개

웹소켓 프로토콜은 웹의 양방향 데이터 전송 문제에 대한 실용적인 솔루션을 제공하기 위해 클라이언트와 서버가 언제든지 메시지를 전송할 수 있게 허용하고, 결과적으로 메시지 수신을 비동기적으로 처리하게 요구하도록 완전히 새롭게 설계됐다(대부분의 최신 브라우저는 HTML5의 클라이언트 측 API로서 웹소켓을 지원한다.)

네티의 우베소켓 지원에는 현재 이용되고 있는 모든 주요 구현이 포함되므로 손쉽게 다음에 개발할 애플리케이션에 도입할 수 있다. 네티의 다른 기능과 마찬가지로 내부 구현 세부 사항에 대해서는 잘 몰라도 프로토콜을 완전히 이용할 수 있다. 

## 예제 웹소켓 애플리케이션
다음 예제 애플리케이션에서는 페이스북의 텍스트 메시징 기능과 비슷하게 브라우저 기반 채팅 애플리케이션을 구현하면서 웹소켓 프로토콜의 실시간 기느에 대해 알아본다.

**애플리케이션의 논리**
1. 클라이언트가 메시지를 보낸다.
2. 연결된 모든 클라이언트로 메시지가 브로드캐스팅 된다.

## 웹소켓 지원 추가

표준 HTTP 또는 HTTPS 프로토콜에서 웹소켓으로 전환하는 데는 업그레이드 핸드셰이크라는 매커니즘이 사용된다. 따라서 웹소켓을 이용하는 애플리케이션은 항상 HTTP/S로 시작한 다음 웹소켓으로 업그레이드 한다. 업그레이드되는 시점은 애플리케이션에 따라 다르다. 즉, 애플리케이션이 시작하는 동안 업그레이드 되거나 특정 URL이 요청될 때 업그레이드 될 수 있다.

이 예제에서는 요청된 URL이 /ws로 끝나는 경우 웹소켓으로 업그레이드하며, 그렇지 않으면 기본적인 HTTP/S를 이용한다. 일단 연결이 업그레이드되면 모든 데이터를 웹소켓을 이용해 전송한다. 

 > 웹소켓 프레임</br>
 웹 소켓은 각 메시지의 일부를 나타내는 프레임으로서 데이터를 전송하며, 한 메시지는 여러 프레임으로 구성될 수 있다.

 ## 웹소켓 프레임 처리

IETF에서 공개한 웹소켓 RFC에서는 6가지 프레임을 정의하며, 네티는 이러한 각 프레임을 위한 POJO구현을 제공한다.

*WebSocketFrmae 형식*
|프레임 형식|설명|
|---|---|
|BinaryWebSocketFrame|이진 데이터를 포함한다.|
|TextWebSocketFrame|텍스트 데이터를 포함한다.|
|ContinuationWebSocketFrame|이전 BinaryWebSocketFrame또는 TextWebSocketFrame에 속하는 텍스트 또는 이진 데이터를 포함한다.|
|CloseWebSocketFrame|CLOSE 요청을 나타내며, 닫기 상태의 코드와 구문을 포함한다.|
|PingWebSocketFrame|PongWebSocketFrame 전송을 요청한다.|
|PongWebSocketFrame|PingWebSocketFram에 대한 응답으로서 전송된다.

채팅 예제 애플리케이션에서는 다음과 같은 프레임 형식을 제공한다.
- CloseWebSocketFrame
- TextWebSocketFrame
- PongWebSocketFrame
- TextWebSocketFrame


## ChannelPipeline 초기화
ChannelHandler를 ChannelPipelien에 추가하려면 ChannelInitializer를 확장하고 initChannel()을 구현해야 한다.

```
public class ChatServerInitializer extends ChannelInitializer<Channel>
{
    private final ChannelGroup group;

    public ChatServerInitializer (ChannelGroup group)
    {
        this.group = group;
    }

    @Override
    protected void initChannel (Channel ch) throws Exception
    {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(new HttpServerCodec());
        pipeline.addLast(new ChunkedWriteHandler());
        pipeline.addLast(new HttpObjectAggregator(64 * 1024));
        pipeline.addLast(new HttpRequestHandler("/ws"));
        pipeline.addLast(new WebSocketServerProtocolHandler("/ws"));
        pipeline.addLast(new TextWebSocketFrameHandler("/ws"));
    }
}
```
*웹소켓 채팅 서버에 필요한 ChannelHandler*
|ChannelHandler|역할|
|---|---|
|HttpServerCodec | 바이트를 HttpRequest, HttpContent, LastHttpContent로 디코딩한다. HttpRequest, HttpContent, LastHttpContent를 바이트로 인코딩한다.|
|ChunkedWriteHandler|파일의 내용을 기록한다.|
|HttpObjectAggregator|HttpMessage 및 해당하는 후속HttpContent를 집계해 요청이나 응답을 처리하는지에 따라 단일 FullHttpRequest또는 FullHttpResponse를 생성한다. 이를 설치하면 파이프라인의 다음 ChannelHandler는 완전한 HTTP요청만 받는다.|
|HttpRequestHandler|/ws URI로 보내지 않은 요청에 해당하는 FullHttpRequest를 처리한다.|
|WebSocketServerProtocolHandler|웹소켓 사양에서 요구하는 대로 웹소켓 업그레이드 핸드셰이크 PingWebSocketFrame, PongWebSocketFrame, CloseWebSocketFrame을 처리한다.|
|TextWebSocketFramehandler|TextWebsocketFrame 및 핸드셰이크 완료 이벤트를 처리한다.|

## 부트스트랩

*서버부트스트랩*