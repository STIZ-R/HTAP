package com.htap.meta;

import com.htap.meta.routing.QueryRouter;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.nio.charset.StandardCharsets;

public class PgWireSession extends SimpleChannelInboundHandler<PgMessage> {

    private final QueryRouter router;

    public PgWireSession(QueryRouter router) {
        this.router = router;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        sendAuthOk(ctx);
        sendParam(ctx, "server_version", "14.0");
        sendParam(ctx, "client_encoding", "UTF8");
        sendReady(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, PgMessage msg) throws Exception {
        if (msg.type == 'Q') {
            String sql = readCString(msg.payload);
            Object res = router.route(sql);
            PgResultWriter.write(ctx, res);
        }
    }

    private void sendAuthOk(ChannelHandlerContext ctx) {
        ctx.writeAndFlush(new PgMessage((byte)'R', Unpooled.buffer().writeInt(0)));
    }

    private void sendParam(ChannelHandlerContext ctx, String key, String value) {
        ByteBuf buf = Unpooled.buffer();
        writeCString(buf, key);
        writeCString(buf, value);
        buf.writeByte(0);
        ctx.writeAndFlush(new PgMessage((byte)'S', buf));
    }

    private void sendReady(ChannelHandlerContext ctx) {
        ByteBuf buf = Unpooled.buffer();
        buf.writeByte('I'); // transaction idle
        ctx.writeAndFlush(new PgMessage((byte)'Z', buf));
    }

    private static void writeCString(ByteBuf buf, String s) {
        buf.writeBytes(s.getBytes(StandardCharsets.UTF_8));
        buf.writeByte(0);
    }

    private static String readCString(io.netty.buffer.ByteBuf buf) {
        StringBuilder sb = new StringBuilder();
        while (buf.isReadable()) {
            byte b = buf.readByte();
            if (b == 0) break;
            sb.append((char)b);
        }
        return sb.toString();
    }
}
