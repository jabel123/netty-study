package com.practice.chap09;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AbsIntegerEncoderTest {

    @Test
    public void testEncodede() {
        ByteBuf buf = Unpooled.buffer();
        for (int i = 1; i < 10; i++)
        {
            buf.writeInt(i * -1);
        }

        EmbeddedChannel channel = new EmbeddedChannel(new AbsIntegerEncoder());
        assertTrue(channel.writeOutbound(buf));
        assertTrue(channel.finish());

        // 바이트를 읽음
        for (int i = 1; i < 10; i++)
        {
            assertEquals(i, (int) channel.readOutbound());
        }

        assertNull(channel.readOutbound());


    }
}
