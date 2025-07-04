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

import build.LOBuild._
import sbt.internal.inc
import sbtrelease._
import ReleaseTransformations._

Global / cancelable := true // ^C support

// If true then you maybe must -Dyarn.cache=per-project but that defeats github yarn cache?
// Global / parallelExecution := false // so compile task on the aggregated projects runs sequentially for sake of human log readers

// keep the release version the same
releaseVersion     := { ver => sbtrelease.Version(ver).map(_.unapply).getOrElse(versionFormatError(ver)) }

// bump the minor version eg. 7.1.0-SNAPSHOT -> 7.2.0-SNAPSHOT
// or bump the patch version eg. 7.1.1-SNAPSHOT -> 7.1.1-SNAPSHOT
// depending on the ENV variable
// Defaults to Minor
releaseNextVersion := { ver => sbtrelease.Version(ver).map(version => {
  val bumpType = sys.props
    .getOrElse("version.bump.type", sbtrelease.Version.Bump.Minor.toString)
  if (bumpType == sbtrelease.Version.Bump.Bugfix.toString) {
    version.bumpBugfix.asSnapshot.unapply
  } else {
    version.bumpMinor.asSnapshot.unapply
  }
}).getOrElse(versionFormatError(ver)) }

releaseProcess := Seq[ReleaseStep](
  inquireVersions, // Check the versions
  setNextVersion, // Set the next version, this is modifying `develop`
  commitNextVersion, // Commit origin/develop
  pushChanges // Push origin/develop
)

Global / carAggregationDirectory := {
  (LocalProject("frontend") / target).value / "cars"
}

ThisBuild / organization := "lo.ventures"

ThisBuild / isSnapshot := true // vaguely unnecessary but meh

lazy val platform = (project in file("platform"))
  .enablePlugins(Yarn)
  .settings(baseYarnBuild : _*)
  .settings(
    name := "platform",
    // Test / test := Def.task(()), // no tests yet .. this should run tsc but...
  )

lazy val courseware = (project in file("courseware"))
  .enablePlugins(Yarn)
  .settings(baseYarnBuild : _*)
  .settings(
    name := "courseware",
    // Test / test := { (yarnRun toTask " test") dependsOn (yarnRun toTask " ukr-check") dependsOn yarnInstall }.value,
  )

lazy val authoring = (project in file("authoring"))
  .enablePlugins(Yarn)
  .settings(baseYarnBuild : _*)
  .settings(
    name := "authoring",
  )

lazy val frontend = (project in file("."))
  .aggregate(
    authoring,
    courseware,
    platform,
  )
  .settings(
    publishArtifact := false,
    publish := Def.task(()),
    addCommandAlias("install", "authoring/yarnInstall; courseware/yarnInstall; platform/yarnInstall"),
    addCommandAlias("build", "compile"),
  )

Keys.onLoadMessage in Scope.Global := {
  if (sys.env.contains("CI")) "" else "\u001b]0;Frontend\u0007"
}
