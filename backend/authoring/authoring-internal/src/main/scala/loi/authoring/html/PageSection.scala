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

package loi.authoring.html

import loi.asset.competency.service.CompetencyService
import loi.asset.util.Assex.*
import loi.authoring.AssetType
import loi.authoring.asset.Asset
import loi.authoring.asset.factory.AssetTypeId
import loi.authoring.edge.Group.{Assesses, Teaches}
import loi.authoring.exchange.exprt.CourseStructureExportService
import net.htmlparser.jericho.*
import scalaz.syntax.std.list.*
import scalaz.syntax.std.option.*
import scalaz.{Foldable, Functor, Monoid, ValidationNel, ~>}
import scalaz.std.string.*
import scalaz.syntax.std.boolean.*
import scaloi.syntax.option.*

import java.io.File
import java.util.UUID
import scala.collection.mutable
import scala.jdk.CollectionConverters.*

/** Parses a single-page HTML import file of the form:
  *
  * ```
  * <h1>[L|LESSON|LESSON TITLE] - Module title</h1>
  * <h1>[C|CHAPTER|CHAPTER TITLE] - Lesson title</h1>
  * <p>LO1.1 Teach this</p>
  * <h1>[P|PAGE|PAGE TITLE] - HTML title</h1>
  * <p>LO1.2 Teach that</p>
  * <p>A bunch of content</p>
  * <h1>[LP] - Module-level HTML title</h1>
  * <p>A bunch of content</p>
  * ```
  */
private[html] object PageSection:

  /** On success returns warnings and the parsed sections. */
  def parse[F[_]: Functor](file: File, asset: Asset[?], competencies: Map[String, UUID])(implicit
    FunK: mutable.Buffer ~> F,
  ): ValidationNel[PageImportError, (F[String], F[PageSection[F]])] =
    // so mutation, but the recursive pure fold was beyond comprehension
    val warnings      = mutable.Buffer.empty[String]
    val errors        = mutable.Buffer.empty[PageImportError]
    val sections      = mutable.Buffer.empty[PageSection[mutable.Buffer]]
    var currentLesson = Option.empty[LessonSection[mutable.Buffer]]
    var currentAsset  = Option.empty[AssetSection[mutable.Buffer]]
    val isLesson      = asset.info.typeId == AssetTypeId.Lesson
    val isModule      = asset.info.typeId == AssetTypeId.Module
    var h1Encountered = false

    // for a very crude title uniqueness check for page titles only
    var siblingPageTitles = mutable.Set.empty[String]

    def loop(implicit element: Element): Unit =
      element.getName match
        case "html" | "body" =>
          element.getChildElements.asScala.foreach(loop(using _))

        case "head" =>

        case "h1" =>
          element.getTextExtractor.toString match
            case H1Re(prefix, title) if prefixMatch(prefix, H1Prefix.Module) && !h1Encountered =>
              if isLesson then errors.append(PageImportError(s"Unexpected ${element.summary} when importing a lesson"))
              else if !asset.title.contains(title) then
                warnings.append(s"""Expected module title "${asset.title.orZ}" but encountered "$title"""")
              siblingPageTitles = mutable.Set.empty[String]

            case H1Re(prefix, title) if prefixMatch(prefix, H1Prefix.Lesson) && (isModule || !h1Encountered) =>
              if isModule then
                val lesson =
                  LessonSection[mutable.Buffer](title, mutable.Buffer.empty, mutable.Buffer.empty, mutable.Buffer.empty)
                sections.append(lesson)
                currentLesson = Some(lesson)
                currentAsset = None
              else if !asset.title.contains(title) then
                warnings.append(s"""Expected lesson title "${asset.title.orZ}" but encountered "$title"""")
              siblingPageTitles = mutable.Set.empty[String]

            case H1Re(prefix, title) if prefixMatch(prefix, H1Prefix.EndLesson) =>
              if currentLesson.nonEmpty then
                currentLesson foreach { lesson =>
                  if (title ne null) && !title.endsWith(lesson.title) then
                    warnings.append(s"""Non-matching "$title" expending "${lesson.title}"""")
                }
                currentLesson = None
              else warnings.append(s"""Unexpected end chapter""")

            case H1Re(prefix @ PrefixAssetTypeId(typeId), title) =>
              if prefixMatch(prefix, H1Prefix.ModulePage) then currentLesson = None
              val section = AssetSection[mutable.Buffer](
                typeId,
                title,
                mutable.Buffer.empty,
                mutable.Buffer.empty,
                mutable.Buffer.empty
              )
              currentLesson.cata(_.assets, sections).append(section)
              currentAsset = Some(section)

              if prefixMatch(prefix, H1Prefix.Page) then
                if !siblingPageTitles.add(title) then errors.append(PageImportError(s"Duplicate Page: $title"))

            case _ =>
              errors.append(PageImportError(s"Invalid H1: ${element.summary}"))
          end match
          h1Encountered = true

        case _ if sections.nonEmpty =>
          element.getTextExtractor.toString match
            case LORe(prefix, competency) if currentAsset.forall(_.paragraphs.isEmpty) =>
              currentAsset.orElse(currentLesson) match
                case Some(competent) =>
                  val assetType = AssetType.types(competent.typeId)
                  val groupOpt  =
                    if prefix == "LO" then
                      if assetType.edgeConfig.contains(Teaches) then Teaches.some
                      else Assesses.some
                    else if prefix.startsWith("T") then Teaches.some
                    else Assesses.some
                  groupOpt foreach { group =>
                    if !assetType.edgeConfig.contains(group) then
                      warnings.append(s"Unsupported $group competency alignment: $competency")
                    else
                      competencies.get(CompetencyService.normalize(competency)) match
                        case Some(uuid) =>
                          (group == Teaches).fold(competent.teaches, competent.assesses).append(uuid)

                        case _ =>
                          warnings.append(s"Unknown competency: $competency")
                  }

                case None =>
                  warnings.append(s"Unexpected competency: ${element.summary}")

            case _ if currentAsset.nonEmpty =>
              currentAsset.get.paragraphs.append(element.toString)

            case txt if txt.isBlank =>

            case _ =>
              errors.append(PageImportError(s"Unexpected content: ${element.summary}"))

        case _ =>
          if errors.lastOption.forall(s => !s.error.endsWith("before H1")) then
            errors.append(PageImportError(s"Unexpected ${element.summary} before H1"))

    val source = new Source(file)
    source.getChildElements.asScala.foreach(loop(using _))

    errors.toList.toNel.toFailure(FunK(warnings) -> FunK(sections.map(_.mapK(FunK))))
  end parse

  // The formats they've used over time
  private def prefixMatch(value: String, prefix: H1Prefix): Boolean =
    Option(value).exists(value =>
      value.equalsIgnoreCase(prefix.shortForm) ||   // C
        value.equalsIgnoreCase(prefix.entryName) || // CHAPTER
        value.equalsIgnoreCase(s"${prefix.entryName} TITLE") // CHAPTER TITLE
    )

  private implicit val booleanMonoid: Monoid[Boolean] = scalaz.std.anyVal.booleanInstance.disjunction

  /** If there are no alignments in the import structure then we will not strip alignment from existing content.
    */
  def isAligned[F[_]: Foldable: Functor](structure: F[PageSection[F]]): Boolean =
    Foldable[F].foldMap(structure) {
      case AssetSection(_, _, teaches, assesses, _) => !Foldable[F].empty(teaches) || !Foldable[F].empty(assesses)
      case LessonSection(_, teaches, _, htmls)      =>
        !Foldable[F].empty(teaches) || isAligned(Functor[F].widen[AssetSection[F], PageSection[F]](htmls))
    }

  // [PAGE TITLE] - 1. Lesson Introductions | 9. The End
  val H1Re = """^\s*(?:\[\s*([^]]+?)\s*])?[\s-–]*(.*?)\s*$""".r

  // [LO] 19.1 Describe foo. | [TEACHES] 19.1 Describe foo. | [T] 19.1 Describe foo.
  val LORe = """^\s*\[(LO|T|TEACHES|A|ASSESSES)]\s*(.*?)\s*$""".r

  /* Lawless Functor for Buffer */
  implicit val bunctor: Functor[mutable.Buffer] = new Functor[mutable.Buffer]:
    override def map[A, B](fa: mutable.Buffer[A])(f: A => B): mutable.Buffer[B] = fa.map(f)

  /* Natural transformation from Buffer to List */
  implicit val blatTrans: mutable.Buffer ~> List = new (mutable.Buffer ~> List):
    override def apply[A](b: mutable.Buffer[A]): List[A] = b.toList

  implicit class ElementOps(private val self: Element) extends AnyVal:
    def summary: String = self.toString.trunc(80)

  implicit class StringOps(private val self: String) extends AnyVal:
    def trunc(n: Int): String = if self.length <= n then self else self.take(n - 3) + "..."

  private object PrefixAssetTypeId:
    def unapply(prefix: String): Option[AssetTypeId] =
      if (prefix eq null) || prefixMatch(prefix, H1Prefix.ModulePage) || prefixMatch(prefix, H1Prefix.Page) then
        AssetTypeId.Html.some
      else if prefixMatch(prefix, H1Prefix.Discussion) then AssetTypeId.Discussion.some
      else CourseStructureExportService.assetTypeNameMap.find(_._2 `equalsIgnoreCase` prefix).map(_._1)
end PageSection

private[html] final case class PageImportError(
  error: String,
  pos: RowColumnVector,
):
  def message: String = s"Row ${pos.getRow}, col ${pos.getColumn}: $error"

private[html] object PageImportError:
  def apply(error: String)(implicit element: Element): PageImportError =
    new PageImportError(error, element.getRowColumnVector)

private[html] sealed trait PageSection[F[_]]:
  val typeId: AssetTypeId
  val title: String
  val teaches: F[UUID]
  val assesses: F[UUID]

  def mapK[K[_]: Functor](f: F ~> K): PageSection[K]

private[html] final case class AssetSection[F[_]](
  typeId: AssetTypeId,
  title: String,
  teaches: F[UUID],
  assesses: F[UUID],
  paragraphs: F[String],
) extends PageSection[F]:

  override def mapK[K[_]: Functor](f: F ~> K): AssetSection[K] =
    AssetSection(typeId, title, f(teaches), f(assesses), f(paragraphs))
end AssetSection

private[html] final case class LessonSection[F[_]: Functor](
  title: String,
  teaches: F[UUID],
  assesses: F[UUID],
  assets: F[AssetSection[F]],
) extends PageSection[F]:
  override val typeId: AssetTypeId = AssetTypeId.Lesson

  override def mapK[K[_]: Functor](f: F ~> K): LessonSection[K] =
    LessonSection(title, f(teaches), f(assesses), f(Functor[F].map(assets)(_.mapK(f))))
end LessonSection
