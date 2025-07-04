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

package de.tomcat.juli

import java.util.logging.LogManager

/** The default Java log manager shuts down in a JVM shutdown hook. The JVM shutdown hooks are run in parallel. This
  * means that no logging is possible during shutdown.
  *
  * This overrides that behaviour to allow us to log during shutdown.
  */
final class DeLogManager extends LogManager:
  override def reset(): Unit = ()
  def doReset(): Unit        = super.reset()

object DeLogManager:
  def shutdown(): Unit =
    LogManager.getLogManager match
      case deLogManager: DeLogManager => deLogManager.doReset()
