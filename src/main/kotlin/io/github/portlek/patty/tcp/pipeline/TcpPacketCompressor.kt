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

import io.github.portlek.patty.Connection
import io.github.portlek.patty.util.ReadWrite
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageCodec
import io.netty.handler.codec.DecoderException
import java.util.zip.Deflater
import java.util.zip.Inflater

class TcpPacketCompressor(
  private val connection: Connection
) : ByteToMessageCodec<ByteBuf>() {
  companion object {
    private const val MAX_COMPRESSED_SIZE = 2097152
  }

  private val deflated = Deflater()
  private val inflated = Inflater()
  private val buf = ByteArray(8192)

  override fun encode(ctx: ChannelHandlerContext, input: ByteBuf, out: ByteBuf) {
    val readable: Int = input.readableBytes()
    if (readable < connection.compressionThreshold) {
      ReadWrite.writeVarInt(out, 0)
      out.writeBytes(input)
    } else {
      val bytes = ByteArray(readable)
      input.readBytes(bytes)
      ReadWrite.writeVarInt(out, bytes.size)
      deflated.setInput(bytes, 0, readable)
      deflated.finish()
      while (!deflated.finished()) {
        val length = deflated.deflate(buf)
        out.writeBytes(buf, 0, length)
      }
      deflated.reset()
    }
  }

  override fun decode(ctx: ChannelHandlerContext, input: ByteBuf, out: MutableList<Any>) {
    if (input.readableBytes() == 0) {
      return
    }
    val size = ReadWrite.readVarInt(input)
    if (size == 0) {
      out.add(input.readBytes(input.readableBytes()))
      return
    }
    if (size < connection.compressionThreshold) {
      throw DecoderException("Badly compressed packet: size of $size is below threshold of ${connection.compressionThreshold}.")
    }
    if (size > MAX_COMPRESSED_SIZE) {
      throw DecoderException("Badly compressed packet: size of $size is larger than protocol maximum of $MAX_COMPRESSED_SIZE.")
    }
    val bytes = ByteArray(input.readableBytes())
    input.readBytes(bytes)
    inflated.setInput(bytes)
    val inflatedArray = ByteArray(size)
    inflated.inflate(inflatedArray)
    out.add(Unpooled.wrappedBuffer(inflatedArray))
    inflated.reset()
  }
}