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

import Dependencies._
import sbt.io.Path._

lazy val util = (project in file("util"))
  .enablePlugins(DECommonSettings)
  .dependsOn(
    LocalProject("scaloi"),
  )
  .settings(
    normalizedName := "util",
    name           := "CPXP Utility Library",
    libraryDependencies ++= Seq(
      ApacheCommons.beanUtils,
      ApacheCommons.codec,
      ApacheCommons.collections4,
      ApacheCommons.io,
      ApacheCommons.lang3,
      ApacheCommons.urlValidator,
      ApacheHttpComponents.httpClient,
      ApacheHttpComponents.httpCore,
      Cats.effect,
      Cloud.AWS.core,
      Cloud.Jclouds.fileSystemApi,
      Cloud.Jclouds.awsS3Provider,
      Jakarta.servletApi,
      JavaEE.javaEE7,
      JSON.Jackson.annotations,
      JSON.Jackson.databind,
      Logging.log4jCore,
      Logging.log4s,
      Logging.Slf4j.api,
      Misc.Kantan.csv,
      Misc.findbugsJsr305,
      Misc.guava,
      Misc.classGraph,
      Prometheus.simpleclient,
      ScalaExtensions.Enumeratum.core,
      ScalaExtensions.java8Compat,
      ScalaZ.core,
//      XML.tagsoup,
    ),
    dependencyUpdatesFilter -= crapCommonsIO,
    dependencyUpdatesFilter -= crapCommonsCodec,
    /** Build zip archives out of sources in src/main/zip.
      */
    Compile / resourceGenerators += Def.task {
      val srcDir  = (Compile / baseDirectory).value
      val outDir  = (Compile / resourceManaged).value
      val stream  = (Compile / streams).value
      val logger  = stream.log
      val zipBase = srcDir / "src" / "main" / "zips"
      val zipDirs = zipBase.listFiles().filter(_.isDirectory).toSeq
      logger.debug(s"$zipBase : $zipDirs")
      zipDirs map { dir =>
        val target   = outDir / "zips" / s"${dir.getName}.zip"
        val mappings = (dir.allPaths --- dir) pair relativeTo(dir)
        logger.debug(mappings.mkString)
        IO.zip(mappings, target, None)
        target
      }
    }
  )
