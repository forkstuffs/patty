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

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

public final class TestRunner {

  public static void main(final String[] args) throws Exception {
    final KeyGenerator gen = KeyGenerator.getInstance("AES");
    gen.init(128);
    final SecretKey key = gen.generateKey();
    Packets.registerAll();
    PattyServer.tcp("127.0.0.1", 25565,
      new TestPacketHeader(),
      new TestPacketEncryptor(key),
      new TestPacketSizer(),
      new TestServerListener(),
      new TestServerConnectionListener())
      .bind();
    PattyClient.tcp("127.0.0.1", 25565,
      new TestPacketHeader(),
      new TestPacketEncryptor(key),
      new TestPacketSizer(),
      new TestClientConnectionListener())
      .connect();
  }
}
