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

package loi.cp.demon

import com.learningobjects.cpxp.component.annotation.{Controller, RequestMapping}
import com.learningobjects.cpxp.component.query.{ApiQuery, ApiQueryResults}
import com.learningobjects.cpxp.component.web.{ApiRootComponent, Method}
import com.learningobjects.de.authorization.Secured
import com.learningobjects.de.web.Queryable as SRSQueryable
import loi.cp.admin.right.AdminRight

import scala.annotation.meta.beanGetter
import scala.beans.BeanProperty

/** DE monitoring statistics root API.
  */
@Controller(value = "demon", root = true)
@RequestMapping(path = "demon")
@Secured(Array(classOf[AdminRight]))
trait DeMonRootApi extends ApiRootComponent:
  import DeMonRootApi.*

  /** Get the SQL monitoring statistics. */
  @RequestMapping(path = "statistics", method = Method.GET)
  def getStatistics(q: ApiQuery): ApiQueryResults[DeMonStat]

  /** Reset the SQL monitoring statistics. */
  @RequestMapping(path = "statistics", method = Method.DELETE)
  def resetStatistics(): Unit
end DeMonRootApi

/** Monitoring root API companion. */
object DeMonRootApi:

  /** Queryable bean getter. */
  type Queryable = SRSQueryable @beanGetter

  /** Statistic POJO. */
  case class DeMonStat(
    @Queryable @BeanProperty statistic: String,
    @Queryable @BeanProperty entity: String,
    @Queryable @BeanProperty count: Int,
    @Queryable @BeanProperty rate: Double,
    @Queryable @BeanProperty duration: Long,
    @Queryable @BeanProperty avgDuration: Double
  )
end DeMonRootApi
