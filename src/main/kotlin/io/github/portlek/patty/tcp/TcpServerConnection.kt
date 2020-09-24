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

package io.github.portlek.patty.tcp

import io.github.portlek.patty.Connection
import io.github.portlek.patty.PattyServer
import io.github.portlek.patty.tcp.pipeline.TcpPacketCodec
import io.github.portlek.patty.tcp.pipeline.TcpPacketEncryptor
import io.github.portlek.patty.tcp.pipeline.TcpPacketSizer
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.*
import io.netty.channel.epoll.Epoll
import io.netty.channel.epoll.EpollServerSocketChannel
import io.netty.channel.epoll.EpollSocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import java.net.SocketAddress

class TcpServerConnection(
  override val patty: PattyServer,
  address: SocketAddress
) : Connection(patty, address) {
  protected val tcpChannel = if (Epoll.isAvailable()) {
    EpollServerSocketChannel::class.java
  } else {
    NioServerSocketChannel::class.java
  }
  override fun channelActive(ctx: ChannelHandlerContext) {
    super.channelActive(ctx)
    patty.connections.add(this)
    patty.protocol.serverListener?.sessionAdded(patty, this)
  }

  override fun channelInactive(ctx: ChannelHandlerContext?) {
    super.channelInactive(ctx)
    patty.connections.remove(this)
    patty.protocol.serverListener?.sessionRemoved(patty, this)
  }

  override fun connect(wait: Boolean) {
    val future = ServerBootstrap()
      .channel(tcpChannel as Class<out ServerChannel>)
      .childHandler(object : ChannelInitializer<Channel>() {
        override fun initChannel(channel: Channel) {
          channel.config().setOption(ChannelOption.IP_TOS, 0x18)
          channel.config().setOption(ChannelOption.TCP_NODELAY, false)
          val pipeline = channel.pipeline()
          refreshReadTimeoutHandler(channel)
          refreshWriteTimeoutHandler(channel)
          pipeline.addLast("encryption", TcpPacketEncryptor(patty))
          pipeline.addLast("sizer", TcpPacketSizer(patty))
          pipeline.addLast("codec", TcpPacketCodec(this@TcpServerConnection))
          pipeline.addLast("manager", this@TcpServerConnection)
        }
      })
      .group(eventLoop)
      .bind(address)
    if (wait) {
      future.sync()
      patty.protocol.serverListener?.also {
        it.serverBound(patty, this)
      }
    } else {
      future.addListener(ChannelFutureListener {
        if (it.isSuccess) {
          patty.protocol.serverListener?.also { listener ->
            listener.serverBound(patty, this)
          }
        } else {
          System.err.println("[ERROR] Failed to asynchronously bind connection listener.")
          if (it.cause() != null) {
            it.cause().printStackTrace()
          }
        }
      })
    }
  }

  override fun close(wait: Boolean) {
    channel?.also {
      if (it.isOpen) {
        val future = it.close()
        if (wait) {
          future.sync()
          patty.protocol.serverListener?.also { listener ->
            listener.serverClosed(patty, this)
          }
        } else {
          future.addListener { listener ->
            if (listener.isSuccess) {
              patty.protocol.serverListener?.also { serverListener ->
                serverListener.serverClosed(patty, this)
              }
            }
          }
        }
      }
    }
    channel = null
    val future = eventLoop.shutdownGracefully()
    if (wait) {
      future.sync()
      patty.protocol.serverListener?.serverClosed(patty, this)
    } else {
      future.addListener {
        if (it.isSuccess) {
          patty.protocol.serverListener?.serverClosed(patty, this)
          return@addListener
        }
        System.err.println("[ERROR] Failed to asynchronously close connection listener.")
        if (it.cause() != null) {
          it.cause().printStackTrace()
        }
      }
    }
  }
}