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

package loi.cp.analytics.bus

import cats.syntax.option.*
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.learningobjects.de.authorization.Secured
import loi.cp.config.JsonSchema.*
import loi.cp.config.{ConfigurationKey, ConfigurationKeyBinding}
import loi.cp.i18n.Translatable
import loi.cp.i18n.Translatable.Any
import loi.cp.overlord.OverlordRight
import scalaz.\/
import scalaz.syntax.either.*

@JsonIgnoreProperties(ignoreUnknown = true)
private[analytics] case class AnalyticsBusSettings(
  pollRate: Int
)

private[analytics] object AnalyticsBusSettings:
  import Translatable.RawStrings.*

  val key: Key.type = Key

  @ConfigurationKeyBinding(
    value = "analyticsbus",
    read = new Secured(Array(classOf[OverlordRight])),
    write = new Secured(Array(classOf[OverlordRight]))
  )
  object Key extends ConfigurationKey[AnalyticsBusSettings]:
    override final val schema = Schema(
      title = "Analytics Bus".some,
      properties = List(
        NumberField("pollRate", description = Some("The poll rate (in seconds)."))
      )
    )

    /** An initial value to be used for previously-unconfigured entities. */
    override val init: AnalyticsBusSettings = AnalyticsBusSettings(240)

    /** Validate that `d` conforms to some manner of restriction. */
    override def validate(d: AnalyticsBusSettings): \/[Any, Unit] =
      if d.pollRate < 5 || d.pollRate > 3600 then Translatable.Any(s"This value must be between 5 and 3600").left
      else ().right
  end Key
end AnalyticsBusSettings
