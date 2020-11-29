package com.chap03.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class PlainNioServer
{
    public void serve(int port) throws IOException
    {
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        ServerSocket ssocket = serverSocketChannel.socket();
        InetSocketAddress address = new InetSocketAddress(port);
        ssocket.bind(address); // 서버를 선택한 포트로 바인딩
        Selector selector = Selector.open(); // 채널을 처리할 셀렉터를 염
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT); // 연결을 수락할 ServerSocket을 셀렉터에 등록
        final ByteBuffer msg = ByteBuffer.wrap("Hi!\r\n".getBytes());

        for(;;)
        {
            try
            {
                selector.select();
            }
            catch(IOException e)
            {
                e.printStackTrace();
                break;
            }
            Set<SelectionKey> readyKeys = selector.selectedKeys();
            final Iterator<SelectionKey> iterator = readyKeys.iterator();
            while (iterator.hasNext())
            {
                SelectionKey key = iterator.next();
                iterator.remove();

                try
                {
                    if (key.isAcceptable())
                    {
                        ServerSocketChannel server = (ServerSocketChannel) key.channel();
                        SocketChannel client= server.accept();
                        client.configureBlocking(false);
                        client.register(selector, SelectionKey.OP_WRITE | SelectionKey.OP_READ, msg.duplicate());
                        System.out.println("Accepted connection from " + client);
                    }
                    if (key.isWritable())
                    {
                        SocketChannel client = (SocketChannel) key.channel();
                        ByteBuffer buffer = (ByteBuffer) key.attachment();
                        while (buffer.hasRemaining())
                        {
                            if (client.write(buffer) == 0)
                            {
                                break;
                            }
                        }
                        client.close();
                    }
                }
                catch (IOException e)
                {
                    key.cancel();
                    try
                    {
                        key.channel().close();
                    }
                    catch (IOException ex)
                    {
                        // 종료시 무시함.
                    }
                }
            }
        }
    }
}
