package com.htap.meta;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class PgBackendEncoder extends MessageToByteEncoder<PgMessage> {

    @Override
    protected void encode(ChannelHandlerContext ctx, PgMessage msg, ByteBuf out) {
        out.writeByte(msg.type);
        out.writeInt(4 + msg.payload.readableBytes());
        out.writeBytes(msg.payload);
    }
}
