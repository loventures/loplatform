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

import com.typesafe.sbt.packager.archetypes.JavaServerAppPackaging
import com.typesafe.sbt.packager.universal.UniversalPlugin.autoImport.Universal
import sbt.Keys._
import sbt.{Def, _}

/** Created by zpowers on 8/17/15.
  */
object DEServerPackagePlugin extends AutoPlugin {
  override def requires: Plugins      = DEWebPlugin && JavaServerAppPackaging
  override def trigger: PluginTrigger = noTrigger

  override def projectSettings: Seq[Def.Setting[_]] = Seq(
    Universal / javaOptions ++= Seq(
      "-J-ea",
      "-J-server",
      "-J-Xmx2G",
      "--sslPort 8181",
      "--ssl true",
      "-x context.xml",
      "-j tomcat"
      // s"-w ${warMapping.value._2}"
    )
  )
}
