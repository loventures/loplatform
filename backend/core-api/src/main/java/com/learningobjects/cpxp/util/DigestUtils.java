/*
 * LO Platform copyright (C) 2007–2025 LO Ventures LLC.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.learningobjects.cpxp.util;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.Key;

public class DigestUtils extends org.apache.commons.codec.digest.DigestUtils {
    /** The SHA-1 digest algorithm name. */
    public static final String MESSAGE_DIGEST_SHA_1 = "SHA-1";

    /** The SHA-1 secure random algorithm name. */
    public static final String SECURE_RANDOM_SHA_1 = "SHA1PRNG";

    /**
     * Encode a byte array to a hex string.
     *
     * @param data the data
     *
     * @return the hex string
     */
    public static String toHexString(byte[] data) {
        return new String(Hex.encodeHex(data));
    }

    /**
     * Decode a hex string to a byte array.
     *
     * @param hex the hex string
     *
     * @return the data
     */
    public static byte[] fromHexString(String hex) {
        try {
            return Hex.decodeHex(hex.toCharArray());
        } catch (DecoderException ex) {
            throw new NumberFormatException("Invalid hex: " + hex);
        }
    }

    public static String aes128Encrypt(String data, String passphrase) throws Exception {
        byte[] keyBytes = md5(passphrase.getBytes("UTF-8"));
        Key key = new SecretKeySpec(keyBytes, "AES");
        byte[] ivBytes = NumberUtils.getNonce(16);
        IvParameterSpec iv = new IvParameterSpec(ivBytes);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key, iv);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(ivBytes);
        CipherOutputStream cout = new CipherOutputStream(out, cipher);
        cout.write(data.getBytes("UTF-8"));
        cout.close();

        return new String(Base64.encodeBase64(out.toByteArray()));
    }

    public static String aes128Decrypt(String data, String passphrase) throws Exception {
        ByteArrayInputStream in = new ByteArrayInputStream(Base64.decodeBase64(data));
        byte[] ivBytes = IOUtils.toByteArray(in, 16);
        IvParameterSpec iv = new IvParameterSpec(ivBytes);
        byte[] keyBytes = md5(passphrase.getBytes("UTF-8"));
        Key key = new SecretKeySpec(keyBytes, "AES");

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, key, iv);
        CipherInputStream cin = new CipherInputStream(in, cipher);
        byte[] decrypted = IOUtils.toByteArray(cin);

        return new String(decrypted, "UTF-8");
    }
}
