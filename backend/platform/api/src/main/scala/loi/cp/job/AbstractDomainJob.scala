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

package loi.cp.job

abstract class AbstractDomainJob[J <: DomainJob[J]] extends AbstractEmailJob[J] with DomainJob[J]:
  this: J =>

  val self: DomainJobFacade

  override def update(job: J): J =
    self.setDomainIds(job.getDomainIds.filter(_.nonEmpty))
    super.update(job)

  override def getDomainIds: List[String] = self.getDomainIds
end AbstractDomainJob
