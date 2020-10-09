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

import io.github.portlek.patty.tcp.PacketEncryptor;
import io.github.portlek.patty.tcp.TcpClientConnection;
import io.github.portlek.patty.tcp.TcpProtocol;
import java.net.InetSocketAddress;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class PattyClient extends Patty {

  @NotNull
  private final String ip;

  private final int port;

  private PattyClient(@NotNull final String ip, final int port, @NotNull final Protocol protocol) {
    super(protocol);
    this.ip = ip;
    this.port = port;
  }

  @NotNull
  public static PattyClient tcp(@NotNull final String ip, final int port, @NotNull final PacketHeader packetHeader,
                                @Nullable final PacketEncryptor packetEncryptor, @NotNull final PacketSizer packetSizer,
                                @Nullable final ConnectionListener connectionListener) {
    return PattyClient.tcp(ip, port, new TcpProtocol(packetEncryptor, packetSizer, packetHeader, null, connectionListener));
  }

  @NotNull
  public static PattyClient tcp(@NotNull final String ip, final int port, @NotNull final TcpProtocol protocol) {
    return new PattyClient(ip, port, protocol);
  }

  public void connect() {
    this.connect(true);
  }

  public void connect(final boolean wait) {
    new TcpClientConnection(this, new InetSocketAddress(this.ip, this.port))
      .connect(wait);
  }
}
