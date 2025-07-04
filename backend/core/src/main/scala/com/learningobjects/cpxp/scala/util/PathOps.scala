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

package com.learningobjects.cpxp.scala.util

import java.nio.file.*

/** Adds "useful" operations to [[Path]] s.
  */
final class PathOps(private val self: Path) extends AnyVal:

  /** Does the file or directory denoted by this [[Path]] exist?
    */
  @inline def exists: Boolean = Files.exists(self)

object PathOps extends ToPathOps

trait ToPathOps:
  import language.implicitConversions

  @inline
  implicit final def ToPathOps(path: Path): PathOps = new PathOps(path)
