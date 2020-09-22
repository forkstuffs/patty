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
package io.github.portlek.patty

import io.github.portlek.patty.util.PoolSpec
import io.netty.bootstrap.Bootstrap
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelInitializer
import io.netty.channel.ServerChannel
import io.netty.channel.epoll.Epoll
import io.netty.channel.epoll.EpollDatagramChannel
import io.netty.channel.epoll.EpollEventLoopGroup
import io.netty.channel.epoll.EpollServerSocketChannel
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioDatagramChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import java.util.function.BiConsumer
import java.util.function.Consumer

class PattyServer(
  private val host: String,
  private val port: Int,
  private val channelClass: Class<out Channel>
) {
  private val eventLoop = if (Epoll.isAvailable()) {
    EpollEventLoopGroup(PoolSpec.UNCAUGHT_FACTORY)
  } else {
    NioEventLoopGroup(PoolSpec.UNCAUGHT_FACTORY)
  }
  private var whenServerBound = Consumer<PattyServer> { }
  private var whenServerClosing = Consumer<PattyServer> { }
  private var whenServerClosed = Consumer<PattyServer> { }
  private var whenSessionAdded = BiConsumer<PattyServer, Session> { _, _ -> }
  private var whenSessionRemoved = BiConsumer<PattyServer, Session> { _, _ -> }
  private var channel: Channel? = null

  fun whenServerBound(whenServerBound: Consumer<PattyServer>): PattyServer {
    this.whenServerBound = whenServerBound
    return this
  }

  fun whenServerClosing(whenServerClosing: Consumer<PattyServer>): PattyServer {
    this.whenServerClosing = whenServerClosing
    return this
  }

  fun whenServerClosed(whenServerClosed: Consumer<PattyServer>): PattyServer {
    this.whenServerClosed = whenServerClosed
    return this
  }

  fun whenSessionAdded(whenSessionAdded: BiConsumer<PattyServer, Session>): PattyServer {
    this.whenSessionAdded = whenSessionAdded
    return this
  }

  fun whenSessionRemoved(whenSessionRemoved: BiConsumer<PattyServer, Session>): PattyServer {
    this.whenSessionRemoved = whenSessionRemoved
    return this
  }

  fun bind(wait: Boolean = true) {
    val future = if (channelClass is SocketChannel) {
      ServerBootstrap()
        .channel(channelClass as Class<out ServerChannel>)
        .childHandler(object : ChannelInitializer<ServerChannel>() {
          override fun initChannel(channel: ServerChannel) {

          }
        })
        .bind()
    } else {
      Bootstrap()
        .bind()
    }
    if (wait) {
      channel = future.sync().channel()
    } else {
      future.addListener(ChannelFutureListener {
        if (it.isSuccess) {
          channel = it.channel()
        }
      })
    }
  }

  companion object {
    private val tcpChannel = if (Epoll.isAvailable()) {
      EpollServerSocketChannel::class.java
    } else {
      NioServerSocketChannel::class.java
    }
    private val udpChannel = if (Epoll.isAvailable()) {
      EpollDatagramChannel::class.java
    } else {
      NioDatagramChannel::class.java
    }

    fun tcp(host: String, port: Int) = PattyServer(host, port, tcpChannel)

    fun udp(host: String, port: Int) = PattyServer(host, port, udpChannel)
  }
}