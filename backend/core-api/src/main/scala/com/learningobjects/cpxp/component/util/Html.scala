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

package com.learningobjects.cpxp.component.util

import java.util.List as jList

import com.learningobjects.cpxp.component.{ComponentInterface, HtmlWriter}

import scala.jdk.CollectionConverters.*

/** Something that dynamical renders to a html writer. */
trait Renderable:
  @throws(classOf[Exception])
  def render(out: HtmlWriter): Unit

/** This type represents server-side HTML content. */
sealed trait Html

/** No html content. */
case object EmptyHtml extends Html:
  val instance = EmptyHtml

/** Raw html content, not to be escaped. */
case class RawHtml(html: String) extends Html

/** A sequence of html contents. */
case class HtmlSequence(htmls: Seq[Html]) extends Html

object HtmlSequence:
  def apply(htmls: jList[Html]) = new HtmlSequence(htmls.asScala.toList)

/** Dynamically-generated html. Principally the tech formerly known as lojava. */
case class DynamicHtml(renderable: Renderable) extends Html

/** Template-rendered html. */
case class HtmlTemplate(
  context: AnyRef,
  component: Option[Class[? <: ComponentInterface]],
  template: String,
  fragment: Boolean,
  bindings: Map[String, AnyRef]
) extends Html:
  def bind(key: String, value: AnyRef): HtmlTemplate =
    copy(bindings = bindings + (key -> value))

  def bind(kv: (String, AnyRef)*): HtmlTemplate = copy(bindings = bindings ++ kv)

  def bindAll(map: Map[String, AnyRef]): HtmlTemplate =
    copy(bindings = bindings ++ map)
end HtmlTemplate

object HtmlTemplate:
  def apply(context: AnyRef, template: String) =
    new HtmlTemplate(context, None, template, true, Map.empty)

  def apply(context: AnyRef, ci: Class[? <: ComponentInterface], template: String) =
    new HtmlTemplate(context, Some(ci), template, true, Map.empty)
