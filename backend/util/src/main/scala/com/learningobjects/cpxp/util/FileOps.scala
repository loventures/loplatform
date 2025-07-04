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

import kantan.csv.*
import kantan.csv.ops.*

import java.io.{File, FileWriter}
import java.nio.charset.StandardCharsets.UTF_8
import scala.language.implicitConversions
import scala.util.Using

final class FileOps(private val file: File) extends AnyVal:
  def writeCsvWithBom[A: HeaderEncoder](writerF: CsvWriter[A] => Unit): Unit =
    Using.resource(new FileWriter(file, UTF_8)) { writer =>
      writer.write(FileOps.BOM)
      val csv = writer.asCsvWriter[A](rfc.withHeader)
      writerF(csv)
    }

object FileOps:
  private final val BOM = '\ufeff'

  implicit def toFileOps(file: File): FileOps = new FileOps(file)
