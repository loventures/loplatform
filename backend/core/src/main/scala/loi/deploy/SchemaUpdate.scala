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

import cats.Parallel
import cats.effect.{Async, Clock}
import cats.instances.list.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import cats.syntax.parallel.*
import com.learningobjects.cpxp.ServiceMeta
import com.learningobjects.cpxp.dto.Ontology
import com.learningobjects.cpxp.util.ClassUtils
import loi.deploy.Benchmark.Bench
import scala.util.Using

import java.io.InputStream
import java.sql.Connection
import javax.sql.DataSource
import scala.io.Source
import scala.jdk.CollectionConverters.*
import scala.util.control.NonFatal

trait SchemaUpdate[F[_]]:
  def preJPA(ds: DataSource): F[Unit]
  def postJPA(sm: ServiceMeta, ds: DataSource, ontology: Ontology, ddlUpdate: Boolean): F[Unit]
  def addForeignKeys(sm: ServiceMeta, ds: DataSource, newFks: Set[ForeignKey], ddlUpdate: Boolean): F[Int]

object SchemaUpdate:
  private val logger = org.log4s.getLogger

  final class Default[F[_], PF[_]](implicit F: Async[F], PF: Parallel[F]) extends SchemaUpdate[F]:

    override def preJPA(ds: DataSource): F[Unit] =
      for
        _      <- logDatabaseInfo(ds)
        _      <- plpgsql(ds)
        exists <- schemaExists(ds) // don't run pre- scripts if no database schema exists
        _      <- runPreSql(ds, exists)
      yield ()

    private def runPreSql(ds: DataSource, exists: Boolean): F[Unit] =
      for
        files  <- sqlResources
        preSqls = files.filter(if exists then isPreUpdate else isPreCreate)
        _      <- preSqls.parTraverse(executeSqlFile(ds))
      yield ()

    override def postJPA(sm: ServiceMeta, ds: DataSource, ontology: Ontology, ddlUpdate: Boolean): F[Unit] =
      if sm.isDas && ddlUpdate then
        for
          _       <- createIndices(ontology, ds)
          files   <- sqlResources
          postSqls = files.filterNot(f => isPreUpdate(f) || isPreCreate(f))
          _       <- postSqls.parTraverse(executeSqlFile(ds))
        yield ()
      else F.unit

    override def addForeignKeys(
      sm: ServiceMeta,
      ds: DataSource,
      newFks: Set[ForeignKey],
      ddlUpdate: Boolean
    ): F[Int] =
      if sm.isDas && ddlUpdate then
        F.delay {
          Using.resource(ds.getConnection) { conn =>
            conn.setAutoCommit(true)
            val existingFks = selectFks(conn)
            val addFks      = newFks -- existingFks
            if addFks.nonEmpty then
              addFks.foreach(fk => logger.info(fk.alterSql))
              val sql = addFks.map(_.alterSql).mkString("\n")
              Using.resource(conn.createStatement) { stmt =>
                stmt.execute(sql)
              }
            logger.info(s"added ${addFks.size} foreign keys")
            addFks.size
          }
        }
      else F.pure(0)

    private def selectFks(conn: Connection): Set[ForeignKey] =
      Using.resource(conn.createStatement) { stmt =>
        val rs = stmt.executeQuery(SelectAllForeignKeys)
        Iterator
          .continually(rs)
          .takeWhile(_.next())
          .map(rs =>
            val table     = rs.getString("table_name")
            val column    = rs.getString("column_name")
            val refTable  = rs.getString("foreign_table_name")
            val refColumn = rs.getString("foreign_column_name")
            ForeignKey(table, column, refTable, refColumn)
          )
          .toSet
      }

    private def createIndices(ontology: Ontology, ds: DataSource): F[Unit] = F.delay {
      Using.resource(ds.getConnection) { conn =>
        conn.setAutoCommit(true)
        val indices = selectIndices(conn).map(_.index.toLowerCase)
        Using.resource(conn.createStatement) { stmt =>
          val indexStatements = ontology.getEntityDescriptors.values.asScala.toList.flatMap(_.generateIndices.asScala)
          // TODO: We ought to create the indices for different tables in
          // parallel however this deadlocks. Supposedly the deadlock was
          // fixed in PostgreSQL 9.6.7 but we still see the issue.
          indexStatements foreach {
            case index @ CreateIndexStatement(name, table) if !indices.contains(name.toLowerCase) =>
              logger.debug(s"Create index $name on $table")
              stmt.execute(index)
            case CreateIndexStatement(name, table)                                                =>
              logger.debug(s"Skipping index $name on $table")
          }
        }
      }
    }

    private def selectIndices(conn: Connection): Set[TableIndex] =
      Using.resource(conn.createStatement) { stmt =>
        val rs = stmt.executeQuery(SelectAllIndices)
        Iterator
          .continually(rs)
          .takeWhile(_.next())
          .map(rs =>
            val table = rs.getString("table_name")
            val index = rs.getString("index_name")
            TableIndex(table, index)
          )
          .toSet
      }

    private def logDatabaseInfo(ds: DataSource): F[Unit] = F.delay {
      Using.resource(ds.getConnection) { conn =>
        val metadata = conn.getMetaData
        logger.info(s"Database ${metadata.getDatabaseProductName} ${metadata.getDatabaseProductVersion}")
      }
    }

    private def schemaExists(ds: DataSource): F[Boolean] = F.delay {
      Using.resource(ds.getConnection) { conn =>
        Using.resource(conn.createStatement) { stmt =>
          stmt.execute("SELECT * FROM pg_class WHERE relname = 'systeminfo'")
          Using.resource(stmt.getResultSet) { resultSet =>
            resultSet.next
          }
        }
      }
    }

    private def executeSqlFile(ds: DataSource)(resource: String): F[Unit] = F.delay {
      logger.info(s"Executing SQL file: $resource")
      Using.resource(openResource(resource)) { in =>
        val sqls = Source.fromInputStream(in, "UTF-8").mkString
        try
          Using.resource(ds.getConnection) { conn =>
            conn.setAutoCommit(true)
            Using.resource(conn.createStatement) { stmt =>
              // CREATE INDEX CONCURRENTLY cannot be executed within a batch so must be split up
              for
                sql <- if sqls.contains("CREATE INDEX CONCURRENTLY") then sqls.split(';') else Array(sqls)
                if sql.trim.nonEmpty
              do stmt.executeUpdate(sql)
            }
          }
        catch
          case NonFatal(e) =>
            e.printStackTrace()
            logger.warn(e)(s"SQL error: $resource")
        end try
      }
    }

    private def plpgsql(ds: DataSource): F[Unit] = F.delay {
      Using.resource(ds.getConnection) { conn =>
        conn.setAutoCommit(true)
        Using.resource(conn.createStatement) { stmt =>
          if !stmt.execute("SELECT * from pg_language where LOWER(lanname) = 'plpgsql'") then
            throw new RuntimeException("Expected results")
          val hasPlpsql = Using.resource(stmt.getResultSet) { resultSet =>
            resultSet.next
          }
          if !hasPlpsql then stmt.execute("CREATE LANGUAGE plpgsql")
        }
      }
    }

    private def openResource(resource: String): InputStream =
      Option(getClass.getResourceAsStream(resource))
        .getOrElse(throw new RuntimeException(s"Unknown resource: $resource"))

    private def isPreUpdate(resource: String): Boolean = resource.contains("pre-update-")
    private def isPreCreate(resource: String): Boolean = resource.contains("pre-create-")

    private def sqlResources: F[List[String]] = F.delay {
      ClassUtils.getResources(getClass, "/postgresql/*.sql").asScala.toList
    }
  end Default
  object Default:
    def apply[F[_], PF[_]](implicit F: Async[F], PF: Parallel[F]): Default[F, PF] = new Default()

  final class Benched[F[_], PF[_]](implicit F: Async[F], PF: Parallel[F], C: Clock[F])
      extends SchemaUpdate[[X] =>> Bench[F, X]]:
    val default                                                                                                   = Default[F, PF]
    override def preJPA(ds: DataSource): Bench[F, Unit]                                                           = Benchmark(default.preJPA(ds), "Pre-JPA")
    override def postJPA(sm: ServiceMeta, ds: DataSource, ontology: Ontology, ddlUpdate: Boolean): Bench[F, Unit] =
      Benchmark(default.postJPA(sm, ds, ontology, ddlUpdate), "Post-JPA")
    override def addForeignKeys(
      sm: ServiceMeta,
      ds: DataSource,
      newFks: Set[ForeignKey],
      ddlUpdate: Boolean
    ): Bench[F, Int] =
      Benchmark(default.addForeignKeys(sm, ds, newFks, ddlUpdate), "Add Foreign Key Constraints")
  end Benched
  object Benched:
    def apply[F[_], PF[_]](implicit F: Async[F], PF: Parallel[F], C: Clock[F]): Benched[F, PF] = new Benched()

  final val SelectAllForeignKeys =
    """
      |SELECT
      |  tc.table_name,
      |  kcu.column_name,
      |  ccu.table_name as foreign_table_name,
      |  ccu.column_name as foreign_column_name
      |FROM information_schema.table_constraints as tc
      |  JOIN information_schema.key_column_usage as kcu
      |    ON tc.constraint_name = kcu.constraint_name AND tc.table_schema = kcu.table_schema
      |  JOIN information_schema.constraint_column_usage as ccu
      |    ON ccu.constraint_name = tc.constraint_name AND ccu.table_schema = tc.table_schema
      |WHERE tc.constraint_type = 'FOREIGN KEY'
    """.stripMargin

  final val SelectAllIndices =
    """
      |SELECT
      |    t.relname AS table_name,
      |    i.relname AS index_name
      |FROM
      |    pg_class t,
      |    pg_class i,
      |    pg_index ix
      |WHERE
      |    t.oid = ix.indrelid
      |    AND i.oid = ix.indexrelid
      |    AND t.relkind = 'r'
      |""".stripMargin

  private final case class TableIndex(table: String, index: String)

  private final val CreateIndexStatement = "CREATE INDEX (?:CONCURRENTLY )?IF NOT EXISTS (\\S+) ON (\\w+).*".r
end SchemaUpdate
