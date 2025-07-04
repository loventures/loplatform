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

package loi.authoring.branch.web

import com.google.common.net.MediaType
import com.helger.css.ECSSVersion
import com.helger.css.decl.visit.{AbstractModifyingCSSUrlVisitor, CSSVisitor, DefaultCSSVisitor}
import com.helger.css.decl.{CSSSelectorSimpleMember, CSSStyleRule, ECSSSelectorCombinator}
import com.helger.css.reader.CSSReader
import com.helger.css.writer.CSSWriter
import com.learningobjects.cpxp.component.annotation.{Component, Controller, PathVariable, RequestMapping}
import com.learningobjects.cpxp.component.web.{ApiRootComponent, Method, TextResponse}
import com.learningobjects.cpxp.component.{BaseComponent, ComponentInstance}
import com.learningobjects.de.authorization.Secured
import jakarta.servlet.http.HttpServletResponse.SC_OK
import loi.asset.course.model.Course
import loi.asset.platform.cache.ValkeyLoadingCache
import loi.authoring.node.AssetNodeService
import loi.authoring.security.right.AccessAuthoringAppRight
import loi.authoring.web.AuthoringWebUtils
import org.apache.commons.io.IOUtils
import scaloi.misc.TimeSource

import java.net.URI
import java.nio.charset.StandardCharsets
import scala.concurrent.duration.*
import scala.jdk.CollectionConverters.*

/** Controller that can serve CSS defaults rewritten to be scoped to a summernote editor (!)
  */
@Component
@Controller(root = true)
class BranchCssWebController(
  ci: ComponentInstance,
  authoringWebUtils: AuthoringWebUtils,
  nodeService: AssetNodeService,
  now: TimeSource,
  cache: ValkeyLoadingCache[String]
) extends BaseComponent(ci)
    with ApiRootComponent:
  import BranchCssWebController.*

  @RequestMapping(path = "authoring/branches/{branch}/defaults", method = Method.GET, csrf = false)
  @Secured(Array(classOf[AccessAuthoringAppRight]))
  def defaultCss(
    @PathVariable("branch") branchId: Long,
  ): TextResponse =
    val workspace = authoringWebUtils.workspaceOrThrow404(branchId, cache = false)
    val course    = nodeService.loadA[Course](workspace).byName(workspace.homeName).get

    val key = s"defaults://$branchId/${course.data.defaultCss.hashCode}-${Version}.css"
    val css = cache.getOrLoad(key, () => downloadAndScopeStylesheets(course.data.defaultCss).mkString("\n"))
    TextResponse(css, MediaType.CSS_UTF_8, SC_OK).cached(now.instant, 1.hour)
  end defaultCss

  def downloadAndScopeStylesheets(urls: Seq[String]): Seq[String] =
    for
      url       <- urls
      uri        = new URI(url)
      stylesheet = IOUtils.toString(uri, StandardCharsets.UTF_8)
      // Comments break the (advanced, version 6.5) CSS parser so strip them but keep newlines for tracking errors
      noComments = CommentRE.replaceAllIn(stylesheet, m => m.matched.filter(_ == '\n'))
      aCSS      <- Option(CSSReader.readFromString(noComments, StandardCharsets.UTF_8, ECSSVersion.CSS30))
    yield
      CSSVisitor.visitCSSUrl(aCSS, new ResolveURIs(uri))
      CSSVisitor.visitCSS(aCSS, ScopeCssRules)

      new CSSWriter().getCSSAsString(aCSS)
end BranchCssWebController

object BranchCssWebController:
  final val CommentRE = "(?s)/\\*.*?\\*/".r
  final val Version   = "8"

  private class ResolveURIs(uri: URI) extends AbstractModifyingCSSUrlVisitor:
    override def getModifiedURI(sURI: String): String =
      if sURI.contains("//") then sURI else uri.resolve(sURI).toString

  // The HTML winds up so:
  // div.default-styling
  //   div.note-editing-area.lesson-chapter
  //     div.note-editable.main-paragraph-text
  //       p
  // p { x } -> .default-styling .note-editable p
  // .lesson-chapter .foo -> .default-styling .note-editable .lesson-chapter .foo

  private object ScopeCssRules extends DefaultCSSVisitor:
    // foo bar { baz } becomes .default-styling .note-editable foo bar { baz }
    // body.foo bar { baz } becomes body.foo .default-styling .note-editable bar { baz }
    override def onBeginStyleRule(aStyleRule: CSSStyleRule): Unit =
      aStyleRule.getAllSelectors.asScala foreach { selector =>
        val first    = selector.getMemberAtIndex(0).getAsCSSString
        val topLevel = first == "body" || first == "html"
        var index    = 0 // shame, misery
        if topLevel then
          // skip all the selectors applied directly to the body; e.g. body.foo.bar
          while index < selector.getMemberCount - 1 && !selector
              .getMemberAtIndex(1 + index)
              .isInstanceOf[ECSSSelectorCombinator]
          do index += 1
          if selector.getMemberCount == index + 1 then // plain body rule
            selector.addMember(index + 1, ECSSSelectorCombinator.BLANK)
          index += 2
        val next     = Option(selector.getMemberAtIndex(index))
        selector.addMember(index + 0, new CSSSelectorSimpleMember(".default-styling"))
        selector.addMember(index + 1, ECSSSelectorCombinator.BLANK)
        selector.addMember(index + 2, new CSSSelectorSimpleMember(".note-editable"))
        if !topLevel then selector.addMember(index + 3, ECSSSelectorCombinator.BLANK)
      }
  end ScopeCssRules
end BranchCssWebController
