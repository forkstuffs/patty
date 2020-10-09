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

import java.security.Key
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec

class TestPacketEncryptor(key: Key) : PacketEncryptor {
    private var inCipher = Cipher.getInstance("AES/CFB8/NoPadding")
    private var outCipher = Cipher.getInstance("AES/CFB8/NoPadding")

    init {
        inCipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(key.encoded))
        outCipher.init(Cipher.ENCRYPT_MODE, key, IvParameterSpec(key.encoded))
    }

    override fun getDecryptOutputSize(length: Int) = inCipher.getOutputSize(length)

    override fun getEncryptOutputSize(length: Int) = outCipher.getOutputSize(length)

    override fun decrypt(input: ByteArray, inputOffset: Int, inputLength: Int, output: ByteArray, outputOffset: Int) =
            inCipher.update(input, inputOffset, inputLength, output, outputOffset)

    override fun encrypt(input: ByteArray, inputOffset: Int, inputLength: Int, output: ByteArray, outputOffset: Int) =
            outCipher.update(input, inputOffset, inputLength, output, outputOffset)
}