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

package io.github.portlek.patty.tcp.pipline

import io.github.portlek.patty.Protocol
import io.github.portlek.patty.packet.PacketOut
import io.github.portlek.patty.packet.PacketRegistry
import io.github.portlek.patty.tcp.TcpConnection
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageCodec

class TcpPacketCodec(
  private val protocol: Protocol
) : ByteToMessageCodec<PacketOut>() {
  override fun encode(ctx: ChannelHandlerContext, packet: PacketOut, buf: ByteBuf) {
    val initial = buf.readerIndex()
    try {
      protocol.header.writePacketId(buf, packet.id)
      packet.write(buf)
    } catch (t: Throwable) {
      buf.writerIndex(initial)
      if (protocol.listener.onPacketError(t)) {
        throw t
      }
    }
  }

  override fun decode(ctx: ChannelHandlerContext, buf: ByteBuf, out: MutableList<Any>) {
    val initial = buf.readerIndex()
    try {
      val id = protocol.header.readPacketId(buf)
      if (id == -1) {
        buf.readerIndex(initial)
        return
      }
      val connection = TcpConnection.get(ctx)
      PacketRegistry.getPacket(connection.state, )
      if (buf.readableBytes() > 0) {
        throw IllegalStateException ("Packet \"" + packet.getClass().getSimpleName() + "\" not fully read.")
      }
      out.add(packet)
    } catch (t: Throwable) {
      buf.readerIndex(buf.readerIndex() + buf.readableBytes())
      if (!protocol.listener.onPacketError(t)) {
        throw t
      }
    }
  }
}