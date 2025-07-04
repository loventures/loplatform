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

import com.learningobjects.sbt.DECommonSettings
import sbt.Defaults.{bgRunMainTask, bgRunTask}
import sbt.Keys.*
import sbt.plugins.JvmPlugin
import sbt.{Def, *}

import java.io.File

/** Configures a web application to with DETomcat.
  */
object DEWebPlugin extends AutoPlugin {

  override def requires: Plugins = JvmPlugin && DECommonSettings

  override def trigger: PluginTrigger = noTrigger

  object autoImport {
    val killAll              = taskKey[Seq[JobHandle]]("")
    val waitOneSec           = taskKey[Unit]("Thread.sleep for one second")
    val detomcatConfig       = settingKey[Option[File]]("A HOCON format config file for detomcat.")
    val detomcatDebugPort    = settingKey[Option[Int]]("A debug port detomcat.")
    val detomcatDebugSuspend = settingKey[Boolean]("If  true suspends the VM on startup until a debugger is attached")
    val detomcatVersion      = settingKey[String]("Version setting for DETomcat.")
  }

  import autoImport.*

  override def projectSettings: Seq[Def.Setting[_]] =
    inConfig(Compile)(
      Seq(
        // Replace the Run task to use fullClasspath instead of fullClasspathASJars
        bgRunNoJarsTask,
        bgRunMainNoJarsTask
      )
    ) ++ Seq(
      // detomcatVersion := "1.3.0-SNAPSHOT",
      detomcatConfig              := None,
      detomcatDebugPort           := None,
      detomcatDebugSuspend        := false,
      javaOptions                 := Seq(
        "-server",
        "-ea",
        "-Xmx2G",
        "-Xms2G",
        "-XX:+UseG1GC",
        "-XX:MaxGCPauseMillis=200",
        "-Dfile.encoding=UTF-8",
        "--add-opens",
        "java.base/java.lang=ALL-UNNAMED"
      ),
      fork                        := true,
      bgCopyClasspath             := false,
      Compile / run / forkOptions := {
        val options = (Compile / run / forkOptions).value
        val suspend = if (detomcatDebugSuspend.value) "y" else "n"
        options
          .withRunJVMOptions(
            detomcatConfig.value
              .map(file => Vector(s"-Dconfig.file=${file.getAbsolutePath}"))
              .getOrElse(Vector.empty) ++
              detomcatDebugPort.value
                .map(port => Vector(s"-agentlib:jdwp=transport=dt_socket,server=y,suspend=$suspend,address=$port"))
                .getOrElse(Vector.empty) ++
              options.runJVMOptions
          )
          .withWorkingDirectory(target.value)
      },
      killAll                     := {
        val service = (bgJobService in Global).value
        val jobs    = service.jobs
        jobs.foreach(service.stop)
        jobs
      },
      waitOneSec                  := {
        Thread.sleep(1000)
      }
    )

  def bgRunNoJarsTask = bgRun := bgRunTask(
    exportedProductJars,
    This / This / This / fullClasspath,
    run / mainClass,
    bgRun / bgCopyClasspath,
    run / runner
  ).evaluated

  def bgRunMainNoJarsTask = bgRunMain := bgRunMainTask(
    exportedProductJars,
    This / This / This / fullClasspath,
    bgRunMain / bgCopyClasspath,
    run / runner
  ).evaluated
}
