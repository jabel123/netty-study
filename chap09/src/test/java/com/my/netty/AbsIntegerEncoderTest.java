package com.my.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Test;
import static org.junit.Assert.*;

public class AbsIntegerEncoderTest
{
    @Test
    public void testEncoded()
    {
        ByteBuf buf = Unpooled.buffer();
        for (int i = 0; i < 10 ; i++)
        {
            buf.writeInt(i * -1);
        }

        EmbeddedChannel channel = new EmbeddedChannel(new AbsIntegerEncoder());
        assertTrue(channel.writeOutbound(buf));
        assertTrue(channel.finish());

        // 바이트를 읽음
        for (int i = 0; i < 10; i++)
        {
            assertEquals((Integer) i, channel.readOutbound());
        }
        assertNull(channel.readOutbound());
    }
}
