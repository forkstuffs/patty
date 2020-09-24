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

import io.github.portlek.patty.tcp.TcpProtocol
import io.github.portlek.patty.tcp.TcpServerConnection
import java.net.InetSocketAddress
import java.util.*

class PattyServer private constructor(
  private val ip: String,
  private val port: Int,
  protocol: Protocol
) : Patty(protocol) {
  private var connection: Connection? = null
  val connections = ArrayList<Connection>()

  fun bind(wait: Boolean = true) {
    connection = TcpServerConnection(this, InetSocketAddress(ip, port)).also { it.connect(wait) }
  }

  fun close(wait: Boolean = true) {
    protocol.serverListener?.also {
      it.serverClosing(this)
    }
    connections
      .filter { it.isConnected() }
      .forEach {
        it.disconnect("Server closed.")
      }
    connection?.close(wait)
  }

  companion object {
    fun tcp(ip: String, port: Int, packetHeader: PacketHeader, packetEncryptor: PacketEncryptor? = null,
            packetSizer: PacketSizer, serverListener: ServerListener? = null,
            sessionListener: SessionListener? = null) =
      PattyServer(ip, port, TcpProtocol(packetHeader, packetEncryptor, packetSizer, serverListener,
        sessionListener))

    fun tcp(ip: String, port: Int, protocol: TcpProtocol) = PattyServer(ip, port, protocol)
  }
}