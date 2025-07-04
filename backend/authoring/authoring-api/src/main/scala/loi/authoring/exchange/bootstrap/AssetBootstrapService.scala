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

package loi.authoring.exchange.bootstrap

import com.learningobjects.cpxp.component.annotation.Service

@Service
trait AssetBootstrapService:

  // Called by reflection because the implementation has a `@Bootstrap`, so conceptually
  // this is part of authoring public API
  def doImport(bootstrapDto: BootstrapImportDto): Unit

  // Called by reflection because the implementation has a `@Bootstrap`, so conceptually
  // this is part of authoring public API
  def renameCourse(config: RenameProgramCourseDto): Unit
end AssetBootstrapService
