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

package loi.cp.config

import scala.collection.SortedMap

object JsonSchema:
  final case class Schema(
    title: Option[String] = None,
    description: Option[String] = None,
    properties: List[Field] = List.empty
  )

  sealed trait Field:
    def name: String
    def title: Option[String]
    def description: Option[String]

  final case class ObjectField(
    name: String,
    title: Option[String] = None,
    description: Option[String] = None,
    properties: List[Field] = List.empty,
    additionalProperties: Option[Field] = None,
  ) extends Field

  final case class BooleanField(name: String, title: Option[String] = None, description: Option[String] = None)
      extends Field

  final case class StringField(name: String, title: Option[String] = None, description: Option[String] = None)
      extends Field

  final case class NumberField(name: String, title: Option[String] = None, description: Option[String] = None)
      extends Field

  final case class ArrayField(
    name: String,
    `type`: String,
    title: Option[String] = None,
    description: Option[String] = None
  ) extends Field

  // This is used to convert "CBLPROD-1234_FOO_BAR" screaming snake case into "Cblprod-1234 Foo Bar"
  // in order to not run afoul of decamel-casing.
  private final val snakeScream = """ ([A-Z]+?)(?:(?= )|$)""".r

  // This is used to convert "fooBarBaz" camel case into "foo Bar Baz" human form.
  private final val camel = "([a-z](?=[A-Z])|[A-Z](?=[A-Z][a-z]))".r

  private def fieldTitle(name: String): String =
    val desnaked =
      snakeScream.replaceAllIn(
        name.capitalize.replace('_', ' '),
        m => s" ${m.group(1).toLowerCase.capitalize}"
      )
    camel.replaceAllIn(desnaked, m => s"${m.matched} ")

  def serialize(schema: Schema, schemaName: String): Map[String, AnyRef] = Map(
    "type"        -> "object",
    "title"       -> schema.title.getOrElse(fieldTitle(schemaName)),
    "description" -> schema.description.orNull,
    "properties"  -> serialize(schema.properties),
  )

  private def serialize(properties: List[Field]): SortedMap[String, AnyRef] =
    SortedMap(properties.map(f => f.name -> serialize(f))*)

  private def serialize(field: Field): Map[String, AnyRef] = field match
    case o: ObjectField  =>
      serialize(field, "object") +
        o.additionalProperties.fold[(String, AnyRef)]("properties" -> serialize(o.properties))(ap =>
          "additionalProperties" -> serialize(ap)
        )
    case _: BooleanField => serialize(field, "boolean")
    case _: StringField  => serialize(field, "string")
    case _: NumberField  => serialize(field, "string") + ("format" -> "number")
    case a: ArrayField   => serialize(field, "array") + ("items"   -> Map("type" -> a.`type`))

  private def serialize(field: Field, tpe: String): Map[String, String] = Map(
    "type"        -> tpe,
    "title"       -> field.title.getOrElse(fieldTitle(field.name)),
    "description" -> field.description.orNull,
  )
end JsonSchema
