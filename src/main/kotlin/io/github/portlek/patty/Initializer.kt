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

import io.netty.channel.Channel
import io.netty.channel.ChannelInitializer
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.handler.timeout.WriteTimeoutHandler

abstract class Initializer<C : Channel> : ChannelInitializer<C>() {
  private val readTimeout = 30
  private val writeTimeout = 0

  protected fun refreshReadTimeoutHandler(channel: Channel) {
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

  protected fun refreshWriteTimeoutHandler(channel: Channel) {
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