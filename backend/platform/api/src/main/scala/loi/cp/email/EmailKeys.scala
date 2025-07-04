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

package loi.cp.email

import java.math.BigInteger
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

import com.google.common.primitives.Longs
import com.learningobjects.cpxp.BaseServiceMeta
import com.learningobjects.cpxp.dto.{Facade, FacadeData, FacadeItem}
import com.learningobjects.cpxp.scala.cpxp.Service.*
import scaloi.syntax.AnyOps.*
import com.learningobjects.cpxp.service.domain.DomainConstants
import com.learningobjects.cpxp.service.email.EmailConstants
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.overlord.OverlordWebService
import com.learningobjects.cpxp.util.{DigestUtils, NumberUtils}

import scala.util.Try
import scalaz.syntax.std.`boolean`.*

/** Provides support for obfuscating and deobfuscating email keys. Even in the absence of obfuscation the reply
  * framework is fairly safe in that it only supports a limited set of reactions.
  */
object EmailKeys:

  /** AES key type. */
  val AES = "AES"

  /** AES encryption algorithm. */
  val AES_ECB_NoPadding = "AES/ECB/NoPadding"

  /** Obfuscate two long values.
    * @param v0
    *   the first value to obfuscate
    * @param v1
    *   the second value to obfuscate
    * @return
    *   the obfuscated string
    */
  def obfuscate(v0: Long, v1: Long): String =
    encode(Longs.toByteArray(v0) ++ Longs.toByteArray(v1))

  /** Deobfuscate two long values.
    * @param value
    *   the obfuscated string
    * @return
    *   the deobfuscated values
    */
  def deobfuscate(value: String): Try[(Long, Long)] =
    Try(decode(value).grouped(8).toArray map { a =>
      Longs.fromByteArray(a)
    } match
      case Array(v0, v1) => (v0, v1))

  /** Encrypts an array of bytes and converts it to base 36 encoding.
    * @param bytes
    *   the bytes to encode
    * @return
    *   the encoded data
    */
  private def encode(bytes: Array[Byte]): String =
    NumberUtils.toBase36Encoding(new BigInteger(1, cipher(encrypt = true).doFinal(bytes)))

  /** Decodes a base 36 string and decrypts the resulting bytes.
    * @param text
    *   the encoded data
    * @return
    *   the decoded data
    */
  private def decode(text: String): Array[Byte] =
    cipher(encrypt = false).doFinal(getBytes(debase(text), 16))

  // support legacy email keys that used mixed case. we had to drop mixed case
  // because microsoft? is lowercasing our addresses. TODO: Killme in a release.
  private def debase(text: String): BigInteger =
    if text.length <= 22 then NumberUtils.fromBase62Encoding(text)
    else NumberUtils.fromBase36Encoding(text.toLowerCase)

  /** Create an initialized cipher.
    * @param encrypt
    *   whether to initialize in encrypt or decrypt mode
    * @return
    *   the cipher
    */
  private def cipher(encrypt: Boolean): Cipher =
    Cipher.getInstance(AES_ECB_NoPadding) <| { c =>
      c.init(encrypt.fold(Cipher.ENCRYPT_MODE, Cipher.DECRYPT_MODE), new SecretKeySpec(clusterKey, AES))
    }

  /** Gets the least significant bytes of the magnitude encoding of a big integer, padding or truncating as necessary.
    * @param i
    *   the integer to encode
    * @param n
    *   the number of bytes to extract
    * @return
    *   the requested bytes
    */
  private def getBytes(i: BigInteger, n: Int): Array[Byte] =
    val a = i.toByteArray
    val l = a.length
    // the array may be long if a leading sign bit overflows or short if it has leading zeroes
    if l >= n then a.slice(l - n, l) else Array.fill(n - l)(0.toByte) ++ a

  /** The cluster email key.
    */
  lazy val clusterKey: Array[Byte] =
    val overlordId  = service[OverlordWebService].findOverlordDomainId()
    val overlord    = service[FacadeService].getFacade(overlordId, classOf[EmailKeyFacade])
    val prefix      = BaseServiceMeta.getServiceMeta.getCluster.concat(":")
    val overlordKey = Option(overlord.getEmailKey).filter(_.startsWith(prefix)) getOrElse {
      (prefix + DigestUtils.toHexString(NumberUtils.getNonce(16))) <| { key =>
        overlord.setEmailKey(key)
      }
    }
    DigestUtils.fromHexString(overlordKey.stripPrefix(prefix))
  end clusterKey

  @FacadeItem(DomainConstants.ITEM_TYPE_DOMAIN)
  trait EmailKeyFacade extends Facade:
    @FacadeData(value = EmailConstants.DATA_TYPE_EMAIL_KEY)
    def getEmailKey: String
    def setEmailKey(key: String): Unit
end EmailKeys
