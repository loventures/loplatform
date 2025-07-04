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

package com.learningobjects.cpxp.hibernate

import com.learningobjects.cpxp.postgresql.{ArgonautUserType, JsonNodeUserType}
import org.hibernate.boot.MetadataBuilder
import org.hibernate.boot.registry.StandardServiceRegistry
import org.hibernate.boot.spi.MetadataBuilderInitializer

/** Contributes information about custom user types to the hibernate metadata initialization process. We do this because
  * otherwise Hibernate suffers classloader issues during local development.
  */
class CpxpMetadataBuilderInitializer extends MetadataBuilderInitializer:
  override def contribute(metadataBuilder: MetadataBuilder, serviceRegistry: StandardServiceRegistry): Unit =
    // these are for when you use .setParameter(..., someJsonNode), hibernate looks
    // the type up by someJsonNode.getClass.getName
    metadataBuilder.applyBasicType(
      new JsonNodeUserType,
      classOf[com.fasterxml.jackson.databind.node.ArrayNode].getName,
      classOf[com.fasterxml.jackson.databind.node.ObjectNode].getName,
      classOf[com.fasterxml.jackson.databind.node.DoubleNode].getName,
      classOf[com.fasterxml.jackson.databind.node.FloatNode].getName,
      classOf[com.fasterxml.jackson.databind.node.IntNode].getName,
      classOf[com.fasterxml.jackson.databind.node.BigIntegerNode].getName,
      classOf[com.fasterxml.jackson.databind.node.DecimalNode].getName,
      classOf[com.fasterxml.jackson.databind.node.ShortNode].getName,
      classOf[com.fasterxml.jackson.databind.node.LongNode].getName,
      classOf[com.fasterxml.jackson.databind.node.NullNode].getName,
      classOf[com.fasterxml.jackson.databind.node.MissingNode].getName,
      classOf[com.fasterxml.jackson.databind.node.BooleanNode].getName,
      classOf[com.fasterxml.jackson.databind.node.TextNode].getName,
      classOf[com.fasterxml.jackson.databind.node.POJONode].getName,
      classOf[com.fasterxml.jackson.databind.node.BinaryNode].getName,
    )
    metadataBuilder.applyBasicType(
      new ArgonautUserType,
      classOf[argonaut.Json].getName,
    )
  end contribute
end CpxpMetadataBuilderInitializer
