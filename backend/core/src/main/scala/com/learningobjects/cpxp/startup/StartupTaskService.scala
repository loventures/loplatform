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

package com.learningobjects.cpxp.startup

import com.learningobjects.cpxp.component.annotation.Service
import scalaz.\/

/** The startup task service is responsible for running upgrade and data bootstrapping startup tasks during system
  * upgrade and domain creation operations.
  */
@Service
trait StartupTaskService:

  /** Runs all startup scripts.
    */
  def startup(): Unit

  /** Run the startup scripts for a domain, post creation.
    *
    * @param id
    *   the domain id
    * @return
    *   either the startup failed or whether any task ran
    */
  def startupDomain(id: Long): Throwable \/ Boolean

  /** Returns whether the last startup succeeded.
    */
  def lastStartupSucceeded: Boolean
end StartupTaskService
