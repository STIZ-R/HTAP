package com.htap.meta;

import io.netty.buffer.ByteBuf;

public class PgMessage {
    public final byte type;
    public final ByteBuf payload;

    public PgMessage(byte type, ByteBuf payload) {
        this.type = type;
        this.payload = payload;
    }
}
