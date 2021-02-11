package com.practice.chap10;

import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Test;

public class IntegerToStringDecoderTest {
    @Test
    public void test()
    {
        EmbeddedChannel channel = new EmbeddedChannel(new IntegerToStringDecoder());
        channel.writeInbound(1);

        Object o = channel.readInbound();
        if (o instanceof Integer)
        {
            System.out.println("인테저");
        }
        else if (o instanceof String)
        {
            System.out.println("스트링");
        }
    }
}
