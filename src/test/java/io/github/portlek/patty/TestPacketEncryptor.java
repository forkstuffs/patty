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
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;
import org.jetbrains.annotations.NotNull;

public final class TestPacketEncryptor implements PacketEncryptor {

  @NotNull
  private final Key key;

  @NotNull
  private final Cipher inCipher;

  @NotNull
  private final Cipher outCipher;

  public TestPacketEncryptor(@NotNull final Key key) throws NoSuchPaddingException, NoSuchAlgorithmException,
    InvalidAlgorithmParameterException, InvalidKeyException {
    this.key = key;
      this.inCipher = Cipher.getInstance("AES/CFB8/NoPadding");
      this.outCipher = Cipher.getInstance("AES/CFB8/NoPadding");
      this.inCipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(key.getEncoded()));
      this.outCipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(key.getEncoded()));
  }

  @Override
  public int getDecryptOutputSize(final int length) {
    return this.inCipher.getOutputSize(length);
  }

  @Override
  public int getEncryptOutputSize(final int length) {
    return this.outCipher.getOutputSize(length);
  }

  @Override
  public int decrypt(final byte[] input, final int inputOffset, final int inputLength, final byte[] output, final int outputOffset) {
    try {
      return this.inCipher.update(input, inputOffset, inputLength, output, outputOffset);
    } catch (final ShortBufferException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public int encrypt(final byte[] input, final int inputOffset, final int inputLength, final byte[] output, final int outputOffset) {
    try {
      return this.outCipher.update(input, inputOffset, inputLength, output, outputOffset);
    } catch (final ShortBufferException e) {
      throw new RuntimeException(e);
    }
  }
}