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

package loi.cp.user

import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.query.{ApiQuery, PredicateOperator}
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance, ComponentService}
import com.learningobjects.cpxp.scala.cpxp.Component.*
import com.learningobjects.cpxp.service.facade.FacadeService
import loi.cp.web.HandleService
import scaloi.syntax.CollectionBoxOps.*

import scala.jdk.CollectionConverters.*

/** Implementation of the global user profile API. With deliberate intent, this class only supports user lookup by
  * opaque handle. This prevents people walking our PK space and scraping users.
  */
@Component
class ProfileRootApiImpl(val componentInstance: ComponentInstance)(implicit
  ms: HandleService,
  fs: FacadeService,
  cs: ComponentService
) extends ProfileRootApi
    with ComponentImplementation:
  override def getProfiles(query: ApiQuery): Seq[Profile] =
    cs.getById(profileIds(query).boxInside().asJava, classOf[Profile]).asScala.toSeq

  override def getProfile(handle: String): Option[Profile] =
    ms.unmask(handle).flatMap(_.tryComponent[Profile])

  /** Extract the PKs of the profiles in any handle prefilter. */
  private def profileIds(query: ApiQuery): Seq[Long] =
    query.getPrefilters.asScala // WARNING: DO NOT ADD SUPPORT FOR ANY NEW FILTERS. FERPA.
      .find(f => (f.getProperty == Profile.HandleProperty) && (f.getOperator == PredicateOperator.IN))
      .fold(Seq.empty[Long])(_.getValue.split(',').toSeq.flatMap(ms.unmask))
end ProfileRootApiImpl
