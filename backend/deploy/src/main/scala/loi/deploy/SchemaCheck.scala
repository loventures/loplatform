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

import java.util.ServiceLoader

import cats.Parallel
import cats.effect.{Clock, Sync}
import scala.annotation.nowarn
import com.learningobjects.cpxp.util.ClassUtils
import doobie.*
import javax.sql.DataSource
import loi.deploy.Benchmark.Bench
import org.hibernate.boot.MetadataSources
import org.hibernate.boot.spi.MetadataSourcesContributor
import io.github.classgraph.ScanResult
import scaloi.syntax.AnyOps.*
import scala.util.Using
import scalaz.syntax.std.boolean.*

import scala.jdk.CollectionConverters.*

trait SchemaCheck[F[_]]:
  // doobie initialisation is in this trait because the implementation used to rely on doobie but that blew our startup time
  def preloadDoobie: F[Any]
  // principles be damned
  def schemaState(ds: DataSource, cg: ScanResult): F[SchemaState]
  def storeChecksum(ds: DataSource, checksum: Long): F[Unit]

case class SchemaState(current: Long, stored: Option[Long]):
  def schemaChanged: Boolean = !stored.contains(current)

object SchemaCheck:

  final class Default[F[_], PF[_]](implicit F: Sync[F], PF: Parallel[F]) extends SchemaCheck[F]:
    override def preloadDoobie: F[Any] = F.delay {
      Meta // in all probability this immediately blocks on the universe above
    }

    override def schemaState(ds: DataSource, cg: ScanResult): F[SchemaState] =
      Parallel.parMap2(currentSchemaVersion(cg), loadSchemaVersion(ds))(SchemaState.apply)

    // refl expresses a shameful logical implicit dependency
    private def currentSchemaVersion(@nowarn cg: ScanResult): F[Long] = F.delay {
      metamodelChecksum
    }

    private def loadSchemaVersion(ds: DataSource): F[Option[Long]] = F.delay {
      Using.resource(ds.getConnection) { conn =>
        Using.resource(conn.createStatement) { stmt =>
          stmt.execute("""
            CREATE TABLE IF NOT EXISTS cpxp_checksum
            (id INTEGER PRIMARY KEY, checksum BIGINT)""")
          val rs = stmt.executeQuery("""
            SELECT checksum FROM cpxp_checksum
            WHERE id = 0""")
          rs.next().option(rs.getLong("checksum"))
        }
      }
    }

    override def storeChecksum(ds: DataSource, checksum: Long) = F.delay {
      Using.resource(ds.getConnection) { conn =>
        Using.resource(conn.createStatement) { stmt =>
          stmt.execute(s"""
            INSERT INTO cpxp_checksum
            VALUES (0, $checksum)
            ON CONFLICT (id)
              DO UPDATE SET checksum = $checksum
              WHERE cpxp_checksum.id = 0""")
        }
      }
    }

    /** Compute a checksum of our metamodel. */
    private def metamodelChecksum: Long =
      Checksum.checksumChecksums(metamodelChecksums.sortBy(_._1).map(_._2))

    /** Generate a list of all our metamodels and their individual checksums. */
    private def metamodelChecksums: List[(String, Int)] =
      entityChecksums ++ sqlChecksums

    /** Generate a list of all our entity classes and their individual checksums. */
    private def entityChecksums: List[(String, Int)] =
      metadataSources.getAnnotatedClasses.asScala.toList map { t =>
        t.getName -> Checksum.checksumClass(t)
      }

    /** Build a metadata sources object populated with our hibernate entities. */
    private def metadataSources: MetadataSources =
      new MetadataSources <| { sources =>
        ServiceLoader.load(classOf[MetadataSourcesContributor]).asScala foreach { contributor =>
          contributor.contribute(sources)
        }
      }

    /** Generate a list of all our sql init files and their individual checksums. */
    private def sqlChecksums: List[(String, Int)] =
      sqlResources map { sql =>
        sql -> Checksum.checksumURL(getClass.getResource(sql))
      }

    /** Get the sql files used to initialize the database. */
    private def sqlResources: List[String] =
      ClassUtils.getResources(getClass, "/postgresql/*.sql").asScala.toList
  end Default

  final class Benched[F[_], PF[_]](implicit F: Sync[F], PF: Parallel[F], clock: Clock[F])
      extends SchemaCheck[Bench[F, *]]:
    val default = new Default[F, PF]()

    override def preloadDoobie: Bench[F, Any] =
      Benchmark(default.preloadDoobie, "Preload doobie")

    override def schemaState(ds: DataSource, cg: ScanResult): Bench[F, SchemaState] =
      Benchmark(default.schemaState(ds, cg), "Get schema state")

    override def storeChecksum(ds: DataSource, checksum: Long): Bench[F, Unit] =
      Benchmark(default.storeChecksum(ds, checksum), "Save schema version")
  end Benched
end SchemaCheck
