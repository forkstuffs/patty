/*
 * MIT License
 *
 * Copyright (c) 2020 Hasan Demirtaş
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package io.github.portlek.patty.tcp;

import io.github.portlek.patty.Connection;
import io.github.portlek.patty.PattyServer;
import io.github.portlek.patty.tcp.pipeline.TcpPacketCodec;
import io.github.portlek.patty.tcp.pipeline.TcpPacketEncryptor;
import io.github.portlek.patty.tcp.pipeline.TcpPacketSizer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.Future;
import java.net.SocketAddress;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class TcpServerConnection extends Connection {

  private final Class<? extends ServerSocketChannel> tcpChannel = Epoll.isAvailable() ? EpollServerSocketChannel.class : NioServerSocketChannel.class;

  @NotNull
  private final PattyServer patty;

  public TcpServerConnection(@NotNull final PattyServer patty, @NotNull final SocketAddress address) {
    super(patty, address);
    this.patty = patty;
  }

  @Override
  public void channelInactive(@Nullable final ChannelHandlerContext ctx) throws Exception {
    super.channelInactive(ctx);
    this.patty.connections.remove(this);
    if (this.patty.protocol.getServerListener() != null) {
      this.patty.protocol.getServerListener().sessionRemoved(this.patty, this);
    }
  }

  @Override
  public void connect(final boolean wait) {
    final ChannelFuture future = new ServerBootstrap()
      .channel(this.tcpChannel)
      .childHandler(new ChannelInitializer<Channel>() {
        @Override
        public void initChannel(final Channel channel) {
          channel.config().setOption(ChannelOption.IP_TOS, 0x18);
          channel.config().setOption(ChannelOption.TCP_NODELAY, false);
          final ChannelPipeline pipeline = channel.pipeline();
          TcpServerConnection.this.refreshReadTimeoutHandler(channel);
          TcpServerConnection.this.refreshWriteTimeoutHandler(channel);
          pipeline.addLast("encryption", new TcpPacketEncryptor(TcpServerConnection.this.patty));
          pipeline.addLast("sizer", new TcpPacketSizer(TcpServerConnection.this.patty));
          pipeline.addLast("codec", new TcpPacketCodec(TcpServerConnection.this));
          pipeline.addLast("manager", TcpServerConnection.this);
        }
      })
      .group(this.eventLoop)
      .bind(this.address);
    if (wait) {
      try {
        future.sync();
      } catch (final InterruptedException e) {
        e.printStackTrace();
      }
      if (this.patty.protocol.getServerListener() != null) {
        this.patty.protocol.getServerListener().serverBound(this.patty, this);
      }
    } else {
      future.addListener(it -> {
        if (it.isSuccess()) {
          if (this.patty.protocol.getServerListener() != null) {
            this.patty.protocol.getServerListener().serverBound(this.patty, this);
          }
        } else {
          System.err.println("[ERROR] Failed to asynchronously bind connection listener.");
          if (it.cause() != null) {
            it.cause().printStackTrace();
          }
        }
      });
    }
  }

  @Override
  public void close(final boolean wait) {
    if (this.channel != null) {
      if (this.channel.isOpen()) {
        final ChannelFuture future = this.channel.close();
        if (wait) {
          try {
            future.sync();
          } catch (final InterruptedException e) {
            e.printStackTrace();
          }
          if (this.patty.protocol.getServerListener() != null) {
            this.patty.protocol.getServerListener().serverClosed(this.patty, this);
          }
        } else {
          future.addListener(listener -> {
            if (listener.isSuccess()) {
              if (this.patty.protocol.getServerListener() != null) {
                this.patty.protocol.getServerListener().serverClosed(this.patty, this);
              }
            }
          });
        }
      }
      this.channel = null;
    }
    final Future<?> future = this.eventLoop.shutdownGracefully();
    if (wait) {
      try {
        future.sync();
      } catch (final InterruptedException e) {
        e.printStackTrace();
      }
      if (this.patty.protocol.getServerListener() != null) {
        this.patty.protocol.getServerListener().serverClosed(this.patty, this);
      } else {
        future.addListener(it -> {
          if (it.isSuccess()) {
            if (this.patty.protocol.getServerListener() != null) {
              this.patty.protocol.getServerListener().serverClosed(this.patty, this);
              return;
            }
            System.err.println("[ERROR] Failed to asynchronously close connection listener.");
            if (it.cause() != null) {
              it.cause().printStackTrace();
            }
          }
        });
      }
    }
  }

  @Override
  public void channelActive(final ChannelHandlerContext ctx) {
    super.channelActive(ctx);
    this.patty.connections.add(this);
    if (this.patty.protocol.getServerListener() != null) {
      this.patty.protocol.getServerListener().sessionAdded(this.patty, this);
    }
  }
}