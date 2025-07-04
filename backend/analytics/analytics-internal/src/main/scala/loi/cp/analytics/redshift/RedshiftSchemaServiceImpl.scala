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

package loi.cp.analytics.redshift

import cats.effect.unsafe.implicits.global
import cats.effect.IO
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.service.component.misc.AnalyticConstants
import com.learningobjects.cpxp.service.data.DataTypes
import com.learningobjects.cpxp.service.domain.{DomainConstants, DomainState}
import com.learningobjects.cpxp.service.item.ItemService
import com.learningobjects.cpxp.service.query.{Comparison, QueryService}
import doobie.*
import doobie.implicits.*
import loi.cp.analytics.bus.{AnalyticBusFacade, AnalyticBusService}
import loi.db.Redshift
import loi.doobie.log.*

import scala.util.{Failure, Success, Try}

@Service
class RedshiftSchemaServiceImpl(queryService: QueryService, analyticBusService: AnalyticBusService)(implicit
  itemService: ItemService
) extends RedshiftSchemaService:

  private implicit val log: org.log4s.Logger = org.log4s.getLogger

  override def queryEtlSchemaNames(): List[String] =
    analyticBusService
      .queryBuses()
      .addCondition(
        AnalyticConstants.DATA_TYPE_ANALYTIC_BUS_SENDER_IDENTIFIER,
        Comparison.eq,
        RedshiftEventSender.RedshiftEventSender
      )
      .getFacades[AnalyticBusFacade]
      .map(_.getConfiguration.schemaName)
      .toList

  override def queryAllSchemaNames(xa: Transactor[IO]): List[String] =
    sql"select * from pg_namespace where nspowner != 1 order by nspname"
      .query[String]
      .to[List]
      .transact(xa)
      .unsafeRunSync()

  // note well: if `upgrade` contains a `CREATE SCHEMA` then it also must contain a
  // `GRANT ALL ON SCHEMA "schema" TO USER "bus user"`
  def upgradeAll(upgrade: ConnectionIO[Unit]): Unit =

    val xa0 = Redshift.buildTransactor(useBusUser = false)

    // .queryAllDomains already includes "del is null" predicate
    val normalDomains = queryService
      .queryAllDomains(DomainConstants.ITEM_TYPE_DOMAIN)
      .addCondition(DomainConstants.DATA_TYPE_DOMAIN_STATE, Comparison.eq, DomainState.Normal)

    queryService
      .queryAllDomains(AnalyticConstants.ITEM_TYPE_ANALYTIC_BUS)
      .addJoinQuery(DataTypes.META_DATA_TYPE_ROOT_ID, normalDomains)
      .addCondition(
        AnalyticConstants.DATA_TYPE_ANALYTIC_BUS_SENDER_IDENTIFIER,
        Comparison.eq,
        RedshiftEventSender.RedshiftEventSender
      )
      .getFacades[AnalyticBusFacade]
      .foreach(bus =>
        val schemaName = bus.getConfiguration.schemaName
        log.info(s"upgrading bus: ${bus.getId}; schema: $schemaName")
        val xa         = Redshift.configureSession(schemaName)(xa0)
        val attempt    = Try(upgrade.transact(xa).unsafeRunSync())
        attempt match
          case Success(_)  => log.info(s"upgraded bus: ${bus.getId}; schema: $schemaName")
          case Failure(ex) => log.warn(ex)(s"failed to upgrade bus: ${bus.getId}; schema: $schemaName")
      )
  end upgradeAll
end RedshiftSchemaServiceImpl
