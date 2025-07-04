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

/* Analytical projects */

lazy val analyticsApi = (project in file("analytics/analytics-api"))
  .dependsOn(
    LocalProject("authoringApi"),
    LocalProject("system"),
  )
  .enablePlugins(DECommonSettings, SbtComponentArchivePlugin)
  .settings(
    normalizedName := "analytics-api",
    name           := "CPXP Analytics",
    description    := "LO Analytics Module",
    libraryDependencies ++= Seq(
      JavaEE.Tomcat.Embedded.core % "provided"
    ),
  )

lazy val analyticsInternal = (project in file("analytics/analytics-internal"))
  .dependsOn(
    LocalProject("analyticsApi"),
    LocalProject("coursewareApi"),
  )
  .enablePlugins(DECommonSettings, SbtComponentArchivePlugin)
  .settings(
    normalizedName := "analytics-internal",
    name           := "CPXP Analytics INTERNAL",
    description    := "LO Analytics Module - INTERNAL",
    libraryDependencies ++= Seq(
      JavaEE.Tomcat.Embedded.core % "provided",
      Databases.Doobie.core,
      Databases.Doobie.postgres,
      Config.ficus,
    ),
  )
