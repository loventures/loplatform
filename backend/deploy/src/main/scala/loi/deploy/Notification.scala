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

package loi.deploy

import java.lang.management.ManagementFactory

import com.learningobjects.cpxp.BaseServiceMeta

import scala.util.Random
import scalaz.\/

object Notification:
  private final val logger = org.log4s.getLogger

  def notifySuccess(): Unit =
    val sm = BaseServiceMeta.getServiceMeta
    logger.info(
      s"CPXP ready, ${sm.getVersion}, ${sm.getBuild}, ${sm.getBranch}, ${sm.getRevision}, ${sm.getBuildDate}, ${sm.getBuildNumber} after ${uptime}ms"
    )
    if sm.isLocal then
      println(fansi.Color.Green("CPXP ready"))
      notifyDesktop("CPXP ready", durationString(SuccessAdjectives), "Purr")

  def notifyFailure(): Unit =
    val sm = BaseServiceMeta.getServiceMeta
    if sm.isLocal then
      println(fansi.Color.Red("CPXP failed"))
      notifyDesktop("CPXP failed", durationString(FailureAdjectives), "Basso")

  private def notifyDesktop(subtitle: String, message: String, sound: String): Unit =
    if "Mac OS X" == System.getProperty("os.name") then
      \/.attempt {
        Runtime.getRuntime.exec(
          Array(
            "osascript",
            "-e",
            s"""display notification "$message" with title "DE Nyancat" subtitle "$subtitle" sound name "$sound""""
          )
        )
      } { e =>
        logger.warn(e)("Notification error")
      }

  private def durationString(adjectives: List[String]) =
    s"After ${Random.shuffle(adjectives).head} ${uptime / 1000} seconds."

  private lazy val uptime = ManagementFactory.getRuntimeMXBean.getUptime

  private val CommonAdjectives = List(
    "but",
    "a paltry",
    "merely",
    "a trifling",
    "just",
    "an inconsequential",
    "a fleeting",
    "a modest",
    "an inconspicuous",
    "an imperceptible",
    "barely",
    "a fugacious",
    "an evanescent",
    "an infinitesimal",
    "a negligible",
    "a shake of a lamb's tail, viz.",
    "about as long as it takes you to say",
    "all of"
  )

  private val SuccessAdjectives = "a glorious" :: "an auspicious" :: CommonAdjectives

  private val FailureAdjectives = "a mendacious" :: "an ill-fated" :: CommonAdjectives
end Notification
