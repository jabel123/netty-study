# 코덱 프레임워크

## 코덱이란?

모든 네트워크 애플리케이션은 피어 간에 전송되는 원시 바이트를 대상 프로그램의 데이터 포맷으로 구문 분석하고 변환하는 방법을 정의해야 한다. 이러한 변환 논리는 바이트 스트림을 한 포맷에서 다른 포맷으로 변환하는 인코더와 디코더로 구성된 코덱에 의해 처리된다. 

특정 애플리케이션에서 의미가 있는 바이트의 시퀀스 구조를 메시지라고 한다. 인코더는 이 메시지를 전송하기에 적합한 형식(대부분 바이트 스트림)으로 변환하며, 디코더는 네트워크 스트림을 다시 프로그램의 메시지 포맷으로 변환한다. 또한 인코더는 아웃바운드 데이터를 처리하며 디코더는 인바운드 데이터를 처리한다.

## 디코더

여기서는 네티의 디코더 클래스를 소개하고 이러한 클래스를 언제, 어떻게 사용해야 하는지 배울 수 있는 구체적인 예를 다룬다. 이러한 클래스는 고유한 두 사용 사례를 지원한다.
- 바이트 스트림을 메시지로 디코딩 : ByteToMessageDecoder 및 ReplayingDecoder
- 메시지를 다른 메시지 유형으로 디코딩 : MessageToMessageDecoder

디코더는 인바운드 데이터를 다른 포맷으로 변환하는 일을 하므로 네티의 디코더는 자연스럽게 ChannelInboundHandler를 구현한다.

디코더는 언제 이용할까? 인바운드 데이터를 ChannelPipeline 내의 다음 ChannelInboundHandler를 위해 변환할 때 이용한다. 또한 ChannelPipeline의 설계 방식 덕분에 여러 디코더를 체인으로 연결하는 방법으로 아무리 복잡한 변환 논리도 구현할 수 있다. 네티가 코드 모듈성과 재사용성을 지원하는 아주 좋은 예다.

### ByteToMessageDecoder 추상 클래스
바이트 스트림을 메시지(또는 다른 바이트의 시퀀스)로 디코딩하는 작업은 매우 일반적이므로 네티는 이 작업을 위한 추상 기본 클래스인 ByteToMessageDecoder를 제공한다. 원격 피어가 완성된 메시지를 한번에 보낼지는 알 수 없으므로 이 클래스는 인바운드 데이터가 처리할 만큼 모일 때까지 버퍼에 저장한다.

|메서드|설명|
|---|---|
|decoder(ChannelHandlerContext ctx, ByteBuf in, List<Object> out)|decoder()는 구현해야 하는 유일한 추상 메서드로서 들어오는 데이터가 포함된 ByteBuf와 디코딩된 메시지가 추가될 List를 받는다. 이 호출은 더 이상 List에 추가할 새로운 항목이 없거나 ByteBuf에 읽을 바이트가 없을 때까지 반복된다. 그 이후 List가 비어있지 않은 경우 그 내용이 파이플가인의 다음 핸들러로 전달된다.
|decodeLast(ChannelHandlerContext ctx, ByteBuf in, List<Object> out)|네티가 제공하는 기본 구현은 단순히 decode()를 호출한다. 이 메서드는 Channel이 비활성화될 때 한 번 호출된다. 특별한 처리가 필요한 경우 이 메서드를 재정의한다.|
</br>

``` 
public class ToIntegerDecoder extends ByteToMessageDecoder {
    @Override
    public void decode(ChnnaleHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception
    {
        if (in.readableBytes() >= 4) 
        {
            out.add(in.readInt());
        }
    }
}
```

ReplayingDecoder를 이용하면 readbleBytes를 사용할 필요가 없어진다.
```
public class ToIntegerDecoder2 extends ReplayingDecoder<Void> {
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        out.add(in.readInt());
    }
}
```

ReplayingDecoderBuffer에는 다음과 같은 측면이 있다.
- 모든 ByteBuf 작업이 지원되는 것은 아니다. 지원되지 않는 메서드를 호출하면 UnsupportedOperationException이 발생한다.
- ReplayingDecoder는  ByteToMessageDecoder보다 약간 느리다.


>추가 디코더  
다음 클래스로 더 복잡한 사용 사례를 처리할 수 있다.  
io.netty.handler.codec.LineBasedFrameDecoder : 네티에서 내부적으로 이용하는 클래스로서 줄바꿈 문자(\n또는 \r\n)를 이용해 메시지 데이터를 구문분석한다.
io.netty.handler.codec.http.HttpObjectDecoder : HTTP 데이터용 디코더
io.netty.handler.codec의 하위패키지에서 특수한 사용 살를 위한 추가 인코더와 디코드를 찾을 수 있다. 자세한 내용은 네티 Javadoc을 참고한다.

### MessageToMessageDecoder 추상 클래스
추상 기본 클래스를 이용해 메시지 포맷을 변환(예: POJO의 한 형식에서 다른 형식으로)

|메서드|설명|
|---|---|
|decode(ChannelInboundContext ctx, I msg, List<Object> out)|인바운드 메시지를 다른 포맷으로 디코딩할 때마다 호출한다. 디코딩된 메시지는 파이프라인의 다음 ChannelInboundHandler로 전달된다.|

### TooLongFrameException 클래스

네티는 비동기 프레임워크이므로 디코딩할 수 있을때까지 바이트 메모리를 버퍼에 저장해야 한다. 또한 디코더가 메모리를 소진할 만큼 데이터를 저장하지 않게 해야한다. 네티는 이 문제를 해겨ㅑㄹ하기 위해 프레임이 지정한 크기를 초과하면 발생하는 TooLongFrameException 예외를 제공한다.


## 인코더
인코더는 ChannelOutboundHandler를 구현하고 아웃바운드 데이터를 한 포맷에서 다른 포맷으로 변환하며, 앞서 살펴본 디코더의 기능을 반대로 수행한다. 네티는 다음과 같은 기능을 가진 인코더를 작성하도록 지원한다.
- 메시지를 바이트로 인코딩
- 메시지를 다른 메시지로 인코딩.


*MessageToByteEncoder API*
|메서드|설명|
|---|---|
|encode(ChannelHandlerContext ctx, I msg, ByteBuf out)| encode 메서드는 구현해야 하는 유일한 추상 메서드이며, ByteBuf로 인코딩할 아웃바운드 메시지(I 형식)를 전달하고 호출한다. 그런 다음 ByteBuf는 파이프라인 내의 다음 ChannelOutboundHandler로 전달된다.||

*MessageToMessage API*
|메서드|설명|
|---|---|
|encoder(ChannelHandlerContext ctx, I msg, List<Object> out)|encoder()는 구현해야 하는 유일한 메서드다. write()로 기록한 각 메시지는 encode()로 전달된 후 하나 이상의 아웃바운드 메시지로 인코딩된다. 그런 다음 파이프라인 내의 다음 ChannelOutboundHandler로 전달된다.|


## 추상 코덱 클래스

지금까지는 디코더와 인코더를 별개의 엔티티로 다뤘지만, 인바운드/ 아웃바운드 데이터와 메시지 변환을 한 클래스에서 관리하는 것이 편리한 경우가 있따. 네티의 추상 코덱 클래스는 디코더/인코더를 한 쌍으로 묶어 지금까지 알아본 두 작업을 함께 처리하므로 이러한 용도에 편리하게 이용할 수 있다. 이러한 클래스는 ChannelInboundHandler와 ChannelOutobundHandler를 모두 구현한다.

그렇다면 디코더와 인코더를 별도로 이용할 필요 없이 항상 이러한 복합 클래스를 이용하지 않는 이유는 무엇일까? 그 이유는 두 가지 기능을 따로 분리해 네티의 설계 원칙인 코드 재사용성과 확장성을 극대화하기 위해서다.

### ByteToMessageCodec 추상클래스
### MessageToMessageCodec 추상클래스

### CombinedChannelDuplexHandler 클래스
디코더와 인코더를 결한ㅂ하면 재사용성이 저하되지만 디코더와 인코더를 단일 유닛으로 배포하는 편의성을 포기하지 않고 재사용성의 저하를 방지하는 방법이 있다. 다음과 같이 선언된 CombinedChannelDuplexHandler를 이용하는 것이다.

public class CombinedChannelDuplexHandler <I extends ChannelInboundHandler, O extends ChannelOutboundHandler>

