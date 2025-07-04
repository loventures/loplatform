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

lazy val Utl: Configuration = config("utl") extend Runtime

lazy val core = project
  .dependsOn(
    LocalProject("coreApi"),
    LocalProject("detomcat"),
    LocalProject("util"),
  )
  .enablePlugins(DECommonSettings, ManifestPlugin)
  .configs(Utl)
  .settings(
    normalizedName := "core",
    name           := "CPXP Core Services",
    inConfig(Utl)(Defaults.compileSettings) ++ org.scalafmt.sbt.ScalafmtPlugin.scalafmtConfigSettings(Utl),

    // Apache Dependencies
    libraryDependencies ++= Seq(
      ApacheHttpComponents.httpCore,
      ApacheHttpComponents.httpMime,
      ApacheHttpComponents.httpClient,
      ApacheCommons.lang3,
      ApacheCommons.collections4,
      ApacheCommons.io,
      ApacheCommons.math3,
      ApacheCommons.net,
      ApacheCommons.exec,
      Config.ficus,
    ),
    // Database Deps
    libraryDependencies ++= Seq(
      Hibernate.core,
      Hibernate.types,
      Databases.postgresql,
      Databases.redshift,
      Databases.hikaricp,
      Config.typesafe,
    ),
    // Json Deps
    libraryDependencies ++= Seq(
      JSON.Argonaut.argonaut,
      JSON.Argonaut.argonautCats,
      JSON.Jackson.core,
      JSON.Jackson.annotations,
      JSON.Jackson.databind,
      JSON.Jackson.mrBean,
      JSON.Jackson.guava,
      JSON.Jackson.jdk8,
      JSON.Jackson.jsr310,
      JSON.Jackson.scala,
    ),
    // Questionable dependencies
    // These dependencies are questionable because they may or may not still be used,
    // or even if they are used, really shouldn't be exported transitively to users of core/,
    // and their usages probably belong in internal/ instead.
    libraryDependencies ++= Seq(
      ASM.asm,
      ASM.util,
      Misc.byteBuddy,
      Misc.sassEmbeddedHost,
      Misc.javassist,
    ),
    // Pekko
    libraryDependencies ++= Seq(
      Pekko.actor,
      Pekko.cluster,
      Pekko.discovery,
      Pekko.remote,
//      Pekko.clusterCustomDowning,
      Pekko.clusterTools,
      Pekko.slf4j,
      Pekko.clusterBootstrap,
      Pekko.discoveryAws
    ),
    // Other stuff
    libraryDependencies ++= Seq(
      Cloud.Jclouds.fileSystemApi,
      Cloud.Jclouds.awsS3Provider,
      Cloud.AWS.core,
      Cloud.AWS.ec2,
      Cloud.AWS.s3,
      Cloud.AWS.sts,
      JavaEE.jaxbApi,
      JavaEE.jaxbImpl,
//      Misc.imsEnterprise,
//      Misc.qtiTools,
      Misc.scalaCsv,
      Misc.opencsv,
      Misc.nashorn,
      Misc.paranamer.force(),
      Logging.log4s,
      Logging.Slf4j.api,
      Logging.Slf4j.jdk14,
      JavaEE.ejbApi,
      JavaEE.Tomcat.Embedded.core % "provided",
      ScalaExtensions.Enumeratum.core,
      ScalaExtensions.java8Compat,
      ScalaZ.core,
      Databases.Doobie.core,
      Databases.Doobie.postgres,
      Databases.Doobie.hikari,
      Cats.effect,
      Cats.mouse,
    ),
    // These should technically be `% "runtime;utl"`, but IntelliJ...
    libraryDependencies ++= Seq(
      JSON.Circe.core,
      JSON.Circe.generic,
      JSON.Circe.jawn,
      Cloud.AWS.translate,
      Http4s.blazeClient,
      Http4s.circe,
      Http4s.client,
      Misc.commonMark,
      Misc.scopt,
    ),
    dependencyUpdatesFilter -= crapCommonsIO,
    dependencyUpdatesFilter -= crapCommonsNet,
  )

lazy val coreApi = (project in file("core-api"))
  .enablePlugins(DECommonSettings)
  .dependsOn(
    LocalProject("scaloi"),
  )
  .settings(
    name           := "CPXP Core - Component API",
    normalizedName := "core-api",
    description    := "APIs and Interfaces to the Component Framework",
    libraryDependencies ++= Seq(
      ApacheCommons.collections4,
      ApacheCommons.io,
      ApacheCommons.lang3,
      ApacheCommons.text,
      ApacheHttpComponents.httpClient,
      Cloud.AWS.s3,
      Cloud.Jclouds.fileSystemApi,
      Cloud.Jclouds.awsS3Provider,
      Config.typesafe,
      Hibernate.core,
      JavaEE.javaEE7,
      JavaEE.Tomcat.servletApi,
      JSON.Jackson.annotations,
      JSON.Jackson.databind,
      JSON.Argonaut.argonaut,
//      JSON.Argonaut.argonautShapeless,
      Misc.findbugsJsr305,
      // Would prefer to not have this as a dependency.
      Misc.guava,
      Misc.jericho,
//      Scala.reflect(scalaVersion.value),
//      Scala.compiler(scalaVersion.value) % Provided,
      ScalaExtensions.java8Compat,
      ScalaExtensions.xml,
      ScalaZ.core,
    ),
    dependencyUpdatesFilter -= crapCommonsIO,
  )

lazy val internal = project
  .dependsOn(
    LocalProject("core"),
  )
  .enablePlugins(DECommonSettings)
  .settings(
    normalizedName := "internal",
    name           := "CPXP Internal",
  )
