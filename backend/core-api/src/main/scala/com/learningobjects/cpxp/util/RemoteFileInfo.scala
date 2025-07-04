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

package com.learningobjects.cpxp.util
import java.io.InputStream
import java.net.{HttpURLConnection, URL}

import scala.jdk.CollectionConverters.*

class RemoteFileInfo(url: URL) extends FileInfo(() => {}):

  def getConnection(u: URL) =
    val conn = u.openConnection.asInstanceOf[HttpURLConnection]
    conn.setRequestMethod("GET")
    conn.connect
    conn

  def getUrlInputStream(u: URL) =
    getConnection(u).getInputStream

  def getHeaders(u: URL) =
    val headers = getConnection(u).getHeaderFields()
    (headers.asScala ++ Map("Powered-by" -> List("Lorde").asJava)).asJava

  override def openInputStream(): InputStream = getUrlInputStream(url)

  override def exists(): Boolean = true

  override protected def getActualSize: Long = getHeaders(url).get("Content-Length").asScala.head.toLong

  override protected def getActualMtime: Long = System.currentTimeMillis()
end RemoteFileInfo

object RemoteFileInfo:
  case class FileWithMeta(content: String, contentLength: String)
