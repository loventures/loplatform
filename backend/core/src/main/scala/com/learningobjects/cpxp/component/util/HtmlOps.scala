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

import com.learningobjects.cpxp.BaseWebContext
import com.learningobjects.cpxp.component.*
import com.learningobjects.cpxp.component.template.{LohtmlTemplate, RenderContext}
import com.learningobjects.cpxp.util.{FormattingUtils, GuidUtil}
import org.apache.commons.lang3.exception.ExceptionUtils

import java.io.StringWriter
import scala.jdk.CollectionConverters.*

object HtmlOps:
  def render(html: Html, out: HtmlWriter): Unit = html match
    case EmptyHtml    =>
    case RawHtml(raw) => out.raw(raw)

    case DynamicHtml(renderable) => renderable.render(out)

    case HtmlSequence(htmls) => htmls.foreach(html => render(html, out))

    case template: HtmlTemplate => // -> htmltemplateutils?
      val scope     = template.context match
        case ci: ComponentInterface =>
          Option(ci.getComponentInstance).map(_.getComponent).orNull
        case _                      => null
      val component = template.component
        .map(c => ComponentSupport.getComponentDescriptor(c))
        .getOrElse(scope)
      val context   = new RenderContext(scope, template.context, out, template.bindings.asJava, null)
      context
        .setBinding("$$component", component.getIdentifier) // TODO: KILLME
      val url = component.getResource(template.template)
      if url == null then
        throw new RuntimeException(
          s"Unable to locate template file for $template (in scope $scope)"
        )
      try
        if template.fragment then LohtmlTemplate.getInstance(url).renderFragment(context)
        else LohtmlTemplate.getInstance(url).render(context)
      catch
        case ex: Exception =>
          throw new RuntimeException("Template error: " + template.template, ex)

  // TODO: tidy up when we drop the context html writer
  def render(html: Html): String =
    try
      val sw: StringWriter = new StringWriter
      val out: HtmlWriter  = new BaseHtmlWriter(sw)
      try
        val old: HtmlWriter = BaseWebContext.getContext.setHtmlWriter(out)
        try
          render(html, out)
          out.close()
        finally
          BaseWebContext.getContext.setHtmlWriter(old)
          ()
      catch
        case th: Throwable =>
          val index: Int =
            ExceptionUtils.indexOfType(th, classOf[UserException])
          if index >= 0 then
            val ue: UserException = ExceptionUtils
              .getThrowables(th)(index)
              .asInstanceOf[UserException]
            out.close()
            sw.append(
              "<div><strong>" + FormattingUtils
                .attr(ue.getTitle) + ":</strong> " + FormattingUtils.attr(ue.getMessage) + "</div>"
            )
          else
            val guid: String = GuidUtil.errorGuid
            out.close()
            sw.append("<div><strong>Component Error: " + guid + "</strong></div>")
          end if
      end try
      sw.toString
    catch
      case ex: Exception =>
        throw ex;
end HtmlOps
