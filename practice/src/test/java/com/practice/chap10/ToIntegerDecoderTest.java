package com.practice.chap10;

import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Test;

public class ToIntegerDecoderTest {

    @Test
    public void testDecode()
    {
        EmbeddedChannel channel = new EmbeddedChannel(new ToIntegerDecoder());
        channel.writeInbound(1);
        channel.writeInbound(2);
        channel.writeInbound(3);
        channel.writeInbound('김');
        channel.writeInbound(4);

        Object o1 = channel.readInbound();
        Object o2 = channel.readInbound();
        Object o3 = channel.readInbound();
        Object o4 = channel.readInbound();
        System.out.println(o1);
        System.out.println(o2);
        System.out.println(o3);
        System.out.println(o4);
    }
}
