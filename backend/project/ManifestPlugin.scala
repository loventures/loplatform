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

import java.text.SimpleDateFormat
import java.util.Date

import com.typesafe.sbt.GitPlugin
import com.typesafe.sbt.SbtGit.GitKeys._
import sbt.Keys._
import sbt.plugins.JvmPlugin
import sbt.{AutoPlugin, Package, PluginTrigger, Plugins, TaskKey, _}
import sbtbuildinfo.BuildInfoKeys._
import sbtbuildinfo.BuildInfoPlugin

object ManifestPlugin extends AutoPlugin {
  override def requires: Plugins = JvmPlugin && BuildInfoPlugin && GitPlugin

  override def trigger: PluginTrigger = noTrigger

  override def projectSettings: Seq[Def.Setting[_]] = Seq(
    packageOptions += writeManifest(
      name = (Compile / name).value,
      org = (Compile / organization).value,
      orgName = (Compile / organizationName).value,
      version = (Compile / version).value.replace("-SNAPSHOT", ""),
      gitBranch = sys.env get "GIT_BRANCH" getOrElse (gitCurrentBranch.value),
      rev = (gitHeadCommit.value).getOrElse("HEAD"),
      date = new Date,
      build = "SBT",
      buildNumber = sys.env get "BUILD_NUMBER" getOrElse (buildInfoBuildNumber.value).toString,
      revLink = s"https://github.com/loventures/loplatform/commit/${(gitHeadCommit.value).get}"
    )
  )

  def writeManifest(
    name: String,
    org: String,
    orgName: String,
    version: String,
    gitBranch: String,
    rev: String,
    date: Date,
    build: String,
    buildNumber: String,
    revLink: String
  ) = Package.ManifestAttributes(
    "Implementation-Title"        -> name,
    "Implementation-Version"      -> version,
    "Implementation-Build"        -> build,
    "Implementation-VendorId"     -> org,
    "Implementation-Vendor"       -> orgName, //
    "Implementation-Branch"       -> gitBranch,
    "Implementation-Revision"     -> rev.take(16),
    "Implementation-RevisionLink" -> revLink,
    "Implementation-BuildDate"    -> new SimpleDateFormat("yyyy/MM/dd HH:mm:ss Z").format(date),
    "Implementation-BuildNumber"  -> buildNumber,
  )
}
