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
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.util.ReferenceCounted
import java.util.concurrent.LinkedBlockingQueue

abstract class PacketManager<O : ReferenceCounted>(
  protected val patty: Patty<O>
) : SimpleChannelInboundHandler<Packet<O>>() {
  private val packets = LinkedBlockingQueue<Packet<O>>()
  private var channel: Channel? = null
  private var disconnected = false
  private var packetHandleThread: Thread? = null
  protected lateinit var connection: Connection<O>

  override fun channelActive(ctx: ChannelHandlerContext) {
    if (disconnected || channel != null) {
      ctx.channel().close()
      return
    }
    channel = ctx.channel()
    connection = Connection.get(patty, channel!!)
    packetHandleThread = Thread {
      try {
        var packet: Packet<O>?
        while (packets.take().also { packet = it } != null) {
          patty.protocol.serverListener?.also {
            it.onPacketReceived(packet!!, connection)
          }
        }
      } catch (e: InterruptedException) {
      } catch (t: Throwable) {
        exceptionCaught(null, t)
      }
    }
    packetHandleThread!!.start()
    patty.protocol.serverListener?.also {
      it.onConnect(connection)
    }
  }

  override fun channelRead0(ctx: ChannelHandlerContext, packet: Packet<O>) {
    if (packet.hasPriority()) {
      patty.protocol.serverListener?.onPacketReceived(packet, connection)
    } else {
      packets.put(packet)
    }
  }
}