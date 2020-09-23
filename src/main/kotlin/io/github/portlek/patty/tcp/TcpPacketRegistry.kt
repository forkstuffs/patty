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

package io.github.portlek.patty.tcp

import io.github.portlek.patty.ConnectionState
import io.github.portlek.patty.packet.ConnectionBound
import java.lang.reflect.Constructor
import java.util.*

object TcpPacketRegistry {
  private val CTORS = HashMap<Class<out TcpPacket>, Constructor<out TcpPacket>>()
  private val PACKET_IDS = HashMap<Class<out TcpPacket>, Int>()
  private val PACKETS = HashMap<Int, Class<out TcpPacket>>()

  fun <T : TcpPacket> createPacket(cls: Class<out TcpPacket>) = CTORS[cls]?.newInstance() as T

  fun getPacket(state: ConnectionState, bound: ConnectionBound, id: Int) = PACKETS[shift(state, bound, id)]

  fun getPacketId(cls: Class<out TcpPacket>): Int {
    val identifier = PACKET_IDS.getOrDefault(cls, -1)
    if (identifier != -1) {
      return identifier
    }
    throw IllegalArgumentException(cls.simpleName + " is not registered")
  }

  fun getPacketId(info: Int) = info and 0x7ffffff

  fun getPacketState(info: Int) = ConnectionState.values()[info shl 27 and 0xf]

  fun getPacketBound(info: Int) = ConnectionBound.values()[info shl 31 and 0x1]

  fun register(cls: Class<out TcpPacket>, state: ConnectionState, bound: ConnectionBound, id: Int) {
    val identifier = shift(state, bound, id)
    PACKET_IDS[cls] = identifier
    if (bound == ConnectionBound.SERVER) {
      PACKETS[identifier] = cls
      CTORS[cls] = cls.getDeclaredConstructor()
    }
  }

  private fun shift(state: ConnectionState, bound: ConnectionBound, id: Int): Int {
    var identifier = id
    identifier = identifier or (state.ordinal shl 27)
    identifier = identifier or (bound.ordinal shl 31)
    return identifier
  }
}