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

package loi.cp.domain

import com.learningobjects.cpxp.component.annotation.ItemMapping
import com.learningobjects.cpxp.service.domain.DomainConstants
import com.learningobjects.de.group.GroupComponent

/** Until we come up with a unified domain component, this component interface declares itself as the stock item mapping
  * for domains in order that we can look up the domain as a non-decorator component. This allows us to
  * component-dereference enrollments whether group or domain.
  */
@ItemMapping(value = DomainConstants.ITEM_TYPE_DOMAIN, singleton = true)
trait GrpDomainComponent extends GroupComponent
