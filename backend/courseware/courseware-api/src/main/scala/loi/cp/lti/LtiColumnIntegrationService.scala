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

package loi.cp.lti

import com.learningobjects.cpxp.component.annotation.Service
import loi.cp.context.ContextId
import loi.cp.storage.UnstoragingError

/** Support for storaging course section data. */
@Service
trait LtiColumnIntegrationService:

  /** Get the course lti column integrations for a given course section, if it exists. May fail due to unparseable data.
    */
  @throws[UnstoragingError]("if the data are ill-formed")
  def get(course: ContextId): Option[CourseColumnIntegrations]

  /** Gets or creates, locks, refreshes, and modificates the lti column integrations for a given course section. May
    * fail due to unparseable data or under contention.
    */
  @throws[UnstoragingError]("if the data are ill-formed")
  def modify(
    crs: ContextId
  )(mod: Option[CourseColumnIntegrations] => Option[CourseColumnIntegrations]): Option[CourseColumnIntegrations]

  /** Gets or creates, locks, refreshes, and sets the course storage for a given course section. May fail due to
    * unparseable data or under contention.
    */
  @throws[UnstoragingError]("if the data are ill-formed")
  def set(course: ContextId, t: Option[CourseColumnIntegrations]): Unit =
    modify(course)((_: Option[CourseColumnIntegrations]) => t)
end LtiColumnIntegrationService
