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

package io.github.portlek.patty.tcp.pipeline

import io.github.portlek.patty.Protocol
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageCodec

class TcpPacketSizer(
  private val protocol: Protocol<ByteBuf>
) : ByteToMessageCodec<ByteBuf>() {
  override fun encode(ctx: ChannelHandlerContext, msg: ByteBuf, out: ByteBuf) {
    val length = msg.readableBytes()
    out.ensureWritable(protocol.header.getLengthSize(length) + length)
    protocol.header.writeLength(out, length)
    out.writeBytes(msg)
  }

  override fun decode(ctx: ChannelHandlerContext, buf: ByteBuf, out: MutableList<Any>) {
    val size = protocol.header.lengthSize
    if (size > 0) {
      buf.markReaderIndex()
      val lengthBytes = ByteArray(size)
      lengthBytes.forEachIndexed { index, _ ->
        if (!buf.isReadable) {
          buf.resetReaderIndex()
          return
        }
        lengthBytes[index] = buf.readByte()
        if ((protocol.header.isLengthVariable && lengthBytes[index] >= 0) || index == size - 1) {
          val length = protocol.header.readLength(Unpooled.wrappedBuffer(lengthBytes), buf.readableBytes())
          if (buf.readableBytes() < length) {
            buf.resetReaderIndex()
            return
          }
          out.add(buf.readBytes(length))
          return
        }
      }
    } else {
      out.add(buf.readBytes(buf.readableBytes()))
    }
  }
}