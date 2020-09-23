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

import io.github.portlek.patty.ConnectionState
import io.github.portlek.patty.DisconnectReason
import io.github.portlek.patty.ConnectionBound
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import java.net.SocketAddress

class TcpConnection(
  ctx: ChannelHandlerContext,
  val bound: ConnectionBound
) {
  companion object {
    private val CONNECTIONS = HashMap<SocketAddress, TcpConnection>()

    fun get(ctx: ChannelHandlerContext, bound: ConnectionBound) =
      CONNECTIONS.getOrElse(ctx.channel().remoteAddress()) {
        TcpConnection(ctx, bound)
      }
  }

  private val channel = ctx.channel()
  private val address = channel.remoteAddress()

  @Volatile
  var state = ConnectionState.UNCONNECTED

  init {
    channel.closeFuture().addListener(object : ChannelFutureListener {
      override fun operationComplete(future: ChannelFuture) {
        future.removeListener(this)
        disconnect(DisconnectReason.DISCONNECTED)
      }
    })
  }

  fun disconnect(reason: DisconnectReason) {
    CONNECTIONS.remove(address)
  }

  fun connect() {
    CONNECTIONS.putIfAbsent(address, this)
  }

  fun isConnected() = CONNECTIONS.containsKey(address)
}