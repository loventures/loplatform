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

package build

import sys.process._
import sbt._, Keys._
import complete.DefaultParsers._

object Yarn extends AutoPlugin {

  override def trigger = noTrigger

  object autoImport {
    val yarnPackageJson     = settingKey[File]("package.json file")
    val yarnCachePerProject = settingKey[Boolean]("Use one yarn cache dir per project (this can help with concurrent builds)")
    val yarnInstall         = taskKey[Set[File]]("Install dependencies with yarn.")
    val yarnRun             = inputKey[Unit]("Runs the specified script with yarn.")
    val yarnCommand         = settingKey[String]("Command for running yarn.")
    val yarnVerbose         = settingKey[Boolean]("Whether to verbosely run yarn (purportedly good for random failure debugging)")
  }

  val yarnCacheOpts = TaskKey.local[String]

  import autoImport._

  override def projectSettings: Seq[Setting[_]] = Seq(
    yarnPackageJson := baseDirectory.value / "package.json",
    yarnCachePerProject := sys.props.get("yarn.cache").exists(_.trim == "per-project"),
    yarnInstall := doYarnInstall().value,
    yarnRun := doYarnScript().evaluated,
    yarnCommand := "yarn",
    yarnVerbose := false,

    yarnCacheOpts := {
      val streams = Keys.streams.value
      if (yarnCachePerProject.value) {
        val cacheDir = streams.cacheDirectory / ".yarn-cache"
        s"--cache-folder ${cacheDir.getAbsolutePath}"
      } else ""
    }
  )

  override def globalSettings: Seq[Setting[_]] = Seq(
    concurrentRestrictions += Tags.limit(Tags.Network, 2),
  )

  def prefixedLogger(name: String): ProcessLogger = ProcessLogger(s => stdout.println(s"[$name]: $s"))

  def doYarnScript() = Def.inputTask[Unit] {
    val script = spaceDelimited("<arg>").parsed.mkString(" ")
    val command = s"${yarnCommand.value} run $script"

    streams.value.log.info(s"running: `$command` in ${baseDirectory.value}")

    val result = Process(command, cwd = baseDirectory.value) ! prefixedLogger(name.value)

    if (result != 0) {
      sys.error(s"yarn script <${name.value}/$script> failed (status code $result)")
    }
  }

  def doYarnInstall() = Def.task[Set[File]] {
    val verbose = if (yarnVerbose.value) "--verbose" else ""
    val command = s"yarn install --non-interactive --network-timeout 180000 --prefer-offline --pure-lockfile $verbose ${yarnCacheOpts.value}"
    val baseDir = baseDirectory.value
    val streams = Keys.streams.value

    streams.log.info(s"running: `$command` in $baseDir")

    def attempt(retry: Int): Int = {
      val res = Process(command, cwd = baseDir) ! prefixedLogger(name.value)
      if ((res != 0) && (retry < 4)) {
        streams.log.warn(s"yarn script <${name.value}/yarnInstall> failed (status code $res), waiting to retry...")
        Thread.sleep(30000L << retry) // 30 seconds, 1 minute, 2 minutes, 4 minutes
        attempt(1 + retry)
      } else {
        res
      }
    }
    val result = attempt(0)

    if (result != 0) {
      sys.error(s"${yarnCommand.value} <${name.value}/yarnInstall> failed (status code $result)")
    }

    Option((baseDir / "node_modules").listFiles()) match {
      case None =>
        streams.log.warn(s"no node_modules installed for ${name.value}...!")
        Set.empty
      case Some(files) =>
        files.toSet
    }
  } tag Tags.Network

}
