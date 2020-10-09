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
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import io.netty.handler.codec.CorruptedFrameException;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public final class TcpPacketSizer extends ByteToMessageCodec<ByteBuf> {

  @NotNull
  private final Protocol protocol;

  public TcpPacketSizer(@NotNull final Patty patty) {
    this.protocol = patty.protocol;
  }

  @Override
  public void encode(final ChannelHandlerContext ctx, final ByteBuf msg, final ByteBuf out) {
    final int length = msg.readableBytes();
    out.ensureWritable(this.protocol.getHeader().getLengthSize(length) + length);
    this.protocol.getHeader().writeLength(out, length);
    out.writeBytes(msg);
  }

  @Override
  public void decode(final ChannelHandlerContext ctx, final ByteBuf buf, final List<Object> out) throws Exception {
    final int size = this.protocol.getHeader().getLengthSize();
    if (size <= 0) {
      out.add(buf.readBytes(buf.readableBytes()));
      return;
    }
    buf.markReaderIndex();
    final byte[] lengthBytes = new byte[size];
    for (int index = 0; index < lengthBytes.length; index++) {
      if (!buf.isReadable()) {
        buf.resetReaderIndex();
        return;
      }
      lengthBytes[index] = buf.readByte();
      if (this.protocol.getHeader().isLengthVariable() && lengthBytes[index] >= 0 || index == size - 1) {
        final int length = this.protocol.getHeader().readLength(Unpooled.wrappedBuffer(lengthBytes), buf.readableBytes());
        if (buf.readableBytes() < length) {
          buf.resetReaderIndex();
          return;
        }
        out.add(buf.readBytes(length));
        return;
      }
    }
    throw new CorruptedFrameException("Length is too long.");
  }
}
