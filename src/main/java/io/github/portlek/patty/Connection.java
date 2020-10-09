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

package io.github.portlek.patty;

import io.github.portlek.patty.tcp.pipeline.TcpPacketCompressor;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutException;
import io.netty.handler.timeout.WriteTimeoutHandler;
import java.net.ConnectException;
import java.net.SocketAddress;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class Connection extends SimpleChannelInboundHandler<Packet> {

  @NotNull
  public final Patty patty;

  protected final BlockingQueue<Packet> packets = new LinkedBlockingQueue<>();

  @NotNull
  protected final SocketAddress address;

  protected final EventLoopGroup eventLoop = Epoll.isAvailable() ? new EpollEventLoopGroup() : new NioEventLoopGroup();

  public int readTimeout = 30;

  public int writeTimeout = 0;

  public int compressionThreshold = -1;

  @Nullable
  protected Channel channel;

  protected boolean disconnected = false;

  @Nullable
  private Thread packetHandleThread;

  protected Connection(@NotNull final Patty patty, @NotNull final SocketAddress address) {
    this.patty = patty;
    this.address = address;
  }

  public void sendPacket(@NotNull final Packet packet) {
    if (this.channel == null) {
      return;
    }
    final boolean cancelled;
    final ConnectionListener connectionListener = this.patty.protocol.getConnectionListener();
    if (connectionListener != null) {
      cancelled = !connectionListener.packetSending(packet, this);
    } else {
      cancelled = true;
    }
    if (!cancelled) {
      this.channel.writeAndFlush(packet).addListener(future -> {
        if (future.isSuccess()) {
          connectionListener.packetSent(packet, this);
        } else {
          this.exceptionCaught(null, future.cause());
        }
      });
    }
  }

  public void disconnect(@NotNull final String reason) {
    this.disconnect(reason, null);
  }

  public void disconnect(@NotNull final String reason, @Nullable final Throwable cause) {
    if (this.disconnected) {
      return;
    }
    this.disconnected = true;
    if (this.packetHandleThread != null) {
      this.packetHandleThread.interrupt();
      this.packetHandleThread = null;
    }
    final ConnectionListener connectionListener = this.patty.protocol.getConnectionListener();
    if (this.channel != null && this.channel.isOpen()) {
      if (connectionListener != null) {
        connectionListener.disconnecting(this, reason, cause);
      }
      this.channel.flush().close().addListener(future -> {
        if (connectionListener != null) {
          connectionListener.disconnected(this, reason, cause);
        }
      });
    } else if (connectionListener != null) {
      connectionListener.disconnected(this, reason, cause);
    }
    this.channel = null;
  }

  public void connect() {
    this.connect(true);
  }

  public void connect(final boolean wait) {
  }

  public void close() {
    this.close(true);
  }

  public void close(final boolean wait) {
  }

  public boolean isConnected() {
    return this.channel != null && this.channel.isOpen() && !this.disconnected;
  }

  @Override
  public void channelRead0(final ChannelHandlerContext ctx, final Packet packet) {
    if (packet.hasPriority()) {
      final ConnectionListener connectionListener = this.patty.protocol.getConnectionListener();
      if (connectionListener != null) {
        connectionListener.packetReceived(packet, this);
      }
    } else {
      this.packets.add(packet);
    }
  }

  @Override
  public void channelActive(final ChannelHandlerContext ctx) {
    if (this.disconnected || this.channel != null) {
      ctx.channel().close();
      return;
    }
    this.channel = ctx.channel();
    final ConnectionListener connectionListener = this.patty.protocol.getConnectionListener();
    this.packetHandleThread = new Thread(() -> {
      try {
        Packet packet;
        while ((packet = this.packets.take()) != null) {
          if (connectionListener != null) {
            connectionListener.packetReceived(packet, this);
          }
        }
      } catch (final Throwable t) {
        this.exceptionCaught(null, t);
      }
    });
    this.packetHandleThread.start();
    if (connectionListener != null) {
      connectionListener.connected(this);
    }
  }

  @Override
  public void channelInactive(final ChannelHandlerContext ctx) throws Exception {
    if (ctx.channel() == this.channel) {
      this.disconnect("Connection closed.");
    }
  }

  @Override
  public void exceptionCaught(@Nullable final ChannelHandlerContext ctx, @NotNull final Throwable cause) {
    final String message;
    if (cause instanceof ConnectTimeoutException ||
      cause instanceof ConnectException && cause.getMessage().contains("connection timed out")) {
      message = "Connection timed out.";
    } else if (cause instanceof ReadTimeoutException) {
      message = "Read timed out.";
    } else if (cause instanceof WriteTimeoutException) {
      message = "Write timed out.";
    } else {
      message = cause.toString();
    }
    this.disconnect(message, cause);
  }

  public void setCompressionThreshold(final int threshold) {
    this.compressionThreshold = threshold;
    if (this.channel == null) {
      return;
    }
    if (this.compressionThreshold >= 0) {
      if (this.channel.pipeline().get("compression") == null) {
        this.channel.pipeline().addBefore("codec", "compression", new TcpPacketCompressor(this));
      }
    } else if (this.channel.pipeline().get("compression") != null) {
      this.channel.pipeline().remove("compression");
    }
  }

  public void setWriteTimeout(final int timeout) {
    this.writeTimeout = timeout;
    this.refreshWriteTimeoutHandler();
  }

  public void setReadTimeout(final int timeout) {
    this.readTimeout = timeout;
    this.refreshReadTimeoutHandler();
  }

  protected void refreshWriteTimeoutHandler() {
    if (this.channel != null) {
      this.refreshWriteTimeoutHandler(this.channel);
    }
  }

  protected void refreshReadTimeoutHandler() {
    if (this.channel != null) {
      this.refreshReadTimeoutHandler(this.channel);
    }
  }

  protected void refreshReadTimeoutHandler(@NotNull final Channel channel) {
    if (this.readTimeout <= 0) {
      if (channel.pipeline().get("readTimeout") != null) {
        channel.pipeline().remove("readTimeout");
      }
      return;
    }
    if (channel.pipeline().get("readTimeout") == null) {
      channel.pipeline().addFirst("readTimeout", new ReadTimeoutHandler(this.readTimeout));
    } else {
      channel.pipeline().replace("readTimeout", "readTimeout", new ReadTimeoutHandler(this.readTimeout));
    }
  }

  protected void refreshWriteTimeoutHandler(@NotNull final Channel channel) {
    if (this.writeTimeout <= 0) {
      if (channel.pipeline().get("writeTimeout") != null) {
        channel.pipeline().remove("writeTimeout");
      }
      return;
    }
    if (channel.pipeline().get("writeTimeout") == null) {
      channel.pipeline().addFirst("writeTimeout", new WriteTimeoutHandler(this.writeTimeout));
    } else {
      channel.pipeline().replace("writeTimeout", "writeTimeout", new WriteTimeoutHandler(this.writeTimeout));
    }
  }
}
