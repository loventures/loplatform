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

lazy val frontendArchiveDir = settingKey[Option[File]]("directory containing frontend archives")
lazy val frontendArchives   = TaskKey.local[List[File]]

//Deploy depends on everything
lazy val deploy = project
  .dependsOn(
    LocalProject("core"),
    LocalProject("coreApi"),
    LocalProject("util"),
    LocalProject("webapp"),
    LocalProject("internal"),
    LocalProject("component"),
    LocalProject("analyticsApi"),
    LocalProject("analyticsInternal"),
    LocalProject("main"),
    LocalProject("api"),
    LocalProject("authoringInternal"),
    LocalProject("cdn"),
    LocalProject("coursewareInternal"),
    LocalProject("integration"),
    LocalProject("overlordInternal"),
    LocalProject("platformInternal"),
    LocalProject("presenceInternal"),
    LocalProject("system"),
    LocalProject("detomcat"),
    LocalProject("scaloi"),
  )
  .enablePlugins(DECommonSettings, DEWebPlugin, DEServerPackagePlugin, DebianPlugin, RpmPlugin, JavaServerAppPackaging, SystemVPlugin)
  .settings(
    maintainer              := "LO <rpm@lo.ventures>",
    packageName             := "detomcat",
    packageSummary          := "DE Tomcat.",
    packageDescription      := """The LO Platform.""",
    Rpm / version           := System
      .getProperty("build.ci.rpmVersion", version.value.replace("-SNAPSHOT", "").replace("-", "_")),
    rpmRelease              := System.getProperty(
      "build.ci.rpmRelease",
      crappyBuildNumber + (if (version.value.contains("-SNAPSHOT")) ".snapshot" else "")
    ),
    Debian / name := "detomcat",
    Debian / version := "1.0.0-" + crappyBuildNumber, // x.y.z-build-aa
    rpmVendor               := "loi",
    rpmRequirements         := Nil,                         // Seq("java-11-amazon-corretto")
    rpmUrl                  := Some("https://lo.ventures/"),
    rpmLicense              := Some("AGPL-3.0"),
    daemonUser              := "detomcat",                  // this doesn't actually override the package name default
    daemonGroup             := "detomcat",                  // this doesn't actually override the package name default
    executableScriptName    := "detomcat.sh",
    scriptClasspath         := "" +: scriptClasspath.value, // add lib/ as a classpath entry
    Universal / javaOptions := Seq(
      "-Dconfig.file=/etc/detomcat/detomcat.conf",
      "-Djava.util.logging.config.file=/etc/detomcat/logging.properties",
      "-J--add-opens",
      "-Jjava.base/java.lang=ALL-UNNAMED", // for ancient jaxb in the lti outcomes parsers
    ),
    Rpm / serviceAutostart  := false,
    // default is List(LinuxSymlink(/usr/bin/detomcat.sh,/usr/share/detomcat/bin/detomcat.sh), LinuxSymlink(/etc/detomcat,/usr/share/detomcat/conf), LinuxSymlink(/usr/share/detomcat/logs,/var/log/detomcat))
    // but the bogus sbt-native-packager scripts remove the symlinks after upgrade and installs them destructively up front
    linuxPackageSymlinks    := List.empty,
    Rpm / maintainerScripts := maintainerScriptsPrepend((Rpm / maintainerScripts).value)(
      RpmConstants.Postun -> "restartService() { true ; }" // disable the auto restart.. TODO: PR to sbt-native-packager if autostart is off
    ),
    addCommandAlias("up", "deploy/killAll; deploy/waitOneSec; deploy/bgRun"),
    addCommandAlias("down", "deploy/killAll")
  )
  .settings(
    normalizedName     := "deploy",
    name               := "CPXP Deploy",
    libraryDependencies ++= Seq(
      Cloud.AWS.core,
      // needed by hibernate-validator
      Logging.Slf4j.api,
      Logging.log4jCore,
      Databases.hikaricp,
      Databases.postgresql,
    ),
    frontendArchiveDir := None,
    libraryDependencies ++= {
      val version = Keys.version.value
      if (
        frontendArchiveDir.value.isEmpty && (System.getenv("PWD") ne null) && (System.getenv("CI") eq null)
      ) // hack for interactive shell and not intellij
        List(
          "lo.ventures" % "authoring"  % version,
          "lo.ventures" % "courseware" % version,
          "lo.ventures" % "platform"   % version,
        ) map (_.intransitive)
      else Nil
    },
    dependencyUpdatesFilter -= moduleFilter(organization = "lo.ventures"),
    frontendArchives   := frontendArchiveDir.value.toList
      .flatMap(dir => (dir * ("*.zip" || "*.jar")).get),
    Universal / mappings ++= frontendArchives.value.map {
      // there is a good way to do this, and a bad way. we choose the bad.
      f => f -> s"lib/${f.getName}"
    },
    // you need this as well so that the above get noticed
    scriptClasspath ++= frontendArchives.value.map(_.getName),
    libraryDependencies ++= Seq(
      // There's probably a better place to depend on this.
      JavaEE.mailImpl
    ),
    // fileDescriptorLimit := Some("65536")
    javaOptions += s"-Dcpxp.home=${(ThisBuild / baseDirectory).value}",
)

lazy val webapp = project
  .enablePlugins(DECommonSettings)
  .settings(
    Compile / unmanagedResourceDirectories +=
      sourceDirectory.value / "main" / "webapp",
  )

// 10 minute intervals since recently
lazy val crappyBuildNumber = (System.currentTimeMillis - 1475000000000L) / 60000L

// support for hacking in a prefix to the stock maintainer scripts
// to see what the rpm scripts actually are: rpm -qp --scripts /path/to/some.rpm  | less

def maintainerScriptsPrepend(
  current: Map[String, Seq[String]] = Map.empty,
  replacements: Seq[(String, String)] = Nil
)(scripts: (String, String)*): Map[String, Seq[String]] = {
  import com.typesafe.sbt.packager.archetypes.TemplateWriter
  val appended = scripts.map { case (key, script) =>
    key -> TemplateWriter.generateScriptFromLines(script +: current.getOrElse(key, Seq.empty), replacements)
  }.toMap
  current ++ appended
}
