package com.my;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;
import org.junit.Test;


import java.nio.charset.Charset;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.*;

public class ByteBufTest {

    @Test
    public void byteBufGetSetTest()
    {
        Charset charset = Charset.forName("UTF-8");
        ByteBuf buf = Unpooled.copiedBuffer("Netty in Action!", charset);

        assertThat('N', is((char)buf.getByte(0)));
        int readerIndex = buf.readerIndex();
        int writerIndex = buf.writerIndex();
        buf.setByte(0, 'B');

        assertThat('B', is((char)buf.getByte(0)));
        assertThat(readerIndex, is(0));
        assertThat(writerIndex, is(buf.writerIndex()));
    }

    @Test
    public void byteBufReadWriteTest()
    {
        Charset charset = CharsetUtil.UTF_8;
        ByteBuf buf = Unpooled.copiedBuffer("Netty in action!", charset);
        assertThat('N', is((char)buf.readByte()));

        int readerIndex = buf.readerIndex();
        int writerIndex = buf.writerIndex();

        buf.writeByte((byte) '?');

        assertThat(readerIndex, is(buf.readerIndex()));
        assertThat(writerIndex + 1, is(buf.writerIndex()));

    }
}
