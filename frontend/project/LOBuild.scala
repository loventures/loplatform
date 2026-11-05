/*
 * LO Platform copyright (C) 2007–2026 LO Ventures LLC.
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
      scalaVersion := "3.8.4",
      // scripts/buildCar.js writes <project>/target/<name>.zip; sbt 2 relocates target.value under the build's
      // target/out/..., so anchor on baseDirectory to keep pointing at what yarn actually produced.
      Compile / products := Def.uncached(baseDirectory.value / "target" / s"${name.value}.zip" :: Nil),

      packagedCar := baseDirectory.value / "target" / s"${name.value}.zip",

      // sbt 2 maps artifacts to virtual file refs rather than java.io.File
      packagedArtifacts += Def.uncached(
        Artifact(name = name.value) -> (fileConverter.value.toVirtualFile(packagedCar.value.toPath): xsbti.HashedVirtualFileRef)
      ),
      artifacts += Artifact(name = name.value),

      // Def.uncached throughout: sbt 2 caches task results by default, but these shell out to yarn, whose inputs sbt
      // does not track, so a cache hit would silently skip the build.
      Compile / compile := Def.uncached {
        (yarnRun toTask " package").value
        val globalOutput = (Global / carAggregationDirectory).value
        val packagedCar = LOBuild.packagedCar.value
        IO.createDirectory(globalOutput)
        IO.copyFile(packagedCar, globalOutput / (packagedCar.getName replace ("zip", "jar")))
        streams.value.log.info(s"Copied packaged $packagedCar to $globalOutput")
        inc.Analysis.Empty
      },

      Compile / compile := Def.uncached { ((Compile / compile) dependsOn yarnInstall).value },

      clean := Def.uncached { clean.value; (baseDirectory.value / "node_modules").delete(); () },

      // sbt 2 makes test an input task yielding a TestResult; yarn test failing throws, so reaching the map means passed.
      Test / test := Def.inputTaskDyn {
        ((yarnRun toTask " test") dependsOn yarnInstall).map(_ => sbt.protocol.testing.TestResult.Passed)
      }.evaluated,

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
