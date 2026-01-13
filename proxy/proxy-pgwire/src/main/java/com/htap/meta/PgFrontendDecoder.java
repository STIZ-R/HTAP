package com.htap.meta;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

public class PgFrontendDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> out) {
        if (buf.readableBytes() < 5) return;

        buf.markReaderIndex();
        byte type = buf.readByte();
        int len = buf.readInt();

        if (buf.readableBytes() < len - 4) {
            buf.resetReaderIndex();
            return;
        }

        ByteBuf payload = buf.readBytes(len - 4);
        out.add(new PgMessage(type, payload));
    }
}
