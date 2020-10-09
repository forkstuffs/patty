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
import io.github.portlek.patty.Patty;
import io.github.portlek.patty.tcp.pipeline.TcpPacketCodec;
import io.github.portlek.patty.tcp.pipeline.TcpPacketEncryptor;
import io.github.portlek.patty.tcp.pipeline.TcpPacketSizer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import java.net.SocketAddress;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class TcpClientConnection extends Connection {

  private final int connectTimeout = 30;

  private final Class<? extends SocketChannel> tcpChannel = Epoll.isAvailable() ? EpollSocketChannel.class : NioSocketChannel.class;

  public TcpClientConnection(@NotNull final Patty patty, @NotNull final SocketAddress address) {
    super(patty, address);
  }

  @Override
  public void disconnect(@NotNull final String reason, @Nullable final Throwable cause) {
    super.disconnect(reason, cause);
    this.eventLoop.shutdownGracefully();
  }

  @Override
  public void connect(final boolean wait) {
    if (this.disconnected) {
      throw new IllegalStateException("Session has already been disconnected.");
    }
    try {
      final Bootstrap bootstrap = new Bootstrap()
        .channel(this.tcpChannel)
        .handler(new ChannelInitializer<Channel>() {
          @Override
          public void initChannel(final Channel channel) {
            channel.config().setOption(ChannelOption.IP_TOS, 0x18);
            channel.config().setOption(ChannelOption.TCP_NODELAY, false);
            final ChannelPipeline pipeline = channel.pipeline();
            TcpClientConnection.this.refreshReadTimeoutHandler(channel);
            TcpClientConnection.this.refreshWriteTimeoutHandler(channel);
            pipeline.addLast("encryption", new TcpPacketEncryptor(TcpClientConnection.this.patty));
            pipeline.addLast("sizer", new TcpPacketSizer(TcpClientConnection.this.patty));
            pipeline.addLast("codec", new TcpPacketCodec(TcpClientConnection.this));
            pipeline.addLast("manager", TcpClientConnection.this);
          }
        })
        .group(this.eventLoop)
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, this.connectTimeout * 1000)
        .remoteAddress(this.address);
      final Runnable connectTask = () -> {
        try {
          if (bootstrap.connect().sync().isSuccess()) {
            while (!this.isConnected() && !this.disconnected) {
              try {
                Thread.sleep(5);
              } catch (final InterruptedException ignored) {
              }
            }
          }
        } catch (final Throwable t) {
          this.exceptionCaught(null, t);
        }
      };
      if (wait) {
        connectTask.run();
      } else {
        new Thread(connectTask).start();
      }
    } catch (final Throwable t) {
      this.exceptionCaught(null, t);
    }
  }
}