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

package loi.asset.platform.cache

import com.learningobjects.cpxp.ServiceMeta
import com.learningobjects.cpxp.component.annotation.Service
import com.redis.api.StringApi
import com.redis.serialization.{Format, Parse}
import com.redis.{RedisClient, RedisClientPool}
import com.typesafe.config.Config
import scalaz.syntax.std.boolean.*
import scaloi.syntax.ʈry.*
import scala.concurrent.duration.*

import java.net.SocketTimeoutException
import javax.net.ssl.SSLContext
import scala.util.{Failure, Success, Try}

trait LoadingCache[V]:

  def get(key: String)(implicit formatter: ValkeyCodec[V]): Option[V]

  def getOrLoad(key: String, load: () => V)(implicit formatter: ValkeyCodec[V]): V

  def put(key: String, value: V)(implicit formatter: ValkeyCodec[V]): Boolean

  /** Deletes entries with the given keys. A key is ignored if it does not exist.
    *
    * @param key
    *   :: rest the keys to delete
    * @return
    *   the number of entries deleted
    */
  def delete(key: String, rest: String*): Long
end LoadingCache

trait ValkeyCodec[A]:
  def format: Format
  def parse: Parse[A]

object ValkeyCodec:
  implicit val StringByteStringFormatter: ValkeyCodec[String] = new ValkeyCodec[String]:
    override val format: Format       = Format.default
    override val parse: Parse[String] = Parse.Implicits.parseString

@Service(unique = true)
class ValkeyLoadingCache[V](config: Config, serviceMeta: ServiceMeta) extends LoadingCache[V]:

  import ValkeyLoadingCache.*

  private val valkeyConfig = config.getConfig("valkey")

  private val isEnabled = valkeyConfig.getBoolean("isEnabled")

  private val timeout = valkeyConfig.getDuration("timeout").toMillis

  private val maxConnections = valkeyConfig.getInt("maxConnections")

  private val tlsEnabled = valkeyConfig.getBoolean("tls")

  private val poolTry = connectAndValidate

  override def get(key: String)(implicit formatter: ValkeyCodec[V]): Option[V] =
    attemptValkeyOperation(_.get(key)(using formatter.format, formatter.parse), None)

  override def getOrLoad(key: String, load: () => V)(implicit
    formatter: ValkeyCodec[V]
  ): V =
    get(key).getOrElse({
      val loaded = load()
      if !put(key, loaded) then logger.warn(s"Failed to cache $key")
      loaded
    })

  override def put(key: String, value: V)(implicit formatter: ValkeyCodec[V]): Boolean =
    attemptValkeyOperation(_.set(key, value, StringApi.Always, expiration)(using formatter.format), false)

  override def delete(key: String, rest: String*): Long = attemptValkeyOperation(_.del(key, rest*).getOrElse(0L), 0L)

  def flushDb(): Boolean = attemptValkeyOperation(_.flushdb, false)

  protected def attemptValkeyOperation[T](action: RedisClient => T, defaultValue: => T): T =
    poolTry
      .map(_.withClient(action))
      .tapFailure({
        case ValkeyUnavailable         =>
        case _: SocketTimeoutException =>
          logger.info(s"Timeout performing Valkey operation after $timeout")
          defaultValue
        case th                        =>
          logger.warn(th)("Error performing Valkey operation")
      })
      .getOrElse(defaultValue)

  private def newClient: RedisClientPool =
    new RedisClientPool(
      valkeyConfig.getString("host"),
      valkeyConfig.getInt("port"),
      database = valkeyConfig.getInt("db"),
      timeout = timeout.toInt,
      maxConnections = maxConnections,
      sslContext = Option.when(tlsEnabled)(SSLContext.getDefault)
    )

  private def connectAndValidate: Try[RedisClientPool] =
    isEnabled.fold(Success(newClient), Failure(ValkeyUnavailable)).flatMap(validateClient)

  private def validateClient(pool: RedisClientPool): Try[RedisClientPool] =
    Try {
      logger.info(s"Connecting to Valkey at ${pool.host}:${pool.port}#${pool.database}")
      pool.withClient { client =>
        client.connect // so side-effect
        val info = client.info.getOrElse("<no info>")
        logger.info(s"Valkey info: $info")
      }
      pool
    } mapExceptions { case th =>
      logger.warn(th)("Error connecting to Valkey")
      if serviceMeta.isProdLike then logger.error("Valkey is not functioning in production!")
      pool.close()
      ValkeyUnavailable
    }
end ValkeyLoadingCache

object ValkeyLoadingCache:
  private final val logger = org.log4s.getLogger

  private final val expiration = 30.days // we filled 32G in 4mo

  private object ValkeyUnavailable extends Exception
