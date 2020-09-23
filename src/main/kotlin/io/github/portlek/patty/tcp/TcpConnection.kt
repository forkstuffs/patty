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

import io.github.portlek.patty.*
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ConnectTimeoutException
import io.netty.handler.timeout.ReadTimeoutException
import io.netty.handler.timeout.WriteTimeoutException
import java.net.ConnectException
import java.net.SocketAddress

class TcpConnection(
  server: PattyServer<ByteBuf>,
  ctx: ChannelHandlerContext,
  bound: ConnectionBound
) : Connection<ByteBuf>(server, ctx, bound) {
  companion object {
    private val CONNECTIONS = HashMap<SocketAddress, TcpConnection>()

    fun get(server: PattyServer<ByteBuf>, ctx: ChannelHandlerContext, bound: ConnectionBound) =
      CONNECTIONS.computeIfAbsent(ctx.channel().remoteAddress()) {
        TcpConnection(server, ctx, bound)
      }
  }
  @Volatile
  var state = ConnectionState.UNCONNECTED
  private val address: SocketAddress = channel.remoteAddress()

  override fun sendPacket(packet: Packet<ByteBuf>) {
    val cancelled = server.protocol.listener?.let {
      !it.onPacketSending(packet)
    } ?: true
    if (!cancelled) {
      channel.writeAndFlush(packet).addListener(ChannelFutureListener { future ->
        if (future.isSuccess) {
          server.protocol.listener?.also {
            it.onPacketSent(packet)
          }
        } else {
          exceptionCaught(future.cause())
        }
      })
    }
  }

  private fun exceptionCaught(cause: Throwable) {
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

  override fun disconnect(reason: String, cause: Throwable?) {
    CONNECTIONS.remove(address)
  }

  fun connect() {
    CONNECTIONS.putIfAbsent(address, this)
  }

  fun isConnected() = CONNECTIONS.containsKey(address)
}