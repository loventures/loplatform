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
import java.nio.file.{Files, Path}

final class PathFileInfo(val path: Path) extends FileInfo(() => ()):
  override def openInputStream(): InputStream = Files.newInputStream(path)

  override def exists(): Boolean = Files.exists(path)

  override protected def getActualSize: Long = Files.size(path)

  override protected def getActualMtime: Long = Files.getLastModifiedTime(path).toMillis

  override def toString = s"PathFileInfo($path)"
end PathFileInfo
