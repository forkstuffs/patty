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

import io.github.portlek.patty.Connection;
import io.github.portlek.patty.util.ReadWrite;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import io.netty.handler.codec.DecoderException;
import java.util.List;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import org.jetbrains.annotations.NotNull;

public final class TcpPacketCompressor extends ByteToMessageCodec<ByteBuf> {

  private static final int MAX_COMPRESSED_SIZE = 2097152;

  @NotNull
  private final Connection connection;

  private final Deflater deflated = new Deflater();

  private final Inflater inflated = new Inflater();

  private final byte[] buf = new byte[8192];

  public TcpPacketCompressor(@NotNull final Connection connection) {
    this.connection = connection;
  }

  @Override
  public void encode(final ChannelHandlerContext ctx, final ByteBuf input, final ByteBuf out) {
    final int readable = input.readableBytes();
    if (readable < this.connection.compressionThreshold) {
      ReadWrite.writeVarInt(out, 0);
      out.writeBytes(input);
    } else {
      final byte[] bytes = new byte[readable];
      input.readBytes(bytes);
      ReadWrite.writeVarInt(out, bytes.length);
      this.deflated.setInput(bytes, 0, readable);
      this.deflated.finish();
      while (!this.deflated.finished()) {
        final int length = this.deflated.deflate(this.buf);
        out.writeBytes(this.buf, 0, length);
      }
      this.deflated.reset();
    }
  }

  @Override
  public void decode(final ChannelHandlerContext ctx, final ByteBuf input, final List<Object> out) {
    if (input.readableBytes() == 0) {
      return;
    }
    final int size = ReadWrite.readVarInt(input);
    if (size == 0) {
      out.add(input.readBytes(input.readableBytes()));
      return;
    }
    if (size < this.connection.getCompressionThreshold()) {
      throw new DecoderException("Badly compressed packet: size of $size is below threshold of ${connection.compressionThreshold}.");
    }
    if (size > TcpPacketCompressor.MAX_COMPRESSED_SIZE) {
      throw new DecoderException("Badly compressed packet: size of $size is larger than protocol maximum of $MAX_COMPRESSED_SIZE.");
    }
    final byte[] bytes = new byte[input.readableBytes()];
    input.readBytes(bytes);
    this.inflated.setInput(bytes);
    final byte[] inflatedArray = new byte[size];
    try {
      this.inflated.inflate(inflatedArray);
    } catch (final DataFormatException e) {
      e.printStackTrace();
    }
    out.add(Unpooled.wrappedBuffer(inflatedArray));
    this.inflated.reset();
  }
}