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

//Platformy projects
import Dependencies._

lazy val api = (project in file("car/api"))
  .dependsOn(
    LocalProject("core"),
    LocalProject("core") % "test->test",
  )
  .enablePlugins(DECommonSettings, SbtComponentArchivePlugin)
  .settings(
    normalizedName := "api",
    name           := "CPXP CAR - API",
    description    := "Core APIs without implementations.",
    libraryDependencies ++= Seq(
      Cloud.AWS.s3.intransitive() % Provided, // eh, but provided will help
      JavaEE.Tomcat.Embedded.core % "provided",
      JSON.Argonaut.argonaut,
//      JSON.Argonaut.argonautShapeless,
      JSON.jsonldJava,
    ),
  )

lazy val integration = (project in file("car/integration"))
  .dependsOn(
    LocalProject("component"),
  )
  .enablePlugins(DECommonSettings, SbtComponentArchivePlugin)
  .settings(
    normalizedName := "integration",
    name           := "CPXP CAR - Integration",
    description    := "Integration components.",
    libraryDependencies ++= Seq(
      Misc.ldap,
      Misc.ltiUtilNeitherCglibNorJackson,
    ),
  )

lazy val main = (project in file("car/main"))
  .dependsOn(
    LocalProject("api"),
    LocalProject("component"),
    LocalProject("presenceApi"),
  )
  .enablePlugins(DECommonSettings, SbtComponentArchivePlugin)
  .settings(
    normalizedName := "main",
    name           := "CPXP CAR - Main",
    description    := "Implementations of API stuff.",
    libraryDependencies ++= Seq(
      ScalaZ.core,
      JSON.Jackson.csv,
      Logging.log4s,
      JavaEE.Tomcat.Embedded.core   % "provided",
      Databases.hikaricp,
//      Misc.Kantan.csv,
      Misc.opencsv,
      // Databases.redshift ffs aws claims to be "postgres"....
    ),
  )

lazy val system = (project in file("car/system"))
  .dependsOn(
    LocalProject("platformApi"),
  )
  .enablePlugins(DECommonSettings, SbtComponentArchivePlugin) // GitTicketsPlugin
  .settings(
    normalizedName := "system",
    name           := "CPXP CAR - System",
    description    := """
    |System level components. These should be completely standalone;
    |neither dependent on the base components nor a dependency thereof.""".stripMargin,
    libraryDependencies ++= Seq(
      Cloud.AWS.core,
      Cloud.AWS.s3,
      ApacheCommons.net,
      JavaEE.javaEE7,
      JavaEE.Tomcat.Embedded.core % "provided",
      JSON.Jackson.xml,
      Prometheus.simpleclient,
      Prometheus.simpleclient_common,
      Prometheus.simpleclient_hotspot,
      Scala.compiler(scalaVersion.value),
    ),
    dependencyUpdatesFilter -= crapCommonsNet,
  )

lazy val cdn = (project in file("car/cdn"))
  .dependsOn(
    LocalProject("coreApi")
  )
  .enablePlugins(DECommonSettings, SbtComponentArchivePlugin)
  .settings(
    normalizedName := "cdn",
    name           := "CPXP CAR - CDN",
    description    := "Third-party scripts for CDN distribution.",
    Compile / unmanagedResourceDirectories +=
      sourceDirectory.value / "main" / "webapp",
  )

lazy val platformApi = (project in file("platform/api"))
  .dependsOn(
    LocalProject("api"),
  )
  .enablePlugins(DECommonSettings, SbtComponentArchivePlugin)
  .settings(
    normalizedName := "platform-api",
    name           := "Platform - API",
    description    := "API for the platform machinery.",
    libraryDependencies ++= Seq(
      Misc.quartz,
    )
  )

lazy val platformInternal = (project in file("platform/internal"))
  .dependsOn(
    LocalProject("component"),
    LocalProject("platformApi"),
    LocalProject("presenceApi"),
  )
  .enablePlugins(DECommonSettings, SbtComponentArchivePlugin)
  .settings(
    normalizedName := "platform-internal",
    name           := "Platform - Internal",
    description    := "Internals of the platform machinery.",
    libraryDependencies ++= Seq(
      Cloud.AWS.core,
      Cloud.AWS.sqs,
      JSON.JWT.scala,
    ),
  )

lazy val presenceApi = (project in file("presence/api"))
  .dependsOn(
    LocalProject("coursewareApi")
  )
  .enablePlugins(DECommonSettings, SbtComponentArchivePlugin)
  .settings(
    normalizedName := "presence-api",
    name           := "Presence - API",
    description    := "API for the presence machinery.",
  )

lazy val presenceInternal = (project in file("presence/internal"))
  .dependsOn(
    LocalProject("presenceApi"),
  )
  .enablePlugins(DECommonSettings, SbtComponentArchivePlugin)
  .settings(
    normalizedName := "presence-internal",
    name           := "Presence - Internal",
    description    := "Internal implementation of the presence machinery.",
  )
