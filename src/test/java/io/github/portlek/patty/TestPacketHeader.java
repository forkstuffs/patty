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

package io.github.portlek.patty;

import io.github.portlek.patty.util.ReadWrite;
import io.netty.buffer.ByteBuf;
import java.io.IOException;
import org.jetbrains.annotations.NotNull;

public final class TestPacketHeader implements PacketHeader {

  @Override
  public boolean isLengthVariable() {
    return true;
  }

  @Override
  public int getLengthSize() {
    return 5;
  }

  @Override
  public int getLengthSize(final int length) {
    final int size;
    if ((length & -128) == 0) {
      size = 1;
    } else if ((length & -16384) == 0) {
      size = 2;
    } else if ((length & -2097152) == 0) {
      size = 3;
    } else if ((length & -268435456) == 0) {
      size = 4;
    } else {
      size = 5;
    }
    return size;
  }

  @Override
  public int readLength(@NotNull final ByteBuf input, final int available) throws IOException {
    return ReadWrite.readVarInt(input);
  }

  @Override
  public void writeLength(@NotNull final ByteBuf output, final int length) {
    ReadWrite.writeVarInt(output, length);
  }

  @Override
  public int readPacketId(@NotNull final ByteBuf input) throws IOException {
    return ReadWrite.readVarInt(input);
  }

  @Override
  public void writePacketId(@NotNull final ByteBuf output, final int packetId) {
    ReadWrite.writeVarInt(output, packetId);
  }
}
