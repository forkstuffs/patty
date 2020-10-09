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
import io.github.portlek.patty.tcp.TcpProtocol;
import io.github.portlek.patty.tcp.TcpServerConnection;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class PattyServer extends Patty {

  public final List<Connection> connections = new ArrayList<>();

  @NotNull
  private final String ip;

  private final int port;

  @Nullable
  private Connection connection;

  private PattyServer(@NotNull final String ip, final int port, @NotNull final Protocol protocol) {
    super(protocol);
    this.ip = ip;
    this.port = port;
  }

  @NotNull
  public static PattyServer tcp(@NotNull final String ip, final int port, @NotNull final PacketHeader packetHeader,
                                @Nullable final PacketEncryptor packetEncryptor, @NotNull final PacketSizer packetSizer,
                                @Nullable final ServerListener serverListener, @Nullable final SessionListener sessionListener) {
    return PattyServer.tcp(ip, port, new TcpProtocol(packetEncryptor, packetSizer, packetHeader, serverListener,
      sessionListener));
  }

  @NotNull
  public static PattyServer tcp(@NotNull final String ip, final int port, @NotNull final TcpProtocol protocol) {
    return new PattyServer(ip, port, protocol);
  }

  public void bind() {
    this.bind(true);
  }

  public void bind(final boolean wait) {
    this.connection = new TcpServerConnection(this, new InetSocketAddress(this.ip, this.port));
    this.connection.connect(wait);
  }

  public void close() {
    this.close(true);
  }

  public void close(final boolean wait) {
    if (this.protocol.getServerListener() != null) {
      this.protocol.getServerListener().serverClosing(this);
    }
    this.connections.stream()
      .filter(Connection::isConnected)
      .forEach(conn ->
        conn.disconnect("Server closed."));
    if (this.connection != null) {
      this.connection.close(wait);
    }
  }
}