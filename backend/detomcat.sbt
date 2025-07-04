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

val detomcat = (project in file("detomcat"))
  .enablePlugins(JavaAppPackaging, BuildInfoPlugin, DECommonSettings)
  .dependsOn(
    LocalProject("scaloi"),
  )
  .settings(
    name                 := "DE Tomcat",
    normalizedName       := "de-tomcat",
    organization         := "com.learningobjects.detomcat",
    maintainer           := "LO Ventures <info@lo.ventures>",
    packageSummary       := "DE Tomcat is a Component Framework aware tomcat distribution.",
    packageDescription   := """A custom embedded tomcat distribution.""",
    executableScriptName := "detomcat",
    run / fork           := true,
    run / javaOptions ++= Seq("-Xmx2G"),
    Global / cancelable  := true,
    buildInfoPackage     := "de.tomcat",
    buildInfoKeys += BuildInfoKey.action("javaVersion")(sys.props("java.version")),
    libraryDependencies ++= Seq(
      // Tomcat EmbededLibs
      JavaEE.Tomcat.Embedded.core,
      JavaEE.Tomcat.Embedded.websocket,
      Http4s.servlet,
      ScalaExtensions.xml,
      Misc.scopt,
      JSON.Argonaut.argonaut,
      Misc.fansi,
      Config.typesafe,
      ApacheCommons.lang3,
    ),
  )
