/*
 * LO Platform copyright (C) 2007–2026 LO Ventures LLC.
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

package com.learningobjects.sbt

import java.io.FileInputStream
import java.util.Properties

import com.typesafe.sbt.GitPlugin
import sbt.Keys._
import sbt._
import sbt.plugins.JvmPlugin

object DECommonSettings extends AutoPlugin {

  override def requires: Plugins = JvmPlugin && GitPlugin

  override def trigger: PluginTrigger = noTrigger

  object autoImport {
    lazy val forkOptions    = taskKey[ForkOptions]("Jvm Options used for forking new test Processes.")
    lazy val readProperties = inputKey[Map[String, String]]("Read the given properties file.")
  }

  import autoImport._

  def baseProjectSettings: Seq[Def.Setting[_]] = Seq(
    updateOptions ~= { _.withCachedResolution(true) },
    Compile / console / scalacOptions --= notInConsole,
    Test / console / scalacOptions --= notInConsole,
  ) ++ Seq(Test).flatMap(testSettings)

  private val notInConsole = Seq[String](
    "-Wunused:imports,nowarn",
    "-Werror"
  )

  val pedantry = Seq(
    "-Ywarn-numeric-widen",
    "-Ywarn-value-discard"
  )

  val javaPedantry = Seq(
    "-deprecation",
    "-encoding",
    "UTF-8",
    "-g",
//    "-Xlint:all",
    "-werror"
  )

  def testSettings(cfg: Configuration) = Seq[Def.Setting[_]](
    libraryDependencies ++= List(
      // exclusion because Intellij will pick up the 4.11 version of junit and love it over the 4.13
      ("com.github.sbt" % "junit-interface" % "0.13.3").exclude("junit", "junit")
    ) map (_ % cfg),
    cfg / scalacOptions --= pedantry,
  )

  lazy val itSettings: Seq[Def.Setting[_]] = inConfig(IntegrationTest)(
    Defaults.testSettings ++ List(
      parallelExecution := false,
      logBuffered       := false // see Buffered Output at https://www.scalatest.org/user_guide/using_scalatest_with_sbt
    )
  ) ++ testSettings(IntegrationTest) ++ org.scalafmt.sbt.ScalafmtPlugin.scalafmtConfigSettings(IntegrationTest)

  lazy val DbTest: Configuration               = config("dbtest") extend Test
  lazy val dbTestSettings: Seq[Def.Setting[_]] = inConfig(DbTest)(
    Defaults.testSettings ++ List(
      parallelExecution := false,
      logBuffered       := false // see Buffered Output at https://www.scalatest.org/user_guide/using_scalatest_with_sbt
    )
  ) ++ testSettings(DbTest) ++ org.scalafmt.sbt.ScalafmtPlugin.scalafmtConfigSettings(DbTest)

  override def projectSettings: Seq[Def.Setting[_]] =
    baseProjectSettings

  override def buildSettings: Seq[Def.Setting[_]] = Seq(
    updateOptions        := updateOptions.value.withCachedResolution(true),
    organization         := "lo.ventures",
    organizationName     := "LO Ventures LLC",
    organizationHomepage := Some(url("https://lo.ventures/")),
    version              := "5.0.0",
    scalaVersion         := "3.8.4",
    scalacOptions        := Seq(
      "-deprecation",
      "-feature",
      "-unchecked",
      "-source",
      "3.8",
      "-preview",
      "-language:higherKinds",
      "-language:existentials",
      "-encoding",
      "UTF-8",
    ),
    scalacOptions ++= notInConsole,
    scalacOptions ++= pedantry,
    scalacOptions ++= Seq(
      "-Ybackend-parallelism",
      "4", // https://xkcd.com/221/
      "-Xkind-projector",
      "-explain-cyclic",
      "-Xtarget:25",
    ),
    javacOptions ++= Seq(
      "-g",
      "-encoding",
      "UTF-8",
      "-parameters",
      "--release",
      "17",
      "-proc:none",
    ),
    readProperties       := {
      import sbt.complete.DefaultParsers.fileParser

      import scala.collection.JavaConverters._
      // fileParser doesn't pass the base directory to file's constructor for some reason.
      val filePath = fileParser(file(sys.props("user.home"))).parsed
      val propFile = if (filePath.isAbsolute) {
        filePath
      } else {
        file(sys.props("user.home")) / filePath.getPath
      }
      val newProps = new Properties()
      val fis      = new FileInputStream(propFile)
      try newProps.load(fis)
      finally fis.close()
      val propMap  = newProps.asScala.toMap
      sys.props ++= propMap
      propMap
    }
  )
}
