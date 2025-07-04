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

package com.learningobjects.cpxp.scala.json

import com.fasterxml.jackson.core.Version
import com.fasterxml.jackson.databind.Module
import com.fasterxml.jackson.databind.Module.SetupContext
import com.fasterxml.jackson.databind.module.SimpleModule

class OptionalFieldModule   extends Module:
  override def getModuleName: String = classOf[OptionalFieldModule].getName

  override def setupModule(context: SetupContext): Unit =
    context.addDeserializers(new OptionalFieldDeserializerWrappers)

  override def version(): Version = new Version(1, 0, 0, "", "com", "learningobjects")
class OptionalFieldModule2  extends SimpleModule
object OptionalFieldModule2 extends OptionalFieldModule2:
  def apply() =
    val mod = new OptionalFieldModule2()
    mod.addDeserializer(classOf[OptionalField[?]], new OptionalFieldDeserializer(null))
    mod
