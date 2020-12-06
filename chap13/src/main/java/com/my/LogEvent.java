package com.my;

import lombok.ToString;

import java.net.InetSocketAddress;

@ToString
public class LogEvent
{
    public static final byte SEPERATOR = (byte) ':';
    private final InetSocketAddress source;
    private final String logfile;
    private final String msg;
    private final long received;

    public LogEvent (InetSocketAddress source, long received, String logfile, String msg)
    {
        this.source = source;
        this.logfile = logfile;
        this.msg = msg;
        this.received = received;
    }

    public LogEvent (String logfile, String msg)
    {
        this(null, -1, logfile, msg);
    }

    public InetSocketAddress getSource ()
    {
        return source;
    }

    public String getLogfile ()
    {
        return logfile;
    }

    public String getMsg ()
    {
        return msg;
    }

    public long getReceived ()
    {
        return received;
    }
}
