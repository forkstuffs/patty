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
import io.github.portlek.patty.Patty
import io.github.portlek.patty.tcp.pipeline.TcpPacketCodec
import io.github.portlek.patty.tcp.pipeline.TcpPacketEncryptor
import io.github.portlek.patty.tcp.pipeline.TcpPacketSizer
import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.epoll.Epoll
import io.netty.channel.epoll.EpollSocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import java.net.SocketAddress

class TcpClientConnection(
  patty: Patty,
  address: SocketAddress
) : Connection(patty, address) {
  private val connectTimeout = 30
  private val tcpChannel = if (Epoll.isAvailable()) {
    EpollSocketChannel::class.java
  } else {
    NioSocketChannel::class.java
  }

  override fun connect(wait: Boolean) {
    if (disconnected) {
      throw IllegalStateException("Session has already been disconnected.")
    }
    try {
      val bootstrap = Bootstrap()
        .channel(tcpChannel)
        .handler(object : ChannelInitializer<Channel>() {
          override fun initChannel(channel: Channel) {
            channel.config().setOption(ChannelOption.IP_TOS, 0x18)
            channel.config().setOption(ChannelOption.TCP_NODELAY, false)
            val pipeline = channel.pipeline()
            refreshReadTimeoutHandler(channel)
            refreshWriteTimeoutHandler(channel)
            pipeline.addLast("encryption", TcpPacketEncryptor(patty))
            pipeline.addLast("sizer", TcpPacketSizer(patty))
            pipeline.addLast("codec", TcpPacketCodec(this@TcpClientConnection))
            pipeline.addLast("manager", this@TcpClientConnection)
          }
        })
        .group(eventLoop)
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout * 1000)
      val connectTask = Runnable {
        try {
          if (bootstrap.connect(address).sync().isSuccess) {
            while (!isConnected() && !disconnected) {
              try {
                Thread.sleep(5)
              } catch (e: InterruptedException) {
              }
            }
          }
        } catch (t: Throwable) {
          exceptionCaught(null, t)
        }
      }
      if (wait) {
        connectTask.run()
      } else {
        Thread(connectTask).start()
      }
    } catch (t: Throwable) {
      exceptionCaught(null, t)
    }
  }

  override fun disconnect(reason: String, cause: Throwable?) {
    super.disconnect(reason, cause)
    eventLoop.shutdownGracefully()
  }
}