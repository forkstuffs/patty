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

package io.github.portlek.patty

import io.netty.util.ReferenceCounted
import java.lang.reflect.Constructor
import java.util.*

object PacketRegistry {
  private val CTORS = HashMap<Class<out Packet>, Constructor<out Packet>>()
  private val PACKET_IDS = HashMap<Class<out Packet>, Int>()
  private val PACKETS = HashMap<Int, Class<out Packet>>()

  fun createPacket(cls: Class<out Packet>) = CTORS[cls]?.newInstance()

  fun getPacket(id: Int) = PACKETS[id]

  fun getPacketId(cls: Class<out Packet>): Int {
    val identifier = PACKET_IDS.getOrDefault(cls, -1)
    if (identifier != -1) {
      return identifier
    }
    throw IllegalArgumentException(cls.simpleName + " is not registered")
  }

  fun getPacketId(info: Int) = info and 0x7ffffff

  fun register(cls: Class<out Packet>, id: Int) {
    PACKET_IDS[cls] = id
    PACKETS[id] = cls
    CTORS[cls] = cls.getDeclaredConstructor()
  }
}