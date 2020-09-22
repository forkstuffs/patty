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

package io.github.portlek.patty.util

import java.io.PrintStream
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.ThreadFactory
import java.util.logging.Logger

class PoolSpec(
  private val logger: Logger,
  private val name: String
) : ThreadFactory, ForkJoinPool.ForkJoinWorkerThreadFactory, Thread.UncaughtExceptionHandler {
  companion object {
    val UNCAUGHT_FACTORY = PoolSpec("PATTY")
  }

  constructor(name: String) : this(Logger.getLogger("PoolSpec"), name)

  override fun newThread(r: Runnable): Thread = Thread(r, name).also { it.uncaughtExceptionHandler = this }

  override fun newThread(pool: ForkJoinPool?) = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool).also {
    it.name = name + " - " + it.poolIndex
    it.uncaughtExceptionHandler = this
  }

  override fun uncaughtException(t: Thread?, e: Throwable) {
    e.printStackTrace(object : PrintStream(System.out) {
      override fun println(x: Any) {
        logger.warning(x.toString())
      }
    })
  }
}