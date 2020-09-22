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

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class PattyServer {

    @NotNull
    private final String host;

    private final int port;

    @NotNull
    private Consumer<PattyServer> whenServerBound = pattyServer -> {
    };

    @NotNull
    private Consumer<PattyServer> whenServerClosing = pattyServer -> {
    };

    @NotNull
    private Consumer<PattyServer> whenSessionClosed = pattyServer -> {
    };

    @NotNull
    private BiConsumer<PattyServer, Session> whenSessionAdded = (pattyServer, session) -> {
    };

    @NotNull
    private BiConsumer<PattyServer, Session> whenSessionRemoved = (pattyServer, session) -> {
    };

    public static PattyServer tcp(@NotNull final String host, final int port) {
        return new PattyServer(host, port);
    }

    public static PattyServer udp(@NotNull final String host, final int port) {
        return new PattyServer(host, port);
    }

    public PattyServer whenServerBound(@NotNull final Consumer<PattyServer> whenServerBound) {
        this.whenServerBound = whenServerBound;
        return this;
    }

    public PattyServer whenServerClosing(@NotNull final Consumer<PattyServer> whenServerClosing) {
        this.whenServerClosing = whenServerClosing;
        return this;
    }

    public PattyServer whenServerClosed(@NotNull final Consumer<PattyServer> whenSessionClosed) {
        this.whenSessionClosed = whenSessionClosed;
        return this;
    }

    public PattyServer whenSessionAdded(@NotNull final BiConsumer<PattyServer, Session> whenSessionAdded) {
        this.whenSessionAdded = whenSessionAdded;
        return this;
    }

    public PattyServer whenSessionRemoved(@NotNull final BiConsumer<PattyServer, Session> whenSessionRemoved) {
        this.whenSessionRemoved = whenSessionRemoved;
        return this;
    }

    public void bind() {
        this.bind(true);
    }

    public void bind(final boolean wait) {

    }

}
