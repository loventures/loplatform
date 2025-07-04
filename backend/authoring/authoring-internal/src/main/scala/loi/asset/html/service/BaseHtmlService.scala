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

package loi.asset.html.service

import com.learningobjects.cpxp.ServiceMeta
import com.learningobjects.cpxp.component.ComponentSupport
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.component.util.ComponentUtils
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.util.FileInfo
import loi.asset.course.model.Course
import loi.asset.html.model.{Html, Javascript, Stylesheet}
import loi.authoring.asset.Asset
import loi.authoring.blob.{BlobRef, BlobService}
import loi.authoring.configuration.AuthoringConfiguration.CdnMappingRE
import loi.authoring.configuration.AuthoringConfigurationService
import loi.authoring.edge.*
import loi.authoring.index.TextExtractor
import loi.authoring.node.BaseAssetNodeService
import loi.authoring.project.ProjectDao2
import loi.authoring.render.DataIds
import loi.authoring.workspace.{AttachedReadWorkspace, ProjectWorkspace, ReadWorkspace}
import scalaz.std.list.*
import scalaz.std.option.*
import scalaz.std.string.*
import scalaz.syntax.std.boolean.*
import scalaz.syntax.std.option.*
import scalaz.syntax.traverse.*
import scalaz.{-\/, \/, \/-}
import scaloi.syntax.any.*
import scaloi.syntax.collection.*
import scaloi.syntax.disjunction.*
import scaloi.syntax.foldable.*
import scaloi.syntax.zero.*
import scaloi.syntax.ʈry.*

import java.io.BufferedInputStream
import java.util.UUID
import scala.util.{Try, Using}

@Service
class BaseHtmlService(
  blobService: BlobService,
  nodeService: BaseAssetNodeService,
  projectDao2: ProjectDao2,
  serviceMeta: ServiceMeta,
  domain: => DomainDTO
)(implicit edgeService: EdgeService, configService: AuthoringConfigurationService)
    extends HtmlService:
  import BaseHtmlService.*

  /** ********* Blob stuff ******
    */
  override def renderPrintFriendlyHtml(
    htmls: Seq[Asset[Html]],
    title: String,
    ws: AttachedReadWorkspace,
  ): Try[String] =
    val validatedBlobsWithHtmls = htmls
      .map(html => validateBlob(html))
      .toList
      .traverse(_.disjunction)
      .toTry
    validatedBlobsWithHtmls.map(blobsAndHtmls => buildHtmlString(blobsAndHtmls, title, ws, false))
  end renderPrintFriendlyHtml

  override def createHtml(
    html: Asset[Html],
    ws: AttachedReadWorkspace,
    edit: Option[FileInfo \/ BlobRef],
    useCdn: Boolean,
  ): Try[RenderedHtmlDto] =
    logger.info(s"Rendering ${html.info.typeId} ${html.info.name}")
    for blobAndHtml <- edit match
                         case None            => validateBlob(html)
                         case Some(-\/(info)) => Try(info -> html)
                         case Some(\/-(ref))  => Try(blobService.ref2Info(ref) -> html)
    yield
      val finalHtml = buildHtmlString(Seq(blobAndHtml), html.data.title, ws, useCdn)
      RenderedHtmlDto(finalHtml, safe = false, "html")
  end createHtml

  override def createHtml(
    edit: FileInfo,
    ws: AttachedReadWorkspace,
  ): RenderedHtmlDto =
    val (defaultCss, defaultJs) = ws
      .getNodeId(ws.homeName)
      .flatMap(courseId => courseDefaults(ws, courseId))
      .getOrElse(Nil -> Nil)

    val preppedStylesheets      = blobConcatStylesheets(Nil, Nil, defaultCss, false)
    val (preppedJs, preppedCss) = blobConcatJavascripts(Nil, defaultJs, false, false)

    val htmlStr   = readBlobToString(edit)
    val finalHtml = createFinalHtmlString(htmlStr, "Untitled", preppedStylesheets + preppedCss, preppedJs)
    RenderedHtmlDto(finalHtml, safe = false, "html")
  end createHtml

  /** Covert html body, title, styles, and scripts into valid html doc
    */
  private def createFinalHtmlString(
    replacedHtml: String,
    htmlTitle: String,
    styles: String,
    js: String
  ) =
    s"""
       |<!DOCTYPE html>
       |<html lang="en">
       |<head>
       |  <meta charset="UTF-8">
       |  <meta name="viewport" content="width=device-width, initial-scale=1.0">
       |  <title>$htmlTitle</title>
       |  $styles
       | </head>
       | <body>
       | $replacedHtml
       | $js
       | </body>
       | </html>
   """.stripMargin

  private def replaceEdgeIdsInHtml(
    html: String,
    resources: Seq[AssetEdge[?, ?]]
  ) =
    // These too could use the webAsset endpoint via cdn if we knew that we could serve all images to anonymous...
    resources.foldLeft(html)((html, resource) =>
      html.replaceAll(
        s"loEdgeId://${resource.edgeId}",
        s"/api/v2/authoring/nodes/${resource.target.info.id}/serve"
      )
    )

  private def validateBlob(html: Asset[Html]): Try[(FileInfo, Asset[Html])] =
    blobService.requireBlobInfo(html).map(blob => (blob, html))

  /**   1. Get the graph of all the HTMLs 2. Sort them out by type 3. Prep the CSS string with all the CSS nodes/blobs
    *      and their resources 4. Prep the JS string with all the JS nodes/blobs 5. Prep the HTML string with all the
    *      HTML nodes/blobs and their resources 7. Create the final HTML string with all the parts
    */
  private def buildHtmlString(
    validatedBlobsAndHtmls: Seq[(FileInfo, Asset[Html])],
    title: String,
    ws: AttachedReadWorkspace,
    useCdn: Boolean,
  ): String =

    val enclosingCourseId = ws match
      case lws: ProjectWorkspace => projectDao2.loadHomeIds(lws).get

    // The HTML is fake if we're rendering a new and unsaved page in narrative authoring
    // so there is no asset at all. In this case we want to pull the stock course defaults.
    val defaults = for
      (_, html) <- validatedBlobsAndHtmls.headOption
      courseId  <- if isFake(html) then ws.getNodeId(ws.homeName) else enclosingCourseId(html.info.name)
      defaults  <- courseDefaults(ws, courseId)
    yield defaults

    val (defaultCss, defaultJs) = defaults | (Nil, Nil)

    val htmls                     = validatedBlobsAndHtmls.map(_._2).filterNot(isFake)
    val simple                    = htmls.exists(_.data.attribution.contains("IMSCC")) // OMG
    // DB hit
    val graph                     = edgeService.stravaigeOutGraphs(
      TraverseGraph
        .fromSources(htmls.map(_.info.name)*)
        .traverse(Group.Dependencies, Group.Resources)
        .traverse(Group.Scripts, Group.Stylesheets)
        .traverse(Group.CssResources) :: Nil,
      ws
    )
    val config                    = configService.getConfig
    val webDependencies           =
      htmls.flatMap(html => graph.targetsInGroup(html, Group.Dependencies))
    val stylesheets               =
      webDependencies.flatMap(dep => graph.targetsInGroupOfType[Stylesheet](dep, Group.Stylesheets)).distinct
    val javascripts               =
      webDependencies.flatMap(dep => graph.targetsInGroupOfType[Javascript](dep, Group.Scripts)).distinct
    val htmlResourceEdgesBySource =
      htmls.groupMapUniq(_.info.id)(html => graph.outEdgesInGroup(html, Group.Resources))
    val cssResourceEdges          = graph.edges.filter(edge => edge.group == Group.CssResources).distinct
    val preppedStylesheets        = blobConcatStylesheets(stylesheets, cssResourceEdges, defaultCss, useCdn)
    val (preppedJs, preppedCss)   = blobConcatJavascripts(javascripts, defaultJs, useCdn, simple)
    val concattedHtmls            =
      blobConcatHtmls(validatedBlobsAndHtmls, htmlResourceEdgesBySource, useCdn, config.injectDataIds)
    createFinalHtmlString(concattedHtmls, title, preppedStylesheets + preppedCss, preppedJs)
  end buildHtmlString

  private def isFake(html: Asset[Html]): Boolean = html.info.id == 0L

  private def courseDefaults(ws: ReadWorkspace, courseId: Long): Option[(Seq[String], Seq[String])] =
    for course <- nodeService.loadA[Course](ws).byId(courseId)
    yield course.data.defaultCss -> course.data.defaultJs

  /**   1. For each JS, if it has a blob, concat together
    *   - a call to the AttachmentWebController endpoint with the node id 2. If `injectIframeResizer`, append resizer
    *     stuff
    *
    * @return
    *   the concatenated string plus any computed stylesheets
    */
  private def blobConcatJavascripts(
    jses: Seq[Asset[Javascript]],
    defaultJs: Seq[String],
    useCdn: Boolean,
    simple: Boolean,
  ): (String, String) =
    val defaults    = defaultJs.toList.map(mapCdnUrlsIf(useCdn)).foldMap(javaScript)
    val js          = jses.toList.flatMap(assetScript(_, useCdn))
    val config      = configService.getConfig
    val resizer     = config.injectIframeResizer ?? resizerScript
    val jquery      = (config.injectContentFeedback && !simple) ?? jqueryScript
    val feedbacker  = (config.injectContentFeedback && !simple) ?? feedbackScript
    val feedbackCss = (config.injectContentFeedback && !simple) ?? feedbackStyle(assetsUrl)
    val loer        = simple !? loScripts
    defaults + js.mkString + resizer + jquery + feedbacker + loer -> feedbackCss
  end blobConcatJavascripts

  private def javaScript(url: String): String =
    s"""<script type="text/javascript" src="$url"></script>\n"""

  private def assetScript(js: Asset[Javascript], useCdn: Boolean): Option[String] =
    js.data.source as { // CDN safe because we're serving by node id and any change will create a new node
      javaScript(s"$cdnPrefix/api/v2/authoring/nodes/${js.info.id}/${useCdn ? "cdnAsset" | "webAsset"}")
    }

  private def jqueryScript: String =
    javaScript(s"$assetsUrl/jquery/jquery.slim.min.js")

  private def resizerScript: String =
    javaScript(s"$assetsUrl/iframe-resizer/iframeResizer.contentWindow.min.js")

  private def cdnPrefix: String =
    Option(serviceMeta.getStaticHost).map { host =>
      val hostPart = domain.hostName.toLowerCase stripSuffix serviceMeta.getStaticSuffix
      s"//$host/cdn/$hostPart"
    } | ""

  private def assetsUrl: String =
    ComponentUtils.resourceUrl(
      "assets",
      ComponentSupport.getComponentDescriptor(loi.authoring.authoringComponentIdentifier)
    )

  /**   1. Group CSS resources by source. 2. For each css, if it has a blob, concat together
    *   - Make string from blob
    *   - Apply any CDN rewrites
    *   - If it has resources, for each resource
    *     - replace the edgeid in the css string with endpoint with resource's node id
    *
    * @return
    *   the concatenated string
    */
  private def blobConcatStylesheets(
    csses: Seq[Asset[Stylesheet]],
    cssResources: Seq[AssetEdge[?, ?]],
    defaultCss: Seq[String],
    useCdn: Boolean,
  ): String =
    val resourceEdgesBySource = cssResources
      .groupBy(_.source)
      .view
      .mapValues(_.sortBy(_.position))
      .toMap

    val defaultLinks = defaultCss.toList foldMap { url =>
      s"""<link href="${mapCdnUrlsIf(useCdn)(url)}" rel="stylesheet" type="text/css"/>\n"""
    }
    val inlineStyles = csses.toList flatFoldMap { css =>
      blobService.getBlobInfo(css) map { cssBlob =>
        val resources    = resourceEdgesBySource.getOrElse(css, Seq.empty)
        val rawCss       = readBlobToString(cssBlob)
        val mappedCss    = mapCdnUrlsIf(useCdn)(rawCss)
        // If we could reliably identify these resources as safe to deliver anonymously then
        // these URLs could use CDN and webAsset instead
        val resourcedCss = resources.foldLeft(mappedCss) { case (stylesheet, resourceEdge) =>
          stylesheet.replaceAll(
            resourceEdge.edgeId.toString,
            s"/api/v2/authoring/nodes/${resourceEdge.target.info.id}/serve"
          )
        }
        s"<style>$resourcedCss</style>\n"
      }
    }
    (defaultLinks + inlineStyles) ||| vanillaCss
  end blobConcatStylesheets

  /**   1. For each HTML's blob,
    *      - read out the HTML string
    *      - if it has resources, for each resource edge
    *        - replace the HTML string's edgeids with the appropriate resource endpoint
    *      - apply any configured external url -> cdn rewrites 2. Concat the strings together
    *
    * @return
    *   the concatenated string
    */
  private def blobConcatHtmls(
    blobsAndHtmls: Seq[(FileInfo, Asset[Html])],
    htmlResourceEdgesBySource: Map[Long, Seq[AssetEdge[?, ?]]],
    useCdn: Boolean,
    injectDataIds: Boolean,
  ): String =
    blobsAndHtmls.toList foldMap { case (blob, html) =>
      val htmlStr    = readBlobToString(blob)
      val htmlIdStr  = htmlStr.transformIf(injectDataIds)(DataIds.render)
      val mappedHtml = mapCdnUrlsIf(useCdn)(htmlIdStr)
      val resources  = htmlResourceEdgesBySource.getOrElse(html.info.id, Seq.empty)
      replaceEdgeIdsInHtml(mappedHtml, resources)
    }

  private def readBlobToString(blob: FileInfo): String =
    Using.resource(blob.openInputStream()) { in =>
      TextExtractor.extract(new BufferedInputStream(in))
    }

  private def mapCdnUrlsIf(useCdn: Boolean)(str: String): String =
    str.transformIf(useCdn)(mapCdnUrls)
end BaseHtmlService

object BaseHtmlService:
  private final val logger = org.log4s.getLogger

  def mapCdnUrls(str: String)(implicit configService: AuthoringConfigurationService): String =
    CdnMappingRE.findAllMatchIn(configService.getConfig.cdnMapping).toList.foldLeft(str) { case (o, m) =>
      o.replace(m.group(1), m.group(2))
    }

  private final val feedbackScript =
    """<script>
      |  $(document).ready(function() {
      |    var curSel = undefined;
      |    function clearSelection() {
      |      document.getSelection()?.empty();
      |      curSel = undefined;
      |    }
      |    function upListener(e) {
      |      var newSel = document.getSelection()?.toString().trim() || undefined;
      |      if (newSel !== curSel) {
      |        curSel = newSel;
      |        var id = e.target.closest('[data-id]')?.getAttribute('data-id');
      |        window.parent.postMessage({ fn: 'onSelection', arg0: newSel, arg1: e.x, arg2: e.y, arg3: id }, '*');
      |      }
      |    }
      |    function dblListener(e) {
      |     if (e.target.nodeName === 'IMG' && e.target.src) {
      |        var id = e.target.closest('[data-id]')?.getAttribute('data-id');
      |        window.parent.postMessage({ fn: 'onSelection', arg0: e.target.src, arg1: e.x, arg2: e.y, arg3: id }, '*');
      |        clearSelection();
      |        e.preventDefault();
      |      }
      |    }
      |    function checkEnabled(first) {
      |      const enabled = !!window.parent.feedbackEnabled;
      |      if (enabled != document.body.classList.contains('feedback-mode')) {
      |        document.body.classList.toggle('feedback-mode', enabled);
      |        if (enabled) {
      |          document.addEventListener('mouseup', upListener);
      |          document.addEventListener('dblclick', dblListener);
      |        } else {
      |          document.removeEventListener('mouseup', upListener);
      |          document.removeEventListener('dblclick', dblListener);
      |        }
      |      }
      |      document.body.classList.toggle('flag-mode', !!window.parent.flagMode);
      |      document.body.classList.toggle('windows', /windows/i.test(navigator.userAgent));
      |    }
      |    window.addEventListener('message', e => {
      |      if (e.data?.fn === 'clearSelection') clearSelection();
      |      else if (e.data?.fn === 'checkEnabled') checkEnabled();
      |      else if (e.data?.fn === 'highlightElement') {
      |        var el = document.querySelector('[data-id="' + e.data.id + '"]');
      |        el?.scrollIntoView({ behavior: 'auto', block: 'center', inline: 'center' });
      |        el?.classList.add('highlit');
      |        setTimeout(() => el?.classList.remove('highlit'), 1000);
      |      }
      |    });
      |    checkEnabled(true);
      |  });
      |</script>
      |""".stripMargin

  private def feedbackStyle(assetsUrl: String) =
    s"""<style>
      |  .highlit {
      |    background-color: #fff100 !important;
      |    outline: 4px solid #fff100 !important;
      |  }
      |  .feedback-mode ::selection {
      |    background-color: #fff100;
      |  }
      |  .flag-mode [lang] {
      |    outline: 1px dotted #adb5bd;
      |  }
      |  .flag-mode [lang]:before {
      |    margin-right: 0.1rem;
      |    line-height: 1;
      |    color: #6c757d;
      |  }
      |  .flag-mode.windows [lang]:before {
      |    font-family: 'Twemoji Country Flags', sans-serif;
      |  }
      |  .flag-mode [lang=de]:before {
      |    content: '🇩🇪';
      |  }
      |  .flag-mode [lang=en]:before {
      |    content: '🇬🇧';
      |  }
      |  .flag-mode [lang=es]:before {
      |    content: '🇪🇸';
      |  }
      |  .flag-mode [lang=fr]:before {
      |    content: '🇫🇷';
      |  }
      |  .flag-mode [lang=ga]:before {
      |    content: '🇮🇪';
      |  }
      |  @font-face {
      |    font-family: 'Twemoji Country Flags';
      |    unicode-range: U+1F1E6-1F1FF, U+1F3F4, U+E0062-E0063, U+E0065, U+E0067, U+E006C, U+E006E, U+E0073-E0074, U+E0077, U+E007F;
      |    src: url('$assetsUrl/fonts/TwemojiCountryFlags.woff2') format('woff2');
      |  }
      |</style>
      |""".stripMargin

  private final val loScripts =
    """
      |<script>
      |  function lonav(edgeOrEvent) {
      |    window.parent.lonav(edgeOrEvent);
      |  }
      |</script>
      |""".stripMargin

  // Vanilla CSS if there are no defaults. Duplicate in _narrative.scss for now. Keep
  // fonts aligned with courseware.ejs.
  private final val vanillaCss =
    """
      |<link href="//fonts.googleapis.com/css?family=Open Sans:300,400,600,700|Material+Icons" rel="stylesheet"/>
      |<style>
      |  html {
      |    margin: 0;
      |    padding: 0;
      |  }
      |  body {
      |    margin: 0;
      |    padding: .5rem 0 0;
      |    font-family: 'Open Sans', 'Oxygen', 'Helvetica Neue', Helvetica, sans-serif;
      |    color: #212529;
      |  }
      |  p, blockquote, h1, h2, h3, h4, h5, pre {
      |    margin: 0 0 1rem;
      |  }
      |
      |  h1, h2, h3, h4, h5 {
      |    font-weight: 400;
      |    color: black;
      |  }
      |
      |  h1 {
      |    font-size: 2.5rem;
      |  }
      |
      |  h2 {
      |    font-size: 2rem;
      |  }
      |
      |  h3 {
      |    font-size: 1.5rem;
      |  }
      |
      |  h4 {
      |    font-size: 1.25rem;
      |  }
      |
      |  h5 {
      |    font-size: 1rem;
      |    font-weight: 600;
      |  }
      |
      |  a[href] {
      |    color: #006388;
      |    text-decoration: underline;
      |    &:hover {
      |      color: #003144;
      |    }
      |  }
      |
      |  blockquote {
      |    border-left: 4px solid $gray-500;
      |    padding-left: .5rem;
      |    font-style: italic;
      |  }
      |
      |  img, iframe {
      |    max-width: 100%;
      |    border-radius: .3rem;
      |    display: block;
      |    margin: 0 auto;
      |  }
      |
      |  ul,
      |  ol {
      |    list-style-position: outside;
      |    margin: 0 0 1rem;
      |
      |    li {
      |      margin: 0 0 .5rem;
      |    }
      |
      |    ol,
      |    ul {
      |      margin-left: 1em;
      |      margin-bottom: 1rem;
      |    }
      |  }
      |
      |  ul {
      |    list-style-type: disc;
      |  }
      |
      |  ol {
      |    list-style-type: decimal;
      |  }
      |
      |  dl {
      |    dt {
      |      font-weight: 600;
      |      margin-bottom: .5rem;
      |    }
      |
      |    dd {
      |      margin: 0 0 1rem;
      |    }
      |  }
      |
      |  :last-child {
      |    margin-bottom: 0;
      |  }
      |
      |  cite,
      |  em,
      |  i {
      |    font-style: italic;
      |  }
      |
      |  code,
      |  var {
      |    font-family: monospace, serif;
      |  }
      |  table {
      |    table-layout: auto;
      |    margin-bottom: 1rem;
      |    background: #fff;
      |    border: none;
      |    border-spacing: 0;
      |    border-collapse: separate !important;
      |    border-radius: 5px;
      |    width: 100%;
      |
      |    caption {
      |      caption-side: bottom;
      |      font-weight: 600;
      |      padding: 0.75rem;
      |      color: inherit;
      |    }
      |
      |    thead,
      |    tbody {
      |      th,
      |      td {
      |        border: solid #d5d5d5;
      |        border-width: 1px 1px 0 0;
      |      }
      |
      |      td:first-child,
      |      th:first-child {
      |        border-width: 1px 1px 0 1px;
      |      }
      |
      |      tr:first-child {
      |        td:first-child,
      |        th:first-child {
      |          border-top-left-radius: 5px;
      |        }
      |        td:last-child,
      |        th:last-child {
      |          border-width: 1px 1px 0 0;
      |          border-top-right-radius: 5px;
      |        }
      |      }
      |      tr:last-child {
      |        td,
      |        th {
      |          border-width: 1px 1px 1px 0;
      |        }
      |        td:first-child,
      |        th:first-child {
      |          border-width: 1px;
      |          border-bottom-left-radius: 5px;
      |        }
      |        td:last-child,
      |        th:last-child {
      |          border-width: 1px 1px 1px 0;
      |          border-bottom-right-radius: 5px;
      |        }
      |      }
      |    }
      |
      |    thead:has(+ tbody) {
      |      tr:last-child {
      |        td:first-child,
      |        th:first-child {
      |          border-bottom-left-radius: 0;
      |        }
      |
      |        td:last-child,
      |        th:last-child {
      |          border-bottom-right-radius: 0;
      |        }
      |      }
      |    }
      |
      |    thead + tbody {
      |      tr:first-child {
      |        td,
      |        th {
      |          border-top-width: 0;
      |        }
      |        td:first-child,
      |        th:first-child,
      |        td:last-child,
      |        th:last-child {
      |          border-top-width: 0;
      |          border-radius: 0;
      |        }
      |      }
      |    }
      |
      |    tr {
      |      padding: 1rem;
      |      vertical-align: top;
      |
      |      th {
      |        font-weight: 600;
      |        padding: 1rem;
      |        text-align: left;
      |      }
      |
      |      td {
      |        padding: 0.75rem;
      |      }
      |    }
      |  }
      |  @media (min-width: 35.5em) {
      |    body {
      |      padding: 1rem 1rem 0;
      |    }
      |  }
      |</style>
      |""".stripMargin
end BaseHtmlService
