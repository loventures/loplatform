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

package loi.deploy

import cats.effect.{Clock, Sync}
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import jakarta.persistence.EntityManagerFactory
import jakarta.persistence.spi.PersistenceUnitInfo
import com.learningobjects.cpxp.dto.{BaseOntology, Ontology}
import com.learningobjects.cpxp.util.ManagedUtils
import com.typesafe.config.Config
import loi.db.ConfigPersistenceUnitInfo
import loi.deploy.Benchmark.Bench
import org.hibernate.jpa.boot.spi.Bootstrap as Hibernate
import io.github.classgraph.ScanResult

trait Persistence[F[_]]:
  def persistenceUnitInfo(config: Config): F[PersistenceUnitInfo]
  def ontology(cg: ScanResult): F[Ontology]
  def entityManagerFactory(config: Config, updateSchema: Boolean, pui: PersistenceUnitInfo): F[EntityManagerFactory]

object Persistence:
  private val logger = org.log4s.getLogger

  final class Default[F[_]](implicit F: Sync[F]) extends Persistence[F]:
    override def ontology(cg: ScanResult): F[Ontology] = F.delay {
      BaseOntology.initOntology(cg)
    }

    override def entityManagerFactory(
      config: Config,
      updateSchema: Boolean,
      pui: PersistenceUnitInfo
    ): F[EntityManagerFactory] =
      for
        puii <- updateAutoDDL(pui, updateSchema)
        emf  <- F.delay(
                  Hibernate
                    .getEntityManagerFactoryBuilder(puii, null)
                    .withDataSource(pui.getNonJtaDataSource)
                    .build()
                )
        _    <- F.delay(ManagedUtils.init(emf))
      yield emf

    def persistenceUnitInfo(config: Config): F[PersistenceUnitInfo] = F.delay {
      ConfigPersistenceUnitInfo.fromConfig(config.getConfig("de.databases.underground"))
    }

    /** If the Hibernate auto DDL property is set to "checksum" then update it to either "update" or "none" depending on
      * whether the checksum suggests a DDL update is warranted.
      */
    private def updateAutoDDL(pui: PersistenceUnitInfo, updateSchema: Boolean): F[PersistenceUnitInfo] =
      F.pure {
        pui.getProperties
          .asInstanceOf[java.util.Map[AnyRef, AnyRef]]
          .compute(
            "hibernate.hbm2ddl.auto",
            (_: AnyRef, v: AnyRef) =>
              if v != "checksum" then v
              else if updateSchema then
                logger.info("Performing Hibernate DDL update because schema checksum mismatches")
                "update"
              else
                logger.info("Skipping Hibernate DDL update because schema checksum matches")
                "none"
          )
        pui
      }
  end Default

  final class Benched[F[_]: {Sync, Clock}] extends Persistence[Bench[F, *]]:
    val default                                               = new Default[F]()
    override def ontology(cg: ScanResult): Bench[F, Ontology] = Benchmark(default.ontology(cg), "Ontology")

    override def entityManagerFactory(
      config: Config,
      updateSchema: Boolean,
      pui: PersistenceUnitInfo
    ): Bench[F, EntityManagerFactory] =
      Benchmark(default.entityManagerFactory(config, updateSchema, pui), "JPA")

    override def persistenceUnitInfo(config: Config): Bench[F, PersistenceUnitInfo] =
      Benchmark(default.persistenceUnitInfo(config), "JDBC")
  end Benched
end Persistence
