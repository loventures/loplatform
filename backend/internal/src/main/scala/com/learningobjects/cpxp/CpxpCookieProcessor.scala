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

package com.learningobjects.cpxp

import java.text.{FieldPosition, SimpleDateFormat}
import java.util.{Date, Locale, TimeZone}

import jakarta.servlet.http.{Cookie, HttpServletRequest}
import org.apache.http.HttpHeaders
import org.apache.tomcat.util.http.Rfc6265CookieProcessor
import scalaz.std.string.*
import scalaz.syntax.std.boolean.*
import scaloi.syntax.any.*
import scaloi.syntax.date.*
import scaloi.syntax.option.*
import scaloi.syntax.regex.*

import _root_.scala.concurrent.duration.*

class CpxpCookieProcessor extends Rfc6265CookieProcessor:
  // Shamelessly stolen from Rfc6265CookieProcessor which indiscriminately sends the same-site attribute to all
  // and sundry which will plausibly break incompatible browsers.
  override def generateHeader(cookie: Cookie, request: HttpServletRequest): String =
    val header    = new StringBuffer
    header.append(cookie.getName)
    header.append('=')
    Option(cookie.getValue) foreach header.append
    val maxAge    = cookie.getMaxAge
    if maxAge > -1 then
      header.append(s"; Max-Age=$maxAge; Expires=")
      if maxAge == 0 then header.append(CpxpCookieProcessor.DateAncienne)
      else
        CpxpCookieProcessor.DateFormat.get
          .format(new Date + maxAge.seconds, header, new FieldPosition(0))
    val useragent = Option(request.getHeader(HttpHeaders.USER_AGENT)).getOrElse("An all around swell browser")
    OptionNZ(cookie.getDomain).foreach(d => header.append(s"; Domain=$d"))
    OptionNZ(cookie.getPath).foreach(p => header.append(s"; Path=$p"))
    header.append(cookie.getSecure ?? "; Secure")
    header.append(cookie.isHttpOnly ?? "; HttpOnly")
    header.append((cookie.getSecure && SameSiteNoneStuff.shouldSendSameSiteNone(useragent)) ?? "; SameSite=None")
    header.toString
  end generateHeader
end CpxpCookieProcessor

// Shamelessly stolen from CookieProcessorBase which scala thinks we can't access
object CpxpCookieProcessor:
  final val DatePattern = "EEE, dd-MMM-yyyy HH:mm:ss z"

  final val DateFormat = ThreadLocal.withInitial[SimpleDateFormat] { () =>
    new SimpleDateFormat(DatePattern, Locale.US) <| { df =>
      df.setTimeZone(TimeZone.getTimeZone("GMT"))
    }
  }

  final val DateAncienne = DateFormat.get.format(new Date(10000L))
end CpxpCookieProcessor

// Don’t send `SameSite=None` to known incompatible clients.
// See: https://www.chromium.org/updates/same-site/incompatible-clients
object SameSiteNoneStuff:

  def shouldSendSameSiteNone(useragent: String): Boolean =
    !isSameSiteNoneIncompatible(useragent)

  // Classes of browsers known to be incompatible.

  def isSameSiteNoneIncompatible(useragent: String): Boolean =
    hasWebKitSameSiteBug(useragent) || dropsUnrecognizedSameSiteCookies(useragent)

  def hasWebKitSameSiteBug(useragent: String): Boolean =
    isIosVersion(major = 12, useragent) ||
      (isMacosxVersion(major = 10, minor = 14, useragent) &&
        (isSafari(useragent) || isMacEmbeddedBrowser(useragent)))

  def dropsUnrecognizedSameSiteCookies(useragent: String): Boolean =
    if isUcBrowser(useragent) then !isUcBrowserVersionAtLeast(major = 12, minor = 13, build = 2, useragent)
    else
      isChromiumBased(useragent) &&
      isChromiumVersionAtLeast(major = 51, useragent) &&
      !isChromiumVersionAtLeast(major = 67, useragent)

  final val regex0 = """\(iP.+; CPU .*OS (\d+)[_\d]*.*\) AppleWebKit\/""".r

  def isIosVersion(major: Int, useragent: String): Boolean =
    regex0 findFirstMatchIn useragent exists { m =>
      m.group(1) == major.toString
    }

  final val regex1 = """\(Macintosh;.*Mac OS X (\d+)_(\d+)[_\d]*.*\) AppleWebKit\/""".r

  def isMacosxVersion(major: Int, minor: Int, useragent: String): Boolean =
    regex1 findFirstMatchIn useragent exists { m =>
      (m.group(1) == major.toString) && (m.group(2) == minor.toString)
    }

  final val regex2 = """Version\/.* Safari\/""".r

  def isSafari(useragent: String): Boolean =
    (regex2 `test` useragent) && !isChromiumBased(useragent)

  final val regex3 =
    """^Mozilla\/[\.\d]+ \(Macintosh;.*Mac OS X [_\d]+\) AppleWebKit\/[\.\d]+ \(KHTML, like Gecko\)$""".r

  def isMacEmbeddedBrowser(useragent: String): Boolean =
    regex3 `test` useragent

  final val regex4 = """Chrom(e|ium)""".r

  def isChromiumBased(useragent: String): Boolean =
    regex4 `test` useragent

  final val regex5 = """Chrom[^ \/]+\/(\d+)[\.\d]* """.r

  def isChromiumVersionAtLeast(major: Int, useragent: String): Boolean =
    regex5 findFirstMatchIn useragent exists { m =>
      m.group(1).toInt >= major
    }

  final val regex6 = """UCBrowser\/""".r

  def isUcBrowser(useragent: String): Boolean =
    regex6 `test` useragent

  final val regex7 = """UCBrowser\/(\d+)\.(\d+)\.(\d+)[\.\d]* """.r

  def isUcBrowserVersionAtLeast(major: Int, minor: Int, build: Int, useragent: String): Boolean =
    regex7 findFirstMatchIn useragent exists { m =>
      val major_version = m.group(1).toInt
      val minor_version = m.group(2).toInt
      val build_version = m.group(3).toInt
      if major_version != major then major_version > major
      else if minor_version != minor then minor_version > minor
      else build_version >= build
    }
end SameSiteNoneStuff
