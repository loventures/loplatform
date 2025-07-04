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

package com.learningobjects.sbt

import java.util.Date

import com.typesafe.sbt.GitPlugin
import com.typesafe.sbt.SbtGit.git
import sbt.Keys._
import sbt._
import sbt.internal.BuildDependencies
import sbt.plugins.JvmPlugin

object SbtComponentArchivePlugin extends AutoPlugin {

  override def requires: Plugins = JvmPlugin && GitPlugin
  override def trigger           = noTrigger

  object autoImport {
    val carJson           = settingKey[File]("The file to write archive metadata to.")
    val packageJson       = taskKey[Seq[File]]("Package car.json")
    val componentProjects = settingKey[Seq[ProjectRef]]("A list of projects with the component plugin enabled.")
    val carImplements     =
      settingKey[Option[LocalProject]]("If this is an internal project, which API project it implements")

    def implements(project: String) = carImplements := Some(LocalProject(project))
  }

  import autoImport._

  override def buildSettings: Seq[Def.Setting[_]] = Seq(
    componentProjects := Seq.empty,
    carImplements     := None
  )

  override lazy val projectSettings = Seq[Def.Setting[_]](
    ThisBuild / componentProjects += thisProjectRef.value,
    Compile / carJson     := (Compile / resourceManaged).value / "car.json",
    Compile / packageJson := {
      val buildDeps = (Compile / buildDependencies).value
      val impls     = carImplements.value
      val proj      = thisProjectRef.value
      val nnme      = normalizedName.value
      val org       = organization.value
      val nme       = name.value
      val ver       = version.value
      val branch    = git.gitCurrentBranch.value
      val commit    = git.gitHeadCommit.value
      val json      = (Compile / carJson).value
      val out       = (Compile / classDirectory).value
      writeArchiveInfo(
        buildDeps,
        impls,
        proj,
        nnme,
        org,
        nme,
        ver,
        branch,
        commit,
        json,
        out
      )
    },
    Compile / resourceGenerators += Def.task {
      (Compile / compile).value
      (Compile / packageJson).value
    },
    packageOptions += Package.ManifestAttributes(
      "Implementation-Explodeable"      -> "true",
      "Implementation-ComponentArchive" -> "true"
    ),
  )

  case class CarJson(
    identifier: String,
    name: String,
    version: String,
    buildDate: Date,
    dependencies: Seq[String],
    implements: Option[String],
    components: Seq[ComponentJson],
    branch: String,
    revision: String
  ) {
    def toJson = { // Poor man's json
      val implementing = implements.fold("") { i => s""""implementing" : "$i",""" }
      s"""
         |{
         |  "identifier" : "$identifier",
         |  "name" : "$name",
         |  "version" : "$version",
         |  "buildDate" : "$buildDate",
         |  "dependencies" : ${dependencies.map(dep => s""""$dep"""").mkString("[", ",", "]")},
         |  $implementing
         |  "components" : [],
         |  "branch" : "${sys.env.getOrElse("GIT_BRANCH", branch)}",
         |  "revision" : "$revision",
         |  "buildNumber" : "${sys.env.getOrElse("BUILD_NUMBER", "")}"
         |}""".stripMargin
    }
  }
  case class ComponentJson(identifier: String, implementation: String)

  def capitalizeTail(a: Seq[String]) = a.head + a.tail.map(_.capitalize).mkString

  def deHyphen(a: String) = capitalizeTail(a.split('-')) // foo-bar to fooBar

  def writeArchiveInfo(
    dependencies: BuildDependencies,
    implements: Option[LocalProject],
    thisProject: ProjectRef,
    normalizedName: String,
    organization: String,
    name: String,
    version: String,
    branch: String,
    revision: Option[String],
    carFile: File,
    classes: File
  ): Seq[File] = {

    val deps                 = dependencies.classpathTransitiveRefs(thisProject) map { projectRef =>
      s"$organization.${projectRef.project}"
    }
    val impl: Option[String] = implements map { projectRef => s"$organization.${deHyphen(projectRef.project)}" }

    /* This feels weird, but Analysis is large and confusing,
     * and we get called non-incrementally every time, so... */
    val stamps = classes.**("*.class").get.map(_.lastModified)
    val stamp  = if (stamps.isEmpty) new Date(0) else new Date(stamps.max)

    val carInfo = CarJson(
      s"$organization.${deHyphen(normalizedName)}",
      name,
      version,
      stamp,
      deps,
      impl,
      Seq.empty,
      branch,
      revision getOrElse ""
    )
    val json    = carInfo.toJson
    if (carFile.exists()) {
      carFile.delete()
    }
    IO.write(carFile, json)
    Seq(carFile)
  }
}
