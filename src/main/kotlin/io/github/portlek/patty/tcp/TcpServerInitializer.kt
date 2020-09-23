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

import io.github.portlek.patty.PattyServer
import io.github.portlek.patty.Protocol
import io.github.portlek.patty.packet.ConnectionBound
import io.github.portlek.patty.tcp.pipline.TcpPacketCodec
import io.github.portlek.patty.tcp.pipline.TcpPacketEncryptor
import io.github.portlek.patty.tcp.pipline.TcpPacketManager
import io.github.portlek.patty.tcp.pipline.TcpPacketSizer
import io.netty.buffer.ByteBuf
import io.netty.channel.Channel
import io.netty.channel.ChannelInitializer
import io.netty.channel.ServerChannel
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.handler.timeout.WriteTimeoutHandler

class TcpServerInitializer(
  private val server: PattyServer<ByteBuf>,
  private val protocol: Protocol<ByteBuf>
) : ChannelInitializer<ServerChannel>() {
  override fun initChannel(channel: ServerChannel) {
    val address = channel.remoteAddress()
    refreshReadTimeoutHandler(channel)
    refreshWriteTimeoutHandler(channel)
    channel.pipeline()
      .addLast("encryptor", TcpPacketEncryptor(protocol))
      .addLast("sizer", TcpPacketSizer(protocol))
      .addLast("codec", TcpPacketCodec(protocol, ConnectionBound.SERVER))
      .addLast("manager", TcpPacketManager(protocol))
  }

  private fun refreshReadTimeoutHandler(channel: Channel) {
    if (readTimeout <= 0) {
      if (channel.pipeline()["readTimeout"] != null) {
        channel.pipeline().remove("readTimeout")
      }
    } else {
      if (channel.pipeline()["readTimeout"] == null) {
        channel.pipeline().addFirst("readTimeout", ReadTimeoutHandler(readTimeout))
      } else {
        channel.pipeline().replace("readTimeout", "readTimeout", ReadTimeoutHandler(readTimeout))
      }
    }
  }

  private fun refreshWriteTimeoutHandler(channel: Channel) {
    if (writeTimeout <= 0) {
      if (channel.pipeline()["writeTimeout"] != null) {
        channel.pipeline().remove("writeTimeout")
      }
    } else {
      if (channel.pipeline()["writeTimeout"] == null) {
        channel.pipeline().addFirst("writeTimeout", WriteTimeoutHandler(writeTimeout))
      } else {
        channel.pipeline().replace("writeTimeout", "writeTimeout", WriteTimeoutHandler(writeTimeout))
      }
    }
  }

}