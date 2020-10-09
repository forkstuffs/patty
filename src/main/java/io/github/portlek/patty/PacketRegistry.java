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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;

public final class PacketRegistry {

  private static final Map<Class<? extends Packet>, Constructor<? extends Packet>> CTORS = new HashMap<>();

  private static final Map<Class<? extends Packet>, Integer> PACKET_IDS = new HashMap<>();

  private static final Map<Integer, Class<? extends Packet>> PACKETS = new HashMap<>();

  private PacketRegistry() {
  }

  @NotNull
  public static Optional<Packet> createPacket(@NotNull final Class<? extends Packet> cls) {
    return Optional.ofNullable(PacketRegistry.CTORS.get(cls))
      .map(constructor -> {
        try {
          return constructor.newInstance();
        } catch (final InstantiationException | IllegalAccessException | InvocationTargetException e) {
          throw new RuntimeException(e);
        }
      });
  }

  @NotNull
  public static Optional<Class<? extends Packet>> getPacket(final int id) {
    return Optional.ofNullable(PacketRegistry.PACKETS.get(id));
  }

  public static int getPacketId(@NotNull final Class<? extends Packet> cls) {
    final Integer identifier = PacketRegistry.PACKET_IDS.getOrDefault(cls, -1);
    if (identifier != -1) {
      return identifier;
    }
    throw new IllegalArgumentException(cls.getSimpleName() + " is not registered");
  }

  public static int getPacketId(final int info) {
    return info & 0x7ffffff;
  }

  public static void register(@NotNull final Class<? extends Packet> cls, final int id) {
    PacketRegistry.PACKET_IDS.put(cls, id);
    PacketRegistry.PACKETS.put(id, cls);
    try {
      PacketRegistry.CTORS.put(cls, cls.getDeclaredConstructor());
    } catch (final NoSuchMethodException e) {
      e.printStackTrace();
    }
  }
}
