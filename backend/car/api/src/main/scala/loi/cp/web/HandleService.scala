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

package loi.cp.web

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.scala.cpxp.PK

/** Service that can mask and unmask PKs. Use this to provide public APIs that provide some degree of protection against
  * just trawling through our predictable PK space.
  */
@Service
trait HandleService:

  /** Mask a PK. Returns a longer string. */
  def mask[A: PK](id: A): String = maskId(PK[A].pk(id))

  /** Mask a numeric id. Returns a longer string. */
  def maskId(id: Long): String

  /** Unmask a masked PK. Returns the PK if it decodes. */
  def unmask(mid: String): Option[Long]
end HandleService
