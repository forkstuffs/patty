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

package io.github.portlek.patty.tcp.pipeline;

import io.github.portlek.patty.Patty;
import io.github.portlek.patty.Protocol;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public final class TcpPacketEncryptor extends ByteToMessageCodec<ByteBuf> {

  @NotNull
  private final Protocol protocol;

  private byte[] decryptedArray = new byte[0];

  private byte[] encryptedArray = new byte[0];

  public TcpPacketEncryptor(@NotNull final Patty patty) {
    this.protocol = patty.protocol;
  }

  @Override
  public void encode(final ChannelHandlerContext ctx, final ByteBuf msg, final ByteBuf out) {
    if (this.protocol.getEncryptor() != null) {
      final int length = msg.readableBytes();
      final byte[] bytes = this.getBytes(msg);
      final int outLength = this.protocol.getEncryptor().getEncryptOutputSize(length);
      if (this.encryptedArray.length < outLength) {
        this.encryptedArray = new byte[outLength];
      }
      out.writeBytes(this.encryptedArray, 0, this.protocol.getEncryptor().encrypt(bytes, 0, length, this.encryptedArray, 0));
    } else {
      out.writeBytes(msg);
    }
  }

  @Override
  public void decode(final ChannelHandlerContext ctx, final ByteBuf buf, final List<Object> out) {
    if (this.protocol.getEncryptor() != null) {
      final int length = buf.readableBytes();
      final byte[] bytes = this.getBytes(buf);
      final ByteBuf result = ctx.alloc().heapBuffer(this.protocol.getEncryptor().getDecryptOutputSize(length));
      result.writerIndex(this.protocol.getEncryptor().decrypt(bytes, 0, length, result.array(), result.arrayOffset()));
      out.add(result);
    } else {
      out.add(buf.readBytes(buf.readableBytes()));
    }
  }

  private byte[] getBytes(final ByteBuf buf) {
    final int length = buf.readableBytes();
    if (this.decryptedArray.length < length) {
      this.decryptedArray = new byte[length];
    }
    buf.readBytes(this.decryptedArray, 0, length);
    return this.decryptedArray;
  }
}