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

lazy val authoringApi = (project in file("authoring/authoring-api"))
  .dependsOn(
    LocalProject("api"),
  )
  .enablePlugins(
    DECommonSettings,
    // for detecting @RightBinding
    SbtComponentArchivePlugin
  )
  .settings(
    normalizedName := "authoring-api",
    name           := "Authoring - API",
    description    := "API of the authoring machinery",
    libraryDependencies ++= Seq(
      Misc.Elastic4s.clientEsJava,
      Misc.awsSigningRequestInterceptor,
      Misc.ApachePOI.ooxml,
      Misc.apachePdfBox,
      Misc.jUniversalCharDet,
      Misc.jericho,
    ),
    dependencyUpdatesFilter -= moduleFilter(organization = "com.sksamuel.elastic4s"), // no updates for amazon
  )

lazy val authoringInternal = (project in file("authoring/authoring-internal"))
  .dependsOn(
    LocalProject("authoringApi"),
    LocalProject("presenceApi"),
  )
  .enablePlugins(DECommonSettings, SbtComponentArchivePlugin)
  .settings(
    normalizedName := "authoring-internal",
    name           := "Authoring - Internal",
    description    := "Internals of the authoring machinery",
    libraryDependencies ++= Seq(
      Databases.redis,
      Cloud.AWS.core,
      Cloud.AWS.auth,
      JavaEE.jaxbApi,
      JavaEE.jaxbImpl,
      Misc.classGraph,
//      Misc.Kantan.csv,
//      Misc.Kantan.generic,
      Misc.apacheTika,
      Misc.nashorn,
      Misc.phCss,
    ),
  )
