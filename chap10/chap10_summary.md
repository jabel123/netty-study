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
