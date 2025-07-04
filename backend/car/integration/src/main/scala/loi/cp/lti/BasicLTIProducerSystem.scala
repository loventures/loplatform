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

package loi.cp.lti

import com.fasterxml.jackson.databind.{ObjectMapper, SerializationFeature}
import com.learningobjects.cpxp.component.annotation.Component
import loi.cp.integration.{BasicLtiConfiguration, BasicLtiRoleMapping, BasicLtiSystemComponent}
import scalaz.std.string.*
import scaloi.syntax.OptionOps.*

import java.lang as jl

@Component(name = "$$name=Basic LTI Provider", description = "$$description=Basic LTI Provider System", version = "0.7")
class BasicLTIProducerSystem(
  mapper: ObjectMapper,
) extends AbstractLTIProviderSystem[BasicLtiSystemComponent]
    with BasicLtiSystemComponent:
  override def getUseExternalIdentifier: jl.Boolean =
    Option(_self).map(_.getUseExternalIdentifier).orNull

  override def setUseExternalIdentifier(useExternalIdentifier: jl.Boolean): Unit =
    _self.setUseExternalIdentifier(useExternalIdentifier)

  override def getConfiguration: String =
    mapper
      .writer(SerializationFeature.INDENT_OUTPUT)
      .writeValueAsString(loadBasicLtiConfiguration)

  override def setConfiguration(config: String): Unit =
    _self.setJsonConfig(OptionNZ(config) match
      case Some(cf) => mapper.readValue(cf, classOf[BasicLtiConfiguration])
      case None     => BasicLtiConfiguration.empty)

  override def getRoleMappings: Seq[BasicLtiRoleMapping] =
    Option(loadBasicLtiConfiguration.roleMappings).getOrElse(Nil)

  private lazy val loadBasicLtiConfiguration =
    Option(_self).mapNonNull(_.getJsonConfig(classOf[BasicLtiConfiguration])).getOrElse(BasicLtiConfiguration.empty)

  override def getBasicLtiConfiguration: BasicLtiConfiguration =
    loadBasicLtiConfiguration

  override def update(system: BasicLtiSystemComponent): BasicLtiSystemComponent =
    setUseExternalIdentifier(Option(system.getUseExternalIdentifier).getOrElse(false))
    setConfiguration(system.getConfiguration)
    super.update(system)
end BasicLTIProducerSystem
