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
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageCodec

class TcpPacketEncryptor(
  private val protocol: Protocol<ByteBuf>
) : ByteToMessageCodec<ByteBuf>() {
  private var decryptedArray = ByteArray(0)
  private var encryptedArray = ByteArray(0)

  override fun encode(ctx: ChannelHandlerContext, msg: ByteBuf, out: ByteBuf) {
    if (protocol.encryptor != null) {
      val length = msg.readableBytes()
      val bytes = getBytes(msg)
      val outLength = protocol.encryptor!!.getEncryptOutputSize(length)
      if (encryptedArray.size < outLength) {
        encryptedArray = ByteArray(outLength)
      }
      out.writeBytes(this.encryptedArray, 0, protocol.encryptor!!.encrypt(bytes, 0, length, this.encryptedArray, 0))
    } else {
      out.writeBytes(msg)
    }
  }

  override fun decode(ctx: ChannelHandlerContext, buf: ByteBuf, out: MutableList<Any>) {
    if (protocol.encryptor != null) {
      val length = buf.readableBytes()
      val bytes = this.getBytes(buf)
      val result = ctx.alloc().heapBuffer(protocol.encryptor!!.getDecryptOutputSize(length))
      result.writerIndex(protocol.encryptor!!.decrypt(bytes, 0, length, result.array(), result.arrayOffset()))
      out.add(result)
    } else {
      out.add(buf.readBytes(buf.readableBytes()))
    }
  }

  private fun getBytes(buf: ByteBuf): ByteArray {
    val length = buf.readableBytes()
    if (decryptedArray.size < length) {
      decryptedArray = ByteArray(length)
    }
    buf.readBytes(decryptedArray, 0, length)
    return decryptedArray
  }
}