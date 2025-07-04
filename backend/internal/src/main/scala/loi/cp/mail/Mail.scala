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

package loi.cp.mail

import java.util.Properties

import cats.effect.Sync
import javax.mail.{Authenticator, PasswordAuthentication, Session}
import com.typesafe.config.Config

import scala.jdk.CollectionConverters.*

sealed trait Mail[F[_]]:
  def newSession: F[Session]
object Mail:
  def fromConfig[F[_]: Sync](config: Config): Mail[F] = new Mail[F]:
    override def newSession: F[Session]                                  =
      val mailConfig = config.getConfig("com.learningobjects.cpxp.mail")
      val props      = toProperties(mailConfig, "mail.")
      val user       = mailConfig.getString("smtp.user")
      val password   = mailConfig.getString("smtp.password")
      val creds      = new PasswordAuthentication(user, password)
      val auth       = new Authenticator:
        override def getPasswordAuthentication: PasswordAuthentication =
          creds
      Sync[F].delay(Session.getInstance(props, auth))
    end newSession
    private def toProperties(config: Config, prefix: String): Properties =
      val props = new Properties()
      config.entrySet().asScala.foreach(entry => props.put(prefix + entry.getKey, config.getString(entry.getKey)))
      props
end Mail
