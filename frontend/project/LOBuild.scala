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

package build

import sbt._, Keys._
import sbt.internal.inc

object LOBuild {

  import Yarn.autoImport._

  lazy val packagedCar = settingKey[File]("the actual result of compiling")
  lazy val carAggregationDirectory = settingKey[File]("where we dump things for de")

  lazy val baseYarnBuild: Seq[Def.Setting[_]] = {
    Seq(
      Compile / products := target.value / s"${name.value}.zip" :: Nil,

      packagedCar := target.value / s"${name.value}.zip",

      packagedArtifacts += (Artifact(name = name.value) -> packagedCar.value),
      artifacts += Artifact(name = name.value),

      Compile / compile := {
        (yarnRun toTask " package").value
        val globalOutput = (Global / carAggregationDirectory).value
        val packagedCar = LOBuild.packagedCar.value
        IO.createDirectory(globalOutput)
        IO.copyFile(packagedCar, globalOutput / (packagedCar.getName replace ("zip", "jar")))
        streams.value.log.info(s"Copied packaged $packagedCar to $globalOutput")
        inc.Analysis.Empty
      },

      Compile / compile := { (Compile / compile) dependsOn yarnInstall }.value,

      clean := { clean.value; (baseDirectory.value / "node_modules").delete(); () },

      Test / test := { (yarnRun toTask " test") dependsOn yarnInstall }.value,

      // disable publishing the main jar produced by `package`
      compile / packageBin / publishArtifact := false,
      // disable publishing the main API docs jar
      Compile / packageDoc / publishArtifact := false,
      // disable publishing the main sources jar
      Compile / packageSrc / publishArtifact := false,

      // don't include "scala_2.10" in artifact name
      crossPaths := false,

      publishMavenStyle := true,
    )
  }
}
