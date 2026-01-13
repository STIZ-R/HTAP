package com.htap.meta;

import com.htap.meta.routing.QueryRouter;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class PgWireServer {

    private final int port;
    private final QueryRouter router;

    public PgWireServer(int port, QueryRouter router) {
        this.port = port;
        this.router = router;
    }

    public void start() throws InterruptedException {
        EventLoopGroup boss = new NioEventLoopGroup(1);
        EventLoopGroup workers = new NioEventLoopGroup();

        ServerBootstrap b = new ServerBootstrap();
        b.group(boss, workers)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(
                                new PgFrontendDecoder(),
                                new PgBackendEncoder(),
                                new PgWireSession(router)
                        );
                    }
                });

        b.bind(port).sync();
    }
}
