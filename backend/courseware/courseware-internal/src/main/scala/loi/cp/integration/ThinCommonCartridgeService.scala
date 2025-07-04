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

package loi.cp.integration

import com.learningobjects.cpxp.component.annotation.Service
import loi.cp.course.CourseComponent

import scala.xml.Elem

@Service
trait ThinCommonCartridgeService:

  def getLoConfig: Elem

  def getLtiXml(library: CourseComponent): Elem

  /** Get Common Cartridge export.
    *
    * @param library
    *   the course or library to export
    * @param modules
    *   if true then also include modules in the links, assuming assignment and grade services
    * @return
    *   the CC xml
    */
  def getThinCommonCartridgeConfiguration(library: CourseComponent, modules: Boolean): Elem
end ThinCommonCartridgeService
