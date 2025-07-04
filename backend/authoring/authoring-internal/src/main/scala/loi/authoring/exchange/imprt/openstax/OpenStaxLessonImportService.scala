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

package loi.authoring.exchange.imprt.openstax

import java.nio.file.{Files, Paths}

import com.fasterxml.jackson.databind.ObjectMapper
import com.learningobjects.cpxp.component.annotation.Service
import loi.asset.license.License
import loi.authoring.asset.factory.AssetTypeId
import loi.authoring.edge.Group
import loi.authoring.exchange.imprt.ImporterUtils.*
import loi.authoring.exchange.imprt.NodeExchangeBuilder
import loi.authoring.exchange.imprt.math.MathMLToLatexConverter
import loi.authoring.exchange.model.NodeExchangeData

import scala.collection.mutable.ListBuffer
import scala.xml.{Elem, Node}

@Service
class OpenStaxLessonImportService(mapper: ObjectMapper):
  private final val log = org.log4s.getLogger

  import OpenStaxLessonImportService.*

  def buildAssetsFromDocument(
    document: Elem,
    licenseAndAuthor: (License, String),
    lessonPath: String
  ): List[NodeExchangeData] =
    val title           = getTitleFromDocument(document)
    val contentChildren = (document \ "content").head.child
    val glossary        = document \ "glossary"
    val sections        = combineNeighboringNonSections(contentChildren)
    val resources       =
      if glossary.isEmpty then sections.map(s => buildResource(s, licenseAndAuthor, lessonPath))
      else
        (sections ++ combineNeighboringNonSections(glossary)).map(s => buildResource(s, licenseAndAuthor, lessonPath))
    val lesson          = NodeExchangeBuilder
      .builder(guid, AssetTypeId.Lesson.entryName)
      .title(title)
      .edges(buildEmptyEdgesFromAssets(resources.map(_._1).toList, Group.Elements))
      .licenseAndAuthor(licenseAndAuthor)
      .build()
    resources.flatMap(_._2).toList ::: List(lesson)
  end buildAssetsFromDocument

  private def buildResource(
    section: Node,
    licenseAndAuthor: (License, String),
    lessonPath: String
  ): (NodeExchangeData, Seq[NodeExchangeData]) =

    val title   = (section \ "title").headOption.map(_.text).getOrElse(generateTitle(section))
    val layouts = buildRowsForSection(section, licenseAndAuthor, lessonPath)

//    val designer = NodeExchangeBuilder
//      .builder(guid, AssetTypeId.ContentDesigner.entryName)
//      .licenseAndAuthor(licenseAndAuthor)
//      .edges(buildEmptyEdgesFromAssets(layouts._1.toList, Group.Rows))
//      .build()
    val resource = NodeExchangeBuilder
      .builder(guid, AssetTypeId.Html.entryName)
      .title(title)
      .licenseAndAuthor(licenseAndAuthor)
//      .edges(buildEmptyEdgesFromAssets(List(designer), Group.ContentDesigner))
      .build()

    val assets = layouts._2 :+ resource
    (resource, assets)
  end buildResource

  def buildRowsForSection(
    section: Node,
    licenseAndAuthor: (License, String),
    lessonPath: String
  ): (Seq[NodeExchangeData], Seq[NodeExchangeData]) =
    val nodesToTransform = getSupportedChildren(section, 0)

    nodesToTransform.collect {
      /* Each transform returns tuple (layout row assets, all (parents and descendants) row assets). */
      case para: Elem if para.label == "para"                                          => buildTextRow(para, licenseAndAuthor)
      case section: Elem if section.label == "section" && isImportableSection(section) =>
        buildTextRow(section, licenseAndAuthor)
      case exercise: Elem if exercise.label == "exercise"                              => buildRevealBlock(exercise, licenseAndAuthor)
      case equation: Elem if equation.label == "equation"                              => buildMathBlock(equation, licenseAndAuthor)
      case table: Elem if table.label == "table"                                       => buildTableRow(table, licenseAndAuthor)
      case figure: Elem if figure.label == "figure"                                    => buildImageRow(figure, licenseAndAuthor, lessonPath)
      case subfigure: Elem if subfigure.label == "subfigure"                           => buildImageRow(subfigure, licenseAndAuthor, lessonPath)
      case note: Elem if note.label == "note"                                          => buildTextRow(note, licenseAndAuthor)
      case example: Elem if example.label == "example"                                 => buildCalloutRow(example, licenseAndAuthor)
      case glossary: Elem if glossary.label == "glossary"                              => buildTermsRow(glossary, licenseAndAuthor)
      case list: Elem if list.label == "list"                                          => buildListRow(list, licenseAndAuthor)
      case title: Elem if title.label == "title"                                       => buildHeaderRow(title, 2, licenseAndAuthor)
      case quote: Elem if quote.label == "quote"                                       => buildTextRow(quote, licenseAndAuthor)
      case <break/>                                                                    => buildBreakRow
      // TODO case cite
    }.unzip match
      case (l, a) => (l.flatten, a.flatten)
    end match
  end buildRowsForSection

  private def getSupportedChildren(section: Node, depth: Int): Seq[Node] =
    section.child.foldLeft(Seq.empty[Node])((acc, child) =>
      child match
        case para: Elem if para.label == "para"                                                   => acc :+ para
        case subsection: Elem if subsection.label == "section" && isImportableSection(subsection) =>
          acc ++ getSupportedChildren(subsection, depth + 1)
        case exercise: Elem if exercise.label == "exercise"                                       => acc :+ exercise
        case equation: Elem if equation.label == "equation"                                       => acc :+ equation
        case figure: Elem if figure.label == "figure" && (figure \\ "subfigure").isEmpty          => acc :+ figure
        case figure: Elem if figure.label == "figure" && (figure \\ "subfigure").nonEmpty         =>
          // TODO: this should be handled specifically
          acc ++ figure \\ "subfigure"
        case table: Elem if table.label == "table"                                                => acc :+ table
        case note: Elem if note.label == "note"                                                   => acc ++ splitNoteRows(note)
        case example: Elem if example.label == "example"                                          => acc :+ example
        case glossary: Elem if glossary.label == "glossary"                                       => acc :+ glossary
        case list: Elem if list.label == "list"                                                   => acc :+ list
        case title: Elem if title.label == "title" && depth > 0                                   => acc :+ title
        case quote: Elem if quote.label == "quote"                                                => acc :+ quote
        case break @ <break/>                                                                     => acc :+ break
        case _                                                                                    => acc
        // TODO case cite
    )

  private def buildListRow(
    node: Node,
    licenseAndAuthor: (License, String)
  ): (Seq[NodeExchangeData], Seq[NodeExchangeData]) =
//    val listExchangeData = NodeExchangeBuilder
//      .builder(guid, AssetTypeId.Text.entryName)
//      .licenseAndAuthor(licenseAndAuthor)
//      .html(buildListHtml(node, 0))
//      .build()
//    val layout           = NodeExchangeBuilder
//      .builder(guid, AssetTypeId.OneColumnLayout.entryName)
//      .licenseAndAuthor(licenseAndAuthor)
//      .edges(buildEmptyEdgesFromAssets(List(listExchangeData), Group.Content))
//      .build()
//
//    (Seq(layout), Seq(layout, listExchangeData))
    (Nil, Nil)
  end buildListRow

  private def buildTermsRow(
    node: Node,
    licenseAndAuthor: (License, String)
  ): (Seq[NodeExchangeData], Seq[NodeExchangeData]) =
//    val terms             = node.child.collect {
//      case definition: Elem if definition.label == "definition" =>
//        DefinitionItem(Option((definition \ "term").text), Option((definition \ "meaning").text))
//    }
//    val termsData         = Definition(
//      terms = terms.toSeq,
//      license = Option(licenseAndAuthor._1),
//      author = Option(licenseAndAuthor._2),
//      attribution = None
//    )
//    val termsExchangeData = NodeExchangeBuilder
//      .builder(guid, AssetTypeId.Definition.entryName, mapper.valueToTree(termsData))
//      .build()
//    val layout            = NodeExchangeBuilder
//      .builder(guid, AssetTypeId.OneColumnLayout.entryName)
//      .licenseAndAuthor(licenseAndAuthor)
//      .edges(buildEmptyEdgesFromAssets(List(termsExchangeData), Group.Content))
//      .build()
//
//    (Seq(layout), Seq(layout, termsExchangeData))
    (Nil, Nil)
  end buildTermsRow

  private def buildImageRow(
    node: Node,
    licenseAndAuthor: (License, String),
    lessonPath: String
  ): (Seq[NodeExchangeData], Seq[NodeExchangeData]) =
    val imagePath    = (node \ "media" \ "image" \ "@src").text
    val contentType  = Files.probeContentType(Paths.get(imagePath))
    val altText      = (node \ "media" \ "@alt").text
    val title        = (node \ "title").text
    val caption      = (node \ "caption").text
    val itemId       = getItemId((node \ "@id").text)
    val imageCaption =
      if itemId.nonEmpty || title.nonEmpty then "<p><strong>" + itemId + " " + title + "</strong> " + caption + "</p>"
      else if caption.nonEmpty then "<p>" + caption + "</p>"
      else "<p>" + altText + "</p>"

//    val imageData = JsonNodeFactory.instance
//      .objectNode()
//      .put("mimeType", contentType)
//      .put("altText", altText)
//      .put("caption", imageCaption)
//      .put("license", licenseAndAuthor._1.abbreviation)
//      .put("author", licenseAndAuthor._2)
//
//    val image = NodeExchangeBuilder
//      .builder(guid, AssetTypeId.Image.entryName, imageData)
//      .title(imagePath)
//      .attachment(Paths.get(lessonPath, imagePath).toString)
//      .build()
//
//    val layout = NodeExchangeBuilder
//      .builder(guid, AssetTypeId.OneColumnLayout.entryName)
//      .licenseAndAuthor(licenseAndAuthor)
//      .edges(buildEmptyEdgesFromAssets(List(image), Group.Content))
//      .build()
//
//    (Seq(layout), Seq(layout, image))
    (Nil, Nil)
  end buildImageRow

  private def buildTextRow(
    section: Node,
    licenseAndAuthor: (License, String)
  ): (Seq[NodeExchangeData], Seq[NodeExchangeData]) =

//    val text   = NodeExchangeBuilder
//      .builder(guid, AssetTypeId.Text.entryName)
//      .licenseAndAuthor(licenseAndAuthor)
//      .html("<p>" + transformNodeToHtmlContent(section, 0) + "</p>")
//      .build()
//    val layout = NodeExchangeBuilder
//      .builder(guid, AssetTypeId.OneColumnLayout.entryName)
//      .licenseAndAuthor(licenseAndAuthor)
//      .edges(buildEmptyEdgesFromAssets(List(text), Group.Content))
//      .build()
//
//    (Seq(layout), Seq(layout, text))
    (Nil, Nil)
  end buildTextRow

  private def buildBreakRow: (Seq[NodeExchangeData], Seq[NodeExchangeData]) =
//    val leaf   = NodeExchangeBuilder.builder(guid, AssetTypeId.Text.entryName).html("<hr />").build()
//    val layout = NodeExchangeBuilder
//      .builder(guid, AssetTypeId.OneColumnLayout.entryName)
//      .edges(buildEmptyEdgesFromAssets(List(leaf), Group.Content))
//      .build()
//    (Seq(layout), Seq(layout, leaf))
    (Nil, Nil)

  private def buildHeaderRow(
    title: Node,
    level: Int,
    licenseAndAuthor: (License, String)
  ): (Seq[NodeExchangeData], Seq[NodeExchangeData]) =
//    val text   = NodeExchangeBuilder
//      .builder(guid, AssetTypeId.Text.entryName)
//      .licenseAndAuthor(licenseAndAuthor)
//      .html(s"<h$level>" + transformNodeToHtmlContent(title, 0) + s"</h$level>")
//      .build()
//    val layout = NodeExchangeBuilder
//      .builder(guid, AssetTypeId.OneColumnLayout.entryName)
//      .licenseAndAuthor(licenseAndAuthor)
//      .edges(buildEmptyEdgesFromAssets(List(text), Group.Content))
//      .build()
//
//    (Seq(layout), Seq(layout, text))
    (Nil, Nil)
  end buildHeaderRow

  private def buildCalloutRow(
    section: Node,
    licenseAndAuthor: (License, String)
  ): (Seq[NodeExchangeData], Seq[NodeExchangeData]) =
//    val calloutData = Text(
//      html = transformNodeToHtmlContent(section, 1),
//      license = Option(licenseAndAuthor._1),
//      author = Some(licenseAndAuthor._2).orElse(None),
//      attribution = None
//    )
//    val callout     = NodeExchangeBuilder
//      .builder(guid, AssetTypeId.Callout .entryName, mapper.valueToTree(calloutData))
//      .build()
//    val layout      = NodeExchangeBuilder
//      .builder(guid, AssetTypeId.OneColumnLayout.entryName)
//      .licenseAndAuthor(licenseAndAuthor)
//      .edges(buildEmptyEdgesFromAssets(List(callout), Group.Content))
//      .build()
//
//    (Seq(layout), Seq(layout, callout))
    (Nil, Nil)
  end buildCalloutRow

  private def buildMathBlock(
    section: Node,
    licenseAndAuthor: (License, String)
  ): (Seq[NodeExchangeData], Seq[NodeExchangeData]) =
//
//    val text   = NodeExchangeBuilder
//      .builder(guid, AssetTypeId.Text.entryName)
//      .licenseAndAuthor(licenseAndAuthor)
//      .html(convertMathML(section))
//      .build()
//    val layout = NodeExchangeBuilder
//      .builder(guid, AssetTypeId.OneColumnLayout.entryName)
//      .licenseAndAuthor(licenseAndAuthor)
//      .edges(buildEmptyEdgesFromAssets(List(text), Group.Content))
//      .build()
//
//    (Seq(layout), Seq(layout, text))
    (Nil, Nil)
  end buildMathBlock

  private def buildTableRow(
    section: Node,
    licenseAndAuthor: (License, String)
  ): (Seq[NodeExchangeData], Seq[NodeExchangeData]) =

    val title     = (section \ "title").text
    val caption   = (section \ "caption").text
    val itemId    = getItemId((section \ "@id").text)
    val maxCols   = (section \ "tgroup" \ "@cols").filter(_.nonEmpty).map(_.text.toInt).max
    val tableInfo =
      if itemId.nonEmpty || title.nonEmpty then
        s"""<tr><td colspan="$maxCols"><strong>""" + itemId + " " + title + "</strong> " + caption + "<td></tr>"
      else if caption.nonEmpty then s"""<tr><td colspan="$maxCols">""" + caption + "<td></tr>"
      else ""

//    val text   = NodeExchangeBuilder
//      .builder(guid, AssetTypeId.Table.entryName)
//      .licenseAndAuthor(licenseAndAuthor)
//      .html("""<table class=\"table\">""" + buildTable(section, header = false) + tableInfo + "</table>")
//      .build()
//    val layout = NodeExchangeBuilder
//      .builder(guid, AssetTypeId.OneColumnLayout.entryName)
//      .licenseAndAuthor(licenseAndAuthor)
//      .edges(buildEmptyEdgesFromAssets(List(text), Group.Content))
//      .build()
//
//    (Seq(layout), Seq(layout, text))
    (Nil, Nil)
  end buildTableRow

  private def buildTable(node: Node, header: Boolean): String =
    node.child
      .collect {
        case tgroup: Elem if tgroup.label == "tgroup" => s"${buildTable(tgroup, header)}"
        case thead: Elem if thead.label == "thead"    => buildTable(thead, header = true)
        case tbody: Elem if tbody.label == "tbody"    => buildTable(tbody, header = false)
        case row: Elem if row.label == "row"          => s"<tr>${buildTable(row, header)}</tr>"
        case entry: Elem if entry.label == "entry"    => buildTableEntry(entry, header)
      }
      .mkString
      .trim
      .replaceAll("\\s+", " ")

  private def buildTableEntry(node: Node, header: Boolean): String =
    val tag       = if header then "th" else "td"
    val nondigits = """\D""".r
    val st        = nondigits.replaceAllIn((node \ "@namest").text, "")
    val end       = nondigits.replaceAllIn((node \ "@nameend").text, "")
    if st != "" && end != "" then
      "<" + tag + " colspan=\"" + (1 + end.toInt - st.toInt) + "\">" + transformNodeToHtmlContent(
        node,
        1
      ) + "</" + tag + ">"
    else "<" + tag + ">" + transformNodeToHtmlContent(node, 1) + "</" + tag + ">"
  end buildTableEntry

  /* Generate a title from the node text. */
  private def generateTitle(node: Node): String =
    // TODO consider another way to make a title other than picking the first 3 words.
    if (node \ "glossary").nonEmpty then "Glossary"
    else
      val words = node.text.split("\\s+")
      words.take(3).mkString(" ")

  private def buildRevealBlock(
    node: Node,
    licenseAndAuthor: (License, String)
  ): (Seq[NodeExchangeData], Seq[NodeExchangeData]) =
    val solutionText = (node \ "solution").headOption.map(n => transformNodeToHtmlContent(n, 0)).getOrElse("")
    val problemText  = (node \ "problem").headOption.map(n => transformNodeToHtmlContent(n, 0)).getOrElse("")

//    val solutionExchangeData = NodeExchangeBuilder
//      .builder(guid, AssetTypeId.Text.entryName)
//      .licenseAndAuthor(licenseAndAuthor)
//      .html(solutionText)
//      .build()
//
//    val problemExchangeData = NodeExchangeBuilder
//      .builder(guid, AssetTypeId.Text.entryName)
//      .licenseAndAuthor(licenseAndAuthor)
//      .html(problemText)
//      .build()

//    val edges = Seq(
//      EdgeExchangeData(Group.Revealed, solutionExchangeData.id, 0, traverse = true, UUID.randomUUID(), EdgeData.empty),
//      EdgeExchangeData(Group.Revealer, problemExchangeData.id, 0, traverse = true, UUID.randomUUID(), EdgeData.empty)
//    )
//
//    val revealExchangeData = NodeExchangeBuilder
//      .builder(guid, AssetTypeId.Reveal.entryName)
//      .licenseAndAuthor(licenseAndAuthor)
//      .edges(edges)
//      .build()

//    val layout = NodeExchangeBuilder
//      .builder(guid, AssetTypeId.OneColumnLayout.entryName)
//      .licenseAndAuthor(licenseAndAuthor)
//      .edges(buildEmptyEdgesFromAssets(List(revealExchangeData), Group.Content))
//      .build()

    (Nil, Nil) // (Seq(layout), Seq(layout, solutionExchangeData, problemExchangeData, revealExchangeData))
  end buildRevealBlock

  private def getTitleFromDocument(document: Elem): String = (document \ "title").text

  private[openstax] def transformNodeToHtmlContent(node: Node, depth: Int): String =
    node.child
      .collect {
        case textNode: scala.xml.Text                                                    => textNode.text
        case para: Elem if para.label == "para"                                          =>
          s"""<div class="para">${transformNodeToHtmlContent(para, depth + 1)}</div>"""
        case list: Elem if list.label == "list"                                          => buildListHtml(list, depth)
        case item: Elem if item.label == "item"                                          => s"<li>${transformNodeToHtmlContent(item, depth + 1)}</li>"
        case term: Elem if term.label == "term"                                          => buildTermHtml(term, depth)
        case emphasis: Elem if emphasis.label == "emphasis"                              => buildEmphasisHtml(emphasis, depth)
        case title: Elem if title.label == "title"                                       =>
          s"""<div class="title"><strong>${transformNodeToHtmlContent(title, depth + 1)}</strong></div>"""
        case section: Elem if section.label == "section" && isImportableSection(section) =>
          s"""<div class="section">${transformNodeToHtmlContent(section, depth + 1)}</div>"""
        case link: Elem if link.label == "link" && (link \ "@url").length == 1           => resolveExternalLink(link)
        case link: Elem if link.label == "link" && (link.text.nonEmpty)                  => s" ${link.text} "
        case link: Elem if link.label == "link" && (link \ "@target-id").nonEmpty        =>
          s""" ${getItemId((link \ "@target-id").text)} """
        case math: Elem if math.label == "math"                                          => convertMathML(math)
        case equation: Elem if equation.label == "equation"                              => convertMathML(equation)
        case sub: Elem if sub.label == "sub"                                             => s"<sub>${transformNodeToHtmlContent(sub, depth + 1)}</sub>"
        case sup: Elem if sup.label == "sup"                                             => s"<sup>${transformNodeToHtmlContent(sup, depth + 1)}</sup>"
        case note: Elem if note.label == "note"                                          =>
          s"""<div class="note">${transformNodeToHtmlContent(note, depth + 1)}</div>"""
        case quote: Elem if quote.label == "quote"                                       =>
          s"""<div class="quote">${transformNodeToHtmlContent(quote, depth + 1)}</div>"""
        case <newline/>                                                                  => "<br>"
        // TODO case cite
      }
      .mkString
      .trim
      .replaceAll("\\s+", " ")

  private def buildListHtml(node: Node, depth: Int): String =
    val ordered       = (node \ "@list-type").text == "enumerated"
    val listStyleType = getListStyleType(node)
    val startTag      =
      if ordered then if listStyleType == "decimal" then "<ol>" else s"""<ol style="list-style-type:$listStyleType">"""
      else "<ul>"
    val endTag        = if ordered then "</ol>" else "</ul>"
    startTag + transformNodeToHtmlContent(node, depth + 1) + endTag

  private def getListStyleType(node: Node): String =
    val supportedStyles = Set("upper-alpha", "lower-alpha", "upper-roman", "lower-roman")
    val numberStyle     = (node \ "@number-style").text
    if supportedStyles.contains(numberStyle) then numberStyle else "decimal"

  private def buildTermHtml(term: Node, depth: Int): String =
    val strong = (term \ "@class").text != "no-emphasis"
    s"""<span class="term">${if strong then "<strong>" else ""}${transformNodeToHtmlContent(term, depth + 1)}${
        if strong then "</strong>"
        else ""
      }</span>"""

  private def buildEmphasisHtml(node: Node, depth: Int): String =
    val underline = (node \ "@effect").text == "underline"
    val startTag  = if underline then """<span style="text-decoration: underline;">""" else "<em>"
    val endTag    = if underline then "</span>" else "</em>"
    startTag + transformNodeToHtmlContent(node, depth + 1) + endTag

  private def convertMathML(node: Node): String =
    MathMLToLatexConverter.convert(node)

  private def resolveExternalLink(node: Node): String =
    val url = (node \ "@url").text
    "<a href=\"" + url + "\">" + node.text + "</a>"

  private def getItemId(itemId: String): String =
    val itemRegex = """ch(\d+)(\D+)(\d+)""".r
    itemId match
      case itemRegex(chap, item, num) => (if item == "tab" then "Table" else "Figure") + s" ${chap.toInt}.${num.toInt}"
      case _                          => itemId

  private def splitNoteRows(node: Node): Seq[Node] =
    val headingText = (node \ "@class").text.split("-").map(_.capitalize).mkString(" ")
    if headingText != "Concept Check" then
      val heading: Node = <title>{scala.xml.Text(headingText)}</title>
      val nodes         = node.child.foldLeft(Seq.empty[Node])((blocks, nextNode) =>
        if tagsToBlocks.contains(nextNode.label) then blocks :+ nextNode
        else if blocks.isEmpty || tagsToBlocks.contains(blocks.last.label) then blocks :+ <note>{nextNode}</note>
        else blocks.slice(0, blocks.length - 1) :+ <note>{blocks.last.child :+ nextNode}</note>
      )
      Seq(<break/>, heading) ++ nodes ++ Seq(<break/>)
    else Seq.empty[Node]
  end splitNoteRows

  /*
   We want to transform <content> children into lessons, but some children are too small (e.g., <para>, <figure>) to be
   lessons. Combine all neighboring non-section nodes under a section node and leave sections alone.
   */
  private def combineNeighboringNonSections(nodes: Seq[Node]): Seq[Node] =
    def appendChild(n: Node, newChild: Node) =
      n match
        case Elem(prefix, label, attribs, scope, child*) =>
          Elem(prefix, label, attribs, scope, minimizeEmpty = true, child ++ newChild*)
        case _                                           => throw new RuntimeException("Can only add children to elements!")
    /* Apologies to Scala for building a mutable collection this way */
    val buf                                  = ListBuffer.empty[Node]
    var combiningNeighbors: Boolean          = false
    var index                                = 0
    nodes.foreach {
      case section: Elem if section.label == "section" =>
        if isImportableSection(section) then
          if combiningNeighbors then
            combiningNeighbors = false
            index += 1
          buf += section
          index += 1
      case node: Elem                                  =>
        if combiningNeighbors then buf(index) = appendChild(buf(index), node)
        else
          combiningNeighbors = true
          buf += <section>{node}</section>
      case _                                           => /* Ignore whitespace */
    }
    buf
  end combineNeighboringNonSections

  def isImportableSection(node: Node): Boolean =
    !sectionClassesToIgnore.contains((node \ "@class").text)
end OpenStaxLessonImportService

object OpenStaxLessonImportService:
  private val tagsToBlocks           = Set("figure", "table", "quote")
  private val sectionClassesToIgnore = Set("chapter-review", "manage-skills", "manage-exercises", "critical-thinking")
