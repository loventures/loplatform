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
import sbtrelease._
import ReleaseTransformations._

enablePlugins(GitPlugin, DERepositories)

name        := "de"
description := "Difference Engine Root Project"

/* Enable Ctrl-C */
Global / cancelable := true

ThisBuild / Compile / packageDoc / publishArtifact := false
Global / publishMavenStyle := false

// keep the release version the same
releaseVersion := { ver => sbtrelease.Version(ver).map(_.unapply).getOrElse(versionFormatError(ver)) }

// bump the minor version eg. 7.1.0-SNAPSHOT -> 7.2.0-SNAPSHOT
// or bump the patch version eg. 7.1.1-SNAPSHOT -> 7.1.1-SNAPSHOT
// depending on the ENV variable
// Defaults to Minor
releaseNextVersion := { ver =>
  sbtrelease
    .Version(ver)
    .map(version => {
      val bumpType = sys.props
        .getOrElse("version.bump.type", sbtrelease.Version.Bump.Minor.toString)
      if (bumpType == sbtrelease.Version.Bump.Bugfix.toString) {
        version.bumpBugfix.asSnapshot.unapply
      } else {
        version.bumpMinor.asSnapshot.unapply
      }
    })
    .getOrElse(versionFormatError(ver))
}

releaseProcess := Seq[ReleaseStep](
  inquireVersions,   // Check the versions
  setNextVersion,    // Set the next version, this is modifying `develop`
  commitNextVersion, // Commit origin/develop
  pushChanges        // Push origin/develop
)

/** Pretend these libraries are always binary compatible so we can upgrade while some of our dependencies remain built
  * against prior versions of these libraries. Remove one day.
  */
ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-java8-compat"       % "always"
ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml"                % "always"
ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-parser-combinators" % "always"

inThisBuild(
  Seq[Def.Setting[_]](
    scalacOptions ~= { _ filterNot (DECommonSettings.pedantry contains _) }
  )
)

com.learningobjects.sbt.Bruce.banner // be welcoming

lazy val component = project
  .dependsOn(
    LocalProject("coursewareApi"),
    LocalProject("util"),
  )
  .enablePlugins(SbtComponentArchivePlugin, DECommonSettings)
  .settings(
    normalizedName := "component",
    name           := "CPXP Components",
    libraryDependencies ++= Seq(
      Cloud.AWS.core,
      JavaEE.javaEE7,
      JavaEE.jaxbApi,
      JavaEE.jaxbImpl,
      JavaEE.mailApi,
      JavaEE.Tomcat.Embedded.core % "provided",
      Misc.ltiUtilNeitherCglibNorJackson,
      Misc.opencsv,
      Misc.paranamer,
//      XML.XSD.ltiJaxb,
    ),
    Compile / unmanagedResourceDirectories +=
      sourceDirectory.value / "main" / "webapp",
  )
