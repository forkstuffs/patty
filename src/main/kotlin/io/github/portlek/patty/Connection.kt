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

import io.github.portlek.patty.tcp.pipeline.TcpPacketCompressor
import io.netty.channel.*
import io.netty.channel.epoll.Epoll
import io.netty.channel.epoll.EpollEventLoopGroup
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.handler.timeout.ReadTimeoutException
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.handler.timeout.WriteTimeoutException
import io.netty.handler.timeout.WriteTimeoutHandler
import java.net.ConnectException
import java.net.SocketAddress
import java.util.concurrent.LinkedBlockingQueue

open class Connection(
  open val patty: Patty,
  val address: SocketAddress
) : SimpleChannelInboundHandler<Packet>() {
  val eventLoop = if (Epoll.isAvailable()) {
    EpollEventLoopGroup()
  } else {
    NioEventLoopGroup()
  }
  var readTimeout = 30
    private set
  var writeTimeout = 0
    private set
  private var packetHandleThread: Thread? = null
  protected val packets = LinkedBlockingQueue<Packet>()
  protected var channel: Channel? = null
  protected var disconnected = false
  var compressionThreshold = -1
    private set

  fun sendPacket(packet: Packet) {
    channel?.also {
      val cancelled = patty.protocol.sessionListener?.let { listener ->
        !listener.packetSending(packet, this)
      } ?: true
      if (!cancelled) {
        it.writeAndFlush(packet).addListener(ChannelFutureListener { future ->
          if (future.isSuccess) {
            patty.protocol.sessionListener?.also { listener ->
              listener.packetSent(packet, this)
            }
          } else {
            exceptionCaught(null, future.cause())
          }
        })
      }
    }
  }

  open fun disconnect(reason: String, cause: Throwable? = null) {
    if (disconnected) {
      return
    }
    disconnected = true
    packetHandleThread?.interrupt()
    if (channel?.isOpen == true) {
      patty.protocol.sessionListener?.disconnecting(this, reason, cause)
      channel!!.flush().close().addListener(ChannelFutureListener {
        patty.protocol.sessionListener?.disconnected(this, reason, cause)
      })
    } else {
      patty.protocol.sessionListener?.disconnected(this, reason, cause)
    }
    channel = null
  }

  open fun connect(wait: Boolean = true) {
  }

  open fun close(wait: Boolean = true) {
  }

  fun isConnected() = channel?.isOpen ?: false && !disconnected

  override fun channelRead0(ctx: ChannelHandlerContext, packet: Packet) {
    if (packet.hasPriority()) {
      patty.protocol.sessionListener?.packetReceived(packet, this)
    } else {
      packets.put(packet)
    }
  }

  override fun channelActive(ctx: ChannelHandlerContext) {
    if (disconnected || channel != null) {
      ctx.channel().close()
      return
    }
    channel = ctx.channel()
    packetHandleThread = Thread {
      try {
        var packet: Packet?
        while (packets.take().also { packet = it } != null) {
          patty.protocol.sessionListener?.also {
            it.packetReceived(packet!!, this)
          }
        }
      } catch (e: InterruptedException) {
      } catch (t: Throwable) {
        exceptionCaught(ctx, t)
      }
    }
    packetHandleThread!!.start()
    patty.protocol.sessionListener?.also {
      it.connected(this)
    }
  }

  override fun exceptionCaught(ctx: ChannelHandlerContext?, cause: Throwable) {
    val message = if (cause is ConnectTimeoutException || cause is ConnectException && cause.message!!.contains("connection timed out")) {
      "Connection timed out."
    } else if (cause is ReadTimeoutException) {
      "Read timed out."
    } else if (cause is WriteTimeoutException) {
      "Write timed out."
    } else {
      cause.toString()
    }
    disconnect(message, cause)
  }

  fun setCompressionThreshold(threshold: Int) {
    compressionThreshold = threshold
    channel?.also {
      if (compressionThreshold >= 0) {
        if (it.pipeline()["compression"] == null) {
          it.pipeline().addBefore("codec", "compression", TcpPacketCompressor(this))
        }
      } else if (it.pipeline()["compression"] != null) {
        it.pipeline().remove("compression")
      }
    }
  }

  fun setWriteTimeout(timeout: Int) {
    writeTimeout = timeout
    refreshWriteTimeoutHandler()
  }

  fun setReadTimeout(timeout: Int) {
    readTimeout = timeout
    refreshReadTimeoutHandler()
  }

  protected open fun refreshWriteTimeoutHandler() {
    channel?.also {
      refreshWriteTimeoutHandler(it)
    }
  }

  protected open fun refreshReadTimeoutHandler() {
    channel?.also {
      refreshReadTimeoutHandler(it)
    }
  }

  protected fun refreshReadTimeoutHandler(channel: Channel) {
    if (readTimeout <= 0) {
      if (channel.pipeline()["readTimeout"] != null) {
        channel.pipeline().remove("readTimeout")
      }
      return
    }
    if (channel.pipeline()["readTimeout"] == null) {
      channel.pipeline().addFirst("readTimeout", ReadTimeoutHandler(readTimeout))
    } else {
      channel.pipeline().replace("readTimeout", "readTimeout", ReadTimeoutHandler(readTimeout))
    }
  }

  protected fun refreshWriteTimeoutHandler(channel: Channel) {
    if (writeTimeout <= 0) {
      if (channel.pipeline()["writeTimeout"] != null) {
        channel.pipeline().remove("writeTimeout")
      }
      return
    }
    if (channel.pipeline()["writeTimeout"] == null) {
      channel.pipeline().addFirst("writeTimeout", WriteTimeoutHandler(writeTimeout))
    } else {
      channel.pipeline().replace("writeTimeout", "writeTimeout", WriteTimeoutHandler(writeTimeout))
    }
  }
}