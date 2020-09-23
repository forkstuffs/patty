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

import io.github.portlek.patty.Session
import io.github.portlek.patty.packet.PacketOut
import io.github.portlek.patty.packet.PacketRegistry
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageCodec

class TcpPacketCodec(
  private val session: Session
) : ByteToMessageCodec<PacketOut>() {
  override fun encode(ctx: ChannelHandlerContext, packet: PacketOut, out: ByteBuf) {
    val initial = out.writerIndex()
    try {
      session.packetHeader.writePacketId(out, packet.id)
      packet.write(out)
    } catch (t: Throwable) {
      out.writerIndex(initial)
      if (session.onPacketError(t)) {
        throw t
      }
    }
  }

  override fun decode(ctx: ChannelHandlerContext, input: ByteBuf, out: MutableList<Any>) {
    val initial = input.readerIndex()
    try {
      val packetId = session.packetHeader.readPacketId(input)
      if (packetId == -1) {
        input.readerIndex(initial)
        return
      }
      val packet = PacketRegistry.getPacket(session.state, session.bound, packetId.toByte()) ?: return
      PacketRegistry.createPacket<TcpPacketIn>(packet).read(input, session)
      if (input.readableBytes() > 0) {
        throw IllegalStateException("Packet \"" + packet.javaClass.simpleName + "\" not fully read.")
      }
      out.add(packet)
    } catch (t: Throwable) {
      input.readerIndex(input.readerIndex() + input.readableBytes())
      if (session.onPacketError(t)) {
        throw t
      }
    }
  }
}