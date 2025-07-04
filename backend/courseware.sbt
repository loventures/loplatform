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
import sbt.LocalProject

/*
 * Projects containing lightweight courseware building blocks
 */
lazy val coursewareApi = (project in file("courseware/courseware-api"))
  .dependsOn(
    LocalProject("analyticsApi"),
    LocalProject("platformApi"),
  )
  .enablePlugins(DECommonSettings, SbtComponentArchivePlugin)
  .settings(
    normalizedName := "coursewareApi",
    name           := "CPXP CAR - Courseware (API)",
    description    := "Common classes for building lightweight courseware on top of.",
    libraryDependencies ++= Seq(
      ScalaExtensions.Enumeratum.circe,
      JSON.JWT.scala
    ),
  )

lazy val coursewareInternal = (project in file("courseware/courseware-internal"))
  .dependsOn(
    LocalProject("coursewareApi"),
    LocalProject("integration"),
    LocalProject("presenceApi"),
  )
  .enablePlugins(DECommonSettings, SbtComponentArchivePlugin)
  .settings(
    normalizedName := "coursewareInternal",
    name           := "CPXP CAR - Courseware (Internal)",
    description    := "Common classes for building lightweight courseware on top of.",
    libraryDependencies ++= Seq(
//      Misc.Kantan.csv,
//      Misc.Kantan.generic,
      Misc.spoiwo,
      ScalaExtensions.parserCombinators,
      Testing.scalaTest,
      JSON.JWT.scala,
      JSON.jsonDiff
    ),
  )
