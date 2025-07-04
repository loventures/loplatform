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

package de.tomcat.internal

import java.util.logging.{Level, Logger}

import de.tomcat.juli.DeLogManager
import org.apache.catalina.startup.Tomcat

import scala.util.control.NonFatal

class ShutdownHook(tomcat: Tomcat, console: Logger) extends Thread:
  override def run(): Unit =
    try
      console.info("Shutdown command received.")
      tomcat.stop()
      tomcat.destroy()
      console.info("Tomcat stopped.")
    catch
      case NonFatal(e) =>
        console.log(Level.WARNING, "Error stopping tomcat", e)
    finally DeLogManager.shutdown()
end ShutdownHook
