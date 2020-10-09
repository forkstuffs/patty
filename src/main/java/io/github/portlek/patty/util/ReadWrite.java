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

package io.github.portlek.patty.util;

import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;

public final class ReadWrite {

  private ReadWrite() {
  }

  public static int readVarInt(@NotNull final ByteBuf buffer) {
    int result = 0;
    int indent = 0;
    int b = buffer.readByte();
    while ((b & 0x80) == 0x80) {
      if (!(indent < 21)) {
        throw new RuntimeException("Too many bytes for a VarInt32.");
      }
      result += (b & 0x7f) << indent;
      indent += 7;
      b = buffer.readByte();
    }
    result += (b & 0x7f) << indent;
    return result;
  }

  public static void writeVarInt(final ByteBuf buffer, int toWrite) {
    while ((toWrite & 0xFFFFFF80) != 0L) {
      buffer.writeByte(toWrite & 0x7F | 0x80);
      toWrite >>>= 7;
    }
    buffer.writeByte(toWrite & 0x7F);
  }
}
