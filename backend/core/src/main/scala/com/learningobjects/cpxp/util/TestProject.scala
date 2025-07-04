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

/** Class that allows an integration or db test to know the project that it is running in. This allows it, for example,
  * to run against a dedicated integration test domain.
  *
  * See project/TestProjectPlugin.scala which invokes this.
  */
object TestProject:

  /** The name of the project being currently tested, if known. */
  var name: String = null

  /** Set the name of the project being tested. */
  @SuppressWarnings(Array("unused")) // invoked reflectively by plugin
  def setName(n: String): Unit = name = n
