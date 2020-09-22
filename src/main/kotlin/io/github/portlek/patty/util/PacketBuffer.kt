/*
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

package io.github.portlek.patty.util

import io.netty.buffer.ByteBuf
import java.math.BigInteger
import java.net.*
import java.nio.charset.StandardCharsets
import java.util.*

class PacketBuffer {
  var buffer: ByteArray
    private set
  var bufferOffset: Int
    private set
  var position: Int

  constructor(capacity: Int) : this(ByteArray(capacity), 0)
  constructor(buffer: ByteArray, offset: Int) {
    this.buffer = buffer
    bufferOffset = offset
    position = offset
  }

  constructor(datagram: ByteBuf) {
    buffer = ByteArray(datagram.readableBytes())
    datagram.readBytes(buffer)
    datagram.release()
    bufferOffset = 0
    position = 0
  }

  val remaining: Int
    get() = buffer.size - position

  fun resetPosition() {
    position = bufferOffset
  }

  fun readBytes(v: ByteArray) {
    ensureRemaining(v.size)
    System.arraycopy(buffer, position, v, 0, v.size)
    position += v.size
  }

  fun readShort(): Short {
    ensureRemaining(2)
    return (buffer[position++].toInt() and 0xFF shl 8 or buffer[position++].toInt() and 0xFF).toShort()
  }

  fun readLShort(): Short {
    ensureRemaining(2)
    return (buffer[position++].toInt() and 0xFF or (buffer[position++].toInt() and 0xFF) shl 8).toShort()
  }

  fun readFloat(): Float {
    return java.lang.Float.intBitsToFloat(readInt())
  }

  fun readLFloat(): Float {
    return java.lang.Float.intBitsToFloat(readLInt())
  }

  fun readInt(): Int {
    ensureRemaining(4)
    return buffer[position++].toInt() and 0xFF shl 24 or (
      buffer[position++].toInt() and 0xFF shl 16) or (
      buffer[position++].toInt() and 0xFF shl 8) or (
      buffer[position++].toInt() and 0xFF)
  }

  fun readLInt(): Int {
    ensureRemaining(4)
    return buffer[position++].toInt() and 0xFF or (
      buffer[position++].toInt() and 0xFF shl 8) or (
      buffer[position++].toInt() and 0xFF shl 16) or (
      buffer[position++].toInt() and 0xFF shl 24)
  }

  fun readDouble(): Double {
    return java.lang.Double.longBitsToDouble(readLong())
  }

  fun readLong(): Long {
    ensureRemaining(8)
    return buffer[position++].toLong() and 0xFF shl 56 or (
      buffer[position++].toLong() and 0xFF shl 48) or (
      buffer[position++].toLong() and 0xFF shl 40) or (
      buffer[position++].toLong() and 0xFF shl 32) or (
      buffer[position++].toLong() and 0xFF shl 24) or (
      buffer[position++].toLong() and 0xFF shl 16) or (
      buffer[position++].toLong() and 0xFF shl 8) or (
      buffer[position++].toLong() and 0xFF)
  }

  fun readLLong(): Long {
    ensureRemaining(8)
    return buffer[position++].toLong() and 0xFF or (
      buffer[position++].toLong() and 0xFF shl 8) or (
      buffer[position++].toLong() and 0xFF shl 16) or (
      buffer[position++].toLong() and 0xFF shl 24) or (
      buffer[position++].toLong() and 0xFF shl 32) or (
      buffer[position++].toLong() and 0xFF shl 40) or (
      buffer[position++].toLong() and 0xFF shl 48) or (
      buffer[position++].toLong() and 0xFF shl 56)
  }

  fun readString(): String {
    val size = readUnsignedVarInt()
    ensureRemaining(size)
    val value = String(buffer, position, size)
    position += size
    return value
  }

  fun readUShort(): Int {
    ensureRemaining(2)
    return buffer[position++].toInt() and 0xFF shl 8 or buffer[position++].toInt() and 0xFF
  }

  fun readAddress(): InetSocketAddress {
    val ipVersion = readByte()
    return if (ipVersion.toInt() == 4) {
      ensureRemaining(6)
      val complement = readUInt().inv()
      val hostname = (complement shr 24 and 0xFF).toString() + "." +
        (complement shr 16 and 0xFF) + "." +
        (complement shr 8 and 0xFF) + "." +
        (complement and 0xFF)
      val port = readUShort()
      InetSocketAddress.createUnresolved(hostname, port)
    } else {
      // Reading sockaddr_in6 structure whose fields are _always_ in big-endian order!
      readUShort() // Addressinfo
      val port = readUShort()
      readUInt() // Flowinfo (see RFC 6437 - can safely leave it at 0)
      val in6addr = ByteArray(16)
      this.readBytes(in6addr)
      readUInt() // Scope ID
      try {
        InetSocketAddress(Inet6Address.getByAddress(null, in6addr, 0), port)
      } catch (e: UnknownHostException) {
        throw IllegalArgumentException("Could not read sockaddr_in6", e)
      }
    }
  }

  fun readByte(): Byte {
    ensureRemaining(1)
    return (buffer[position++].toInt() and 0xFF) as Byte
  }

  fun readUInt(): Long {
    ensureRemaining(4)
    return buffer[position++].toLong() and 0xFF shl 24 or (
      buffer[position++].toLong() and 0xFF shl 16) or (
      buffer[position++].toLong() and 0xFF shl 8) or (
      buffer[position++].toLong() and 0xFF)
  }

  fun readUUID(): UUID {
    return UUID(readLLong(), readLLong())
  }

  fun readUnsignedVarInt(): Int {
    var out = 0
    var bytes = 0
    var input: Byte
    do {
      input = readByte()
      out = out or (input.toInt() and 0x7F shl bytes++ * 7)
      if (bytes > 6) {
        throw RuntimeException("VarInt too big")
      }
    } while (input.toInt() and 0x80 == 0x80)
    return out
  }

  fun readSignedVarInt(): Int {
    val `val` = readUnsignedVarLong()
    return decodeZigZag32(`val`)
  }

  fun readUnsignedVarLong(): Long {
    var out: Long = 0
    var bytes = 0
    var input: Byte
    do {
      input = readByte()
      out = out or (input.toLong() and 0x7F shl bytes++ * 7)
      if (bytes > 10) {
        throw RuntimeException("VarInt too big")
      }
    } while (input.toInt() and 0x80 == 0x80)
    return out
  }

  fun readSignedVarLong() = this.decodeZigZag64(readVarNumber(10))

  fun writeUnsignedVarLong(valu: Long) {
    var value = valu
    while (value and -128 != 0L) {
      writeByte((value and 127 or 128).toByte())
      value = value ushr 7
    }
    writeByte(value.toByte())
  }

  fun writeSignedVarLong(value: Long) {
    val signedLong = encodeZigZag64(value)
    writeVarBigInteger(signedLong)
  }

  fun writeUnsignedVarInt(value: Int) {
    var value = value
    while (value and -128 != 0) {
      writeByte((value and 127 or 128).toByte())
      value = value ushr 7
    }
    writeByte(value.toByte())
  }

  fun writeSignedVarInt(value: Int) {
    val signedValue = encodeZigZag32(value)
    writeUnsignedVarLong(signedValue)
  }

  fun skip(size: Int) {
    ensureRemaining(size)
    position += size
  }

  fun readBoolean(): Boolean {
    return readByte().toInt() != 0x00
  }

  fun readTriad(): Int {
    ensureRemaining(3)
    return buffer[position++].toInt() and 0xFF or (
      buffer[position++].toInt() and 0xFF shl 8) or (
      buffer[position++].toInt() and 0xFF shl 16)
  }

  fun writeBoolean(v: Boolean) {
    writeByte(if (v) 0x01.toByte() else 0x00.toByte())
  }

  fun writeByte(v: Byte) {
    ensureCapacity(1)
    buffer[position++] = v
  }

  fun writeBytes(v: ByteArray) {
    ensureCapacity(v.size)
    System.arraycopy(v, 0, buffer, position, v.size)
    position += v.size
  }

  fun writeShort(v: Short) {
    ensureCapacity(2)
    buffer[position++] = (v.toInt() shr 8 and 0xFF).toByte()
    buffer[position++] = (v.toInt() and 0xFF).toByte()
  }

  fun writeLShort(v: Short) {
    ensureCapacity(2)
    buffer[position++] = (v.toInt() and 0xFF).toByte()
    buffer[position++] = (v.toInt() shr 8 and 0xFF).toByte()
  }

  fun writeUInt(v: Long) {
    ensureCapacity(4)
    buffer[position++] = (v.toInt() shr 24 and 0xFF).toByte()
    buffer[position++] = (v.toInt() shr 16 and 0xFF).toByte()
    buffer[position++] = (v.toInt() shr 8 and 0xFF).toByte()
    buffer[position++] = (v.toInt() and 0xFF).toByte()
  }

  fun writeFloat(v: Float) {
    writeInt(java.lang.Float.floatToRawIntBits(v))
  }

  fun writeLFloat(v: Float) {
    writeLInt(java.lang.Float.floatToRawIntBits(v))
  }

  fun writeInt(v: Int) {
    ensureCapacity(4)
    buffer[position++] = (v shr 24 and 0xFF).toByte()
    buffer[position++] = (v shr 16 and 0xFF).toByte()
    buffer[position++] = (v shr 8 and 0xFF).toByte()
    buffer[position++] = (v and 0xFF).toByte()
  }

  fun writeLInt(v: Int) {
    ensureCapacity(4)
    buffer[position++] = (v and 0xFF).toByte()
    buffer[position++] = (v shr 8 and 0xFF).toByte()
    buffer[position++] = (v shr 16 and 0xFF).toByte()
    buffer[position++] = (v shr 24 and 0xFF).toByte()
  }

  fun writeDouble(v: Double) {
    writeLong(java.lang.Double.doubleToRawLongBits(v))
  }

  fun writeLong(v: Long) {
    ensureCapacity(8)
    buffer[position++] = (v shr 56 and 0xFF).toByte()
    buffer[position++] = (v shr 48 and 0xFF).toByte()
    buffer[position++] = (v shr 40 and 0xFF).toByte()
    buffer[position++] = (v shr 32 and 0xFF).toByte()
    buffer[position++] = (v shr 24 and 0xFF).toByte()
    buffer[position++] = (v shr 16 and 0xFF).toByte()
    buffer[position++] = (v shr 8 and 0xFF).toByte()
    buffer[position++] = (v and 0xFF).toByte()
  }

  fun writeLLong(v: Long) {
    ensureCapacity(8)
    buffer[position++] = (v and 0xFF).toByte()
    buffer[position++] = (v shr 8 and 0xFF).toByte()
    buffer[position++] = (v shr 16 and 0xFF).toByte()
    buffer[position++] = (v shr 24 and 0xFF).toByte()
    buffer[position++] = (v shr 32 and 0xFF).toByte()
    buffer[position++] = (v shr 40 and 0xFF).toByte()
    buffer[position++] = (v shr 48 and 0xFF).toByte()
    buffer[position++] = (v shr 56 and 0xFF).toByte()
  }

  fun writeString(v: String) {
    val ascii = v.toByteArray(StandardCharsets.UTF_8)
    writeUnsignedVarInt(ascii.size)
    ensureCapacity(ascii.size)
    System.arraycopy(ascii, 0, buffer, position, ascii.size)
    position += ascii.size
  }

  fun writeUShort(v: Int) {
    ensureCapacity(2)
    buffer[position++] = (v shr 8 and 0xFF).toByte()
    buffer[position++] = (v and 0xFF).toByte()
  }

  fun writeAddress(address: SocketAddress?) {
    require(address is InetSocketAddress) { "Unknown socket address family (only AF_INET and AF_INET6 supported)" }
    val addr = address
    if (addr.address is Inet4Address) {
      ensureCapacity(7)
      writeByte(4.toByte())
      val inet = addr.address as Inet4Address
      val bytes = inet.address
      var complement = bytes[0].toLong() shl 24 or (
        bytes[1].toLong() shl 16) or (
        bytes[2].toLong() shl 8) or
        bytes[3].toLong()
      complement = complement.inv()
      writeUInt(complement)
      writeUShort(addr.port)
    } else if (addr.address is Inet6Address) {
      val in6addr = addr.address as Inet6Address
      ensureCapacity(25)
      writeByte(6.toByte())
      writeUShort(afInet6.toInt())
      writeUShort(addr.port.toInt())
      writeUInt(0L)
      this.writeBytes(in6addr.address)
      writeUInt(0L)
    }
  }

  fun writeUniqueId(uuid: UUID) {
    writeLLong(uuid.mostSignificantBits)
    writeLLong(uuid.leastSignificantBits)
  }

  /**
   * Writes three integer.
   *
   * @param int integer to write
   */
  fun writeTriad(int: Int) {
    ensureCapacity(3)
    buffer[position] = (int and 0xFF).toByte()
    position++
    buffer[position] = (int shr 8 and 0xFF).toByte()
    position++
    buffer[position] = (int shr 16 and 0xFF).toByte()
    position++
  }

  private fun ensureRemaining(remaining: Int) {
    require(position + remaining <= buffer.size) { "Cannot read more bytes than are available" }
  }

  private fun readVarNumber(size: Int): BigInteger {
    var result = BigInteger.ZERO
    var offset = 0
    var int: Int
    do {
      require(offset < size) { "Var Number too big" }
      int = readByte().toInt()
      result = result.or(BigInteger.valueOf(int.toLong() and 0x7f shl (offset * 7.toLong()).toInt()))
      offset++
    } while (int and 0x80 > 0)
    return result
  }

  private fun encodeZigZag32(int: Int): Long {
    // Note:  the right-shift must be arithmetic
    return (int shl 1 xor int shr 31).toLong()
  }

  private fun decodeZigZag32(lng: Long) = (lng shr 1).toInt() xor (-(lng and 1)).toInt()

  private fun encodeZigZag64(lng: Long): BigInteger {
    val origin = BigInteger.valueOf(lng)
    val left = origin.shiftLeft(1)
    val right = origin.shiftRight(63)
    return left.xor(right)
  }

  private fun decodeZigZag64(lng: Long) = this.decodeZigZag64(BigInteger.valueOf(lng).and(unsignedLongMaxValue))

  private fun decodeZigZag64(big: BigInteger): BigInteger {
    val left = big.shiftRight(1)
    val right = big.and(BigInteger.ONE).negate()
    return left.xor(right)
  }

  private fun writeVarBigInteger(value: BigInteger) {
    var value = value
    require(value <= unsignedLongMaxValue) { "The value is too big" }
    value = value.and(unsignedLongMaxValue)
    val i = BigInteger.valueOf(-128)
    val bigIntX7 = BigInteger.valueOf(0x7f)
    val bigIntX8 = BigInteger.valueOf(0x80)
    while (value.and(i) != BigInteger.ZERO) {
      writeByte(value
        .and(bigIntX7)
        .or(bigIntX8)
        .toByte())
      value = value.shiftRight(7)
    }
    writeByte(value.toByte())
  }

  private fun ensureCapacity(capacity: Int) {
    while (position + capacity > buffer.size) {
      reallocate(capacity)
    }
  }

  private fun reallocate(extra: Int) {
    val nextBuffer = ByteArray(2 * buffer.size)
    System.arraycopy(buffer, bufferOffset, nextBuffer, 0, buffer.size - bufferOffset)
    buffer = nextBuffer
    position -= bufferOffset
    bufferOffset = 0
  }

  companion object {
    /**
     * Javadoc.
     */
    private val unsignedLongMaxValue = BigInteger("FFFFFFFFFFFFFFFF", 16)

    /**
     * Javadoc.
     */
    private val afInet6 = (if (System.getProperty("os.name") == "windows") 23 else 10).toShort()
  }
}