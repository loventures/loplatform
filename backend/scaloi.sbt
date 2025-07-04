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

lazy val scaloi = (project in file("scaloi"))
  .enablePlugins(DECommonSettings)
  .settings(
    normalizedName               := "scaloi",
    name                         := "ScaLOI",
    description                  := "Generic Functional Utilities from Learning Objects",
    scalaVersion                 := "3.7.1",
    libraryDependencies ++= List(
      JSON.Argonaut.argonaut,
      ScalaExtensions.Enumeratum.core,
      ScalaExtensions.Enumeratum.argonaut,
      ScalaZ.core,
      Jakarta.servletApi,
    ),
    doc                          := (doc / target).value,
    packageDoc / publishArtifact := false,
  )
