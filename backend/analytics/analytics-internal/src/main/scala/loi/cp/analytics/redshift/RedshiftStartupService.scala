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
import com.learningobjects.cpxp.component.annotation.Service
import com.typesafe.config.ConfigFactory
import doobie.implicits.*
import loi.cp.analytics.bus.{AnalyticBusConfiguration, AnalyticBusService, AnalyticBusState}
import loi.cp.bootstrap.Bootstrap
import loi.db.Redshift
import loi.doobie.log.*
import loi.typesafe.config.syntax.config.loiTypesafeConfigSyntaxConfig
import org.apache.commons.lang3.StringUtils

@Service
class RedshiftStartupService(
  analyticBusService: AnalyticBusService,
  redshiftSchemaService: RedshiftSchemaService,
):
  private implicit final val logger: org.log4s.Logger = org.log4s.getLogger

  /** Creates a Redshift schema if it doesn't already exist, and creates a Redshift analytic bus to populate the schema.
    */
  @Bootstrap("analytics.redshift.initializeEtl")
  def initializeEtl(config: InitializeEtlConfig): Unit = initializeEtl(config.redshiftSchemaName)

  // exists for the convenience of sys/script callers
  def initializeEtl(schemaName: String): Unit =

    if StringUtils.isEmpty(schemaName) then ()
    else
      val xa = Redshift.buildTransactor(useBusUser = false)

      val existingNames = redshiftSchemaService.queryAllSchemaNames(xa)
      if !existingNames.contains(schemaName) then
        val config = ConfigFactory.load().getConfig("de.databases.redshift")
        RedshiftSchema.createAll(schemaName, config.getOptionString("busUser.user")).transact(xa).unsafeRunSync()

      analyticBusService.createBus(
        RedshiftEventSender.RedshiftEventSender,
        AnalyticBusState.Active,
        AnalyticBusConfiguration(maxEvents = 5000, schemaName = schemaName)
      )
end RedshiftStartupService

case class InitializeEtlConfig(redshiftSchemaName: String)
