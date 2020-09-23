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

import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ConnectTimeoutException
import io.netty.handler.timeout.ReadTimeoutException
import io.netty.handler.timeout.WriteTimeoutException
import io.netty.util.ReferenceCounted
import java.net.ConnectException
import java.net.SocketAddress

class Connection<O : ReferenceCounted>(
  val patty: Patty<O>,
  private val channel: Channel,
) {
  companion object {
    val CONNECTIONS = HashMap<SocketAddress, Connection<*>>()

    fun <O : ReferenceCounted> get(patty: Patty<O>, channel: Channel) =
      CONNECTIONS.computeIfAbsent(channel.remoteAddress()) {
        Connection(patty, channel)
      } as Connection<O>
  }

  private var disconnected = false
  private val address = channel.remoteAddress()

  init {
    channel.closeFuture().addListener(object : ChannelFutureListener {
      override fun operationComplete(future: ChannelFuture) {
        future.removeListener(this)
        disconnect("Connection lost")
      }
    })
  }

  fun sendPacket(packet: Packet<O>) {
    val cancelled = patty.protocol.serverListener?.let {
      !it.onPacketSending(packet, this)
    } ?: true
    if (!cancelled) {
      channel.writeAndFlush(packet).addListener(ChannelFutureListener { future ->
        if (future.isSuccess) {
          patty.protocol.serverListener?.also {
            it.onPacketSent(packet, this)
          }
        } else {
          exceptionCaught(future.cause())
        }
      })
    }
  }

  fun disconnect(reason: String, cause: Throwable? = null) {
    CONNECTIONS.remove(address)
  }

  fun connect() {
    CONNECTIONS.putIfAbsent(address, this)
  }

  fun isConnected() = channel.isOpen && !disconnected

  fun exceptionCaught(cause: Throwable) {
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
}