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

package loi.authoring.index

import com.google.common.io.ByteStreams
import com.learningobjects.de.web.MediaType
import loi.authoring.blob.{BlobRef, BlobService}
import loi.authoring.syntax.index.*
import net.htmlparser.jericho.Source
import org.apache.commons.io.IOUtils
import org.apache.pdfbox.Loader
import org.apache.pdfbox.text.PDFTextStripper
import org.apache.poi.extractor.ExtractorFactory
import org.mozilla.universalchardet.{Constants, UnicodeBOMInputStream, UniversalDetector}
import scalaz.std.list.*
import scalaz.std.string.*
import scalaz.syntax.std.option.*
import scaloi.syntax.any.*
import scaloi.syntax.boolean.*
import scaloi.syntax.option.*
import scaloi.syntax.ʈry.*

import java.io.{BufferedInputStream, InputStream, InputStreamReader}
import scala.util.{Try, Using}

/** Type that extracts a string from a stream. */
trait Extractor:

  /** Extract the plaintext content of an [[InputStream]]. */
  def extract(in: InputStream): String // In my imagination I would stream directly to the search cluster but..

/** Type that supports extracting plaintext from supported blobs. */
trait BlobExtractor extends Extractor:

  /** Does this class support a particular blob. */
  def supports(fileName: String, contentType: MediaType, size: Long): Boolean

object BlobExtractor:
  private final val logger = org.log4s.getLogger

  /** Get the strings from a [[BlobLike]]. */
  def blobStrings[A: BlobLike](a: A): List[String] =
    getExtractor(a)
      .tapNone(
        logger.info(s"Unhandled blob: $a")
      )
      .foldZ(extractText(_, a) :: Nil)

  /** Extract the HTML content from a [[BlobLike]] [[A]] if possible. */
  def blobHtml[A: BlobLike](a: A): Option[String] =
    HtmlExtractor
      .supports(BlobLike[A].fileName(a), BlobLike[A].contentType(a), BlobLike[A].size(a))
      .flatOption(
        extractRaw(a, HtmlExtractor.extractHtml).toOption
      )

  /** Find an [[Extractor]] that supports the given [[BlobLike]]. */
  def getExtractor[A: BlobLike](a: A): Option[Extractor] =
    SupportedExtractors.find(_.supports(BlobLike[A].fileName(a), BlobLike[A].contentType(a), BlobLike[A].size(a)))

  /** Extract the text from a [[BlobLike]] [[A]] using the given [[Extractor]]. */
  def extractText[A: BlobLike](extractor: Extractor, a: A): String =
    extractRaw(a, extractor.extract).getOrElse("")

  def extractRaw[A: BlobLike, B](a: A, fAB: InputStream => B): Try[B] =
    Try {
      Using.resource(BlobLike[A].open(a))(fAB)
    } tapFailure { e =>
      logger.warn(e)(s"Error extracting blob: $a")
    }

  private final val SupportedExtractors = List(TextExtractor, HtmlExtractor, PdfExtractor, OoxmlExtractor)

  /** Type class describing blob like things. */
  trait BlobLike[A]:
    def toString(a: A): String
    def fileName(a: A): String
    def contentType(a: A): MediaType
    def size(a: A): Long

    /** Open a closable markable input stream from [[A]]. */
    def open(a: A): InputStream

  object BlobLike:
    implicit def apply[A: BlobLike]: BlobLike[A] = implicitly

    /** Evidence for how a [[BlobRef]] is [[BlobLike]]. */
    implicit def blobRefBlobLike(implicit blobService: BlobService): BlobLike[BlobRef] =
      new BlobLike[BlobRef]:
        override def toString(a: BlobRef): String       = a.toString
        override def fileName(a: BlobRef): String       = a.filename
        override def contentType(a: BlobRef): MediaType = a.contentType
        override def size(a: BlobRef): Long             = a.size
        override def open(a: BlobRef): InputStream      = new BufferedInputStream(blobService.ref2Stream(a))
  end BlobLike
end BlobExtractor

/** Handy base extractor that knows a bit about text files.
  * @param maximumSize
  *   the maximum file size this will accept
  * @param mediaTypes
  *   the media types this will accept
  */
abstract class BaseExtractor(maximumSize: Long, mediaTypes: MediaType*) extends BlobExtractor:
  import BaseExtractor.*

  override def supports(fileName: String, contentType: MediaType, size: Long): Boolean =
    (size < maximumSize) && mediaTypes.exists(_.includes(contentType))

  /** Infer the character set of this file and return a [[InputStreamReader]] to read the text content. */
  protected def reader(in: InputStream): InputStreamReader =
    val charset = in.markReset(maximumSize.toInt)(i =>
      (Option(UniversalDetector.detectCharset(i)) - Constants.CHARSET_GB18030) | "UTF-8"
    )
    new InputStreamReader(in.transformIf(charset.contains("UTF"))(new UnicodeBOMInputStream(_)), "UTF-8")
end BaseExtractor

object BaseExtractor:
  implicit class InputStreamOps(private val self: InputStream) extends AnyVal:

    /** Perform an operation on a stream, wrapped by [mark()] and [reset()].
      * @param amount
      *   the maximum amount of data to buffer
      */
    def markReset[A](amount: Int)(f: InputStream => A): A =
      self.mark(amount)
      val a = f(self)
      self.reset()
      a
  end InputStreamOps
end BaseExtractor

/** Support for extracting the text from plain text files. */
object TextExtractor extends BaseExtractor(1.megabyte, MediaType.TEXT_PLAIN, MediaType.TEXT_VTT):
  override def extract(in: InputStream): String = IOUtils.toString(reader(in))

/** Support for extracting the text from HTML files. */
object HtmlExtractor extends BaseExtractor(5.megabytes, MediaType.TEXT_HTML):
  override def extract(in: InputStream): String = fromHtml(extractHtml(in))

  def extractHtml(in: InputStream): String = IOUtils.toString(reader(in))

  /** Extract plaintext from a HTML string. */
  def fromHtml(html: String): String = fromHtml(new Source(html))

  /** Extract plaintext from a [[Source]]. Includes some relevant attributes in addition to text content. */
  private def fromHtml(source: Source): String =
    source.getTextExtractor.setIncludeAttributes(true).toString // TODO: more attributes, like aria?
end HtmlExtractor

/** Support for extracting the text from PDF files. */
object PdfExtractor extends BaseExtractor(20.megabytes, MediaType.APPLICATION_PDF):
  override def extract(in: InputStream): String =
    val bytes = ByteStreams.toByteArray(in)
    Using.resource(Loader.loadPDF(bytes)) { document =>
      if !document.getCurrentAccessPermission.canExtractContent then throw new Exception("Content extraction denied")
      new PDFTextStripper().getText(document)
    }

/** Support for extracting the text from OOXML files (docx/xlsx/pptx). */
object OoxmlExtractor
    extends BaseExtractor(
      20.megabytes,
      MediaType.APPLICATION_OOXML_DOC,
      MediaType.APPLICATION_OOXML_XLS,
      MediaType.APPLICATION_OOXML_PPT
    ):
  override def extract(in: InputStream): String =
    ExtractorFactory.createExtractor(in).getText
