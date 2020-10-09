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

import io.github.portlek.patty.*;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;

public final class TcpPacketCodec extends ByteToMessageCodec<Packet> {

  @NotNull
  private final Connection connection;

  private final Protocol protocol;

  public TcpPacketCodec(@NotNull final Connection connection) {
    this.connection = connection;
    this.protocol = connection.patty.protocol;
  }

  @Override
  public void encode(final ChannelHandlerContext ctx, final Packet packet, final ByteBuf buf) {
    final int initial = buf.readerIndex();
    try {
      this.protocol.getHeader().writePacketId(buf, packet.getId());
      packet.write(buf, this.connection);
    } catch (final Throwable t) {
      buf.writerIndex(initial);
      final ConnectionListener connectionListener = this.protocol.getConnectionListener();
      if (connectionListener != null) {
        if (connectionListener.packetError(t, this.connection)) {
          throw t;
        }
      }
    }
  }

  @Override
  public void decode(final ChannelHandlerContext ctx, final ByteBuf buf, final List<Object> out) throws Exception {
    final int initial = buf.readerIndex();
    try {
      final int id = this.protocol.getHeader().readPacketId(buf);
      if (id == -1) {
        buf.readerIndex(initial);
        return;
      }
      final Optional<Class<? extends Packet>> packetCls = PacketRegistry.getPacket(id);
      if (!packetCls.isPresent()) {
        buf.readerIndex(initial);
        return;
      }
      final Optional<Packet> packetOptional = PacketRegistry.createPacket(packetCls.get());
      if (!packetOptional.isPresent()) {
        buf.readerIndex(initial);
        return;
      }
      final Packet packet = packetOptional.get();
      packet.read(buf, this.connection);
      if (buf.readableBytes() > 0) {
        throw new IllegalStateException("Packet \"" + packet.getClass().getSimpleName() + "\" not fully read.");
      }
      out.add(packet);
    } catch (final Throwable t) {
      buf.readerIndex(buf.readerIndex() + buf.readableBytes());
      final ConnectionListener connectionListener = this.protocol.getConnectionListener();
      if (connectionListener != null && connectionListener.packetError(t, this.connection)) {
        throw t;
      }
    }
  }
}
