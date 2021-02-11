package com.practice.chap10;

import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Test;

public class ToIntegerDecoder2Test {
    @Test
    public void decodeTest()
    {
        EmbeddedChannel channel = new EmbeddedChannel(new ToIntegerDecoder2());
        channel.writeInbound(1);
        channel.writeInbound(2);
        channel.writeInbound(3);

        Object o = channel.readInbound();
        System.out.println(o);

    }
}
