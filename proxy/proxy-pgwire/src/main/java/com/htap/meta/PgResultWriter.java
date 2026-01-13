package com.htap.meta;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class PgResultWriter {

    public static void write(ChannelHandlerContext ctx, Object res) {

        if (res instanceof List) {
            List<Map<String,Object>> rows = (List<Map<String,Object>>) res;

            if (rows.isEmpty()) {
                sendCommandComplete(ctx, "SELECT 0");
                sendReady(ctx);
                return;
            }

            sendRowDesc(ctx, rows.get(0));

            for (Map<String,Object> row : rows) {
                sendDataRow(ctx, row);
            }

            sendCommandComplete(ctx, "SELECT " + rows.size());
        } else {
            sendCommandComplete(ctx, "UPDATE " + res);
        }

        sendReady(ctx);
    }

    private static void sendRowDesc(ChannelHandlerContext ctx, Map<String,Object> row) {
        ByteBuf buf = Unpooled.buffer();
        buf.writeShort(row.size());
        for (Map.Entry<String,Object> col : row.entrySet()) {
            writeCString(buf, col.getKey()); // column name
            buf.writeInt(0); // table OID
            buf.writeShort(0); // attr number
            buf.writeInt(getTypeOid(col.getValue())); // type OID
            buf.writeShort(-1); // type length (-1 = variable)
            buf.writeInt(0); // type modifier
            buf.writeShort(0); // format code 0=text
        }
        ctx.writeAndFlush(new PgMessage((byte)'T', buf));
    }

    private static void sendDataRow(ChannelHandlerContext ctx, Map<String,Object> row) {
        ByteBuf buf = Unpooled.buffer();
        buf.writeShort(row.size());
        for (Object val : row.values()) {
            if (val == null) {
                buf.writeInt(-1);
            } else {
                byte[] bytes = val.toString().getBytes(StandardCharsets.UTF_8);
                buf.writeInt(bytes.length);
                buf.writeBytes(bytes);
            }
        }
        ctx.writeAndFlush(new PgMessage((byte)'D', buf));
    }

    private static void sendCommandComplete(ChannelHandlerContext ctx, String tag) {
        ByteBuf buf = Unpooled.copiedBuffer(tag, StandardCharsets.UTF_8);
        ctx.writeAndFlush(new PgMessage((byte)'C', buf));
    }

    private static void sendReady(ChannelHandlerContext ctx) {
        ByteBuf buf = Unpooled.buffer();
        buf.writeByte('I'); // idle
        ctx.writeAndFlush(new PgMessage((byte)'Z', buf));
    }

    private static int getTypeOid(Object val) {
        if (val instanceof Integer) return PgTypes.INT4;
        if (val instanceof Long) return PgTypes.INT8;
        if (val instanceof Double) return PgTypes.FLOAT8;
        if (val instanceof Boolean) return PgTypes.BOOL;
        return PgTypes.TEXT; // default
    }

    private static void writeCString(ByteBuf buf, String s) {
        buf.writeBytes(s.getBytes(StandardCharsets.UTF_8));
        buf.writeByte(0);
    }
}
