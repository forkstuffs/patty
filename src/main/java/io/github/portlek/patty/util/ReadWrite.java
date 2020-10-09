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
import java.io.IOException;

public final class ReadWrite {

  private ReadWrite() {
  }

  public static int readVarInt(final ByteBuf buf) throws IOException {
    int value = 0;
    int size = 0;
    int b;
    while (((b = buf.readByte()) & 0x80) == 0x80) {
      value |= b & 0x7F << size++ * 7;
      if (size > 5) {
        throw new IOException("VarInt too long (length must be <= 5)");
      }
    }
    return value | b & 0x7F << size * 7;
  }

  public static void writeVarInt(final ByteBuf buf, int towrite) {
    while ((towrite & ~0x7F) != 0) {
      buf.writeByte(towrite & 0x7F | 0x80);
      towrite = towrite >>> 7;
    }
    buf.writeByte(towrite);
  }
}
