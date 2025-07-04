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

package loi.cp.security

import com.learningobjects.cpxp.controller.upload.UploadInfo
import com.learningobjects.de.authorization.Secured
import loi.cp.admin.right.HostingAdminRight
import loi.cp.config.{ConfigurationKey, ConfigurationKeyBinding}
import loi.cp.i18n.Translatable
import loi.cp.security.SecuritySettings.*
import org.apache.commons.io.FilenameUtils
import scalaz.std.option.*
import scalaz.std.string.*
import scalaz.syntax.functor.*
import scalaz.syntax.std.boolean.*
import scalaz.syntax.std.option.*
import scalaz.{ValidationNel, \/}
import scaloi.syntax.boolean.*
import scaloi.syntax.option.*

import java.net.{URI, URISyntaxException}

final case class SecuritySettings(
  allowedOrigins: DelimitedString,
  frameOptions: String,
  allowedFileTypes: DelimitedString
)

object SecuritySettings:
  import loi.cp.config.JsonSchema.*

  final val JsonSchema = Schema(
    title = "Security".some,
    properties = List(
      StringField("allowedOrigins"),
      StringField("frameOptions"),
      StringField("allowedFileTypes"),
    )
  )

  // common filetypes in production on 2019.12.19
  final val AllowedFileTypes = Set(
    "accdb",
    "ai",
    // "bas",
    "blob",
    // "class",
    "cpp",
    "cr2",
    "cs",
    "css",
    "csv",
    "doc",
    "docx",
    "dotx",
    "gdoc",
    "gif",
    "heic",
    "htm",
    "html",
    "indd",
    "java",
    "jpeg",
    "jpg",
    // "js",
    "key",
    // "lnk",
    "m4a",
    "m4v",
    "mht",
    "mov",
    "mp3",
    "mp4",
    "mpp",
    "nef",
    "odt",
    "ogg",
    "pages",
    "pdf",
    "php",
    "png",
    "ppsx",
    "ppt",
    "pptx",
    "psd",
    "pub",
    "py",
    "rar",
    "rpt",
    "rtf",
    "sql",
    "svg",
    "tif",
    "tiff",
    "txt",
    // "url",
    "wav",
    "webarchive",
    "webm",
    "webp",
    "wmv",
    "xls",
    "xlsm",
    "xlsx",
    "xlt",
    "xps",
    "zip"
  )

  // https://support.symantec.com/us/en/article.info3768.html on 2019.12.19
  final val BannedFileTypes = Set(
    "ade",
    "adp",
    "app",
    "asp",
    "bas",
    "bat",
    "cer",
    "chm",
    "cla",
    "class",
    "cmd",
    "cnt",
    "com",
    "cpl",
    "crt",
    "csh",
    "der",
    "exe",
    "fxp",
    "gadget",
    "grp",
    "hlp",
    "hpj",
    "hta",
    "inf",
    "ins",
    "isp",
    "its",
    "jar",
    "js",
    "jse",
    "ksh",
    "lnk",
    "mad",
    "maf",
    "mag",
    "mam",
    "maq",
    "mar",
    "mas",
    "mat",
    "mau",
    "mav",
    "maw",
    "mcf",
    "mda",
    "mdb",
    "mde",
    "mdt",
    "mdw",
    "mdz",
    "msc",
    "msh",
    "msh1",
    "msh1xml",
    "msh2",
    "msh2xml",
    "mshxml",
    "msi",
    "msp",
    "mst",
    "ocx",
    "ops",
    "osd",
    "pcd",
    "pif",
    "pl",
    "plg",
    "prf",
    "prg",
    "ps1",
    "ps1xml",
    "ps2",
    "ps2xml",
    "psc1",
    "psc2",
    "pst",
    "reg",
    "scf",
    "scr",
    "sct",
    "shb",
    "shs",
    "tmp",
    "url",
    "vb",
    "vbe",
    "vbp",
    "vbs",
    "vsmacros",
    "vsw",
    "ws",
    "wsc",
    "wsf",
    "wsh",
    "xbap",
    "xnk",
  )

  type DelimitedString = String
  type Origin          = String
  val defaultSettings =
    SecuritySettings(allowedOrigins = "", frameOptions = "", allowedFileTypes = AllowedFileTypes.mkString(", "))

  @ConfigurationKeyBinding(
    value = "security",
    read = new Secured(Array(classOf[HostingAdminRight])),
    write = new Secured(Array(classOf[HostingAdminRight]))
  )
  object config extends ConfigurationKey[SecuritySettings]:
    import Translatable.RawStrings.*
    override val init: SecuritySettings = defaultSettings

    /** Validate that `d` conforms to some manner of restriction. */
    override def validate(d: SecuritySettings): Translatable.Any \/ Unit =
      for
        _ <- validateFrameOptions(d.frameOptions)
        _ <- validateFileTypeSuffices(d.allowedFileTypes)
        _ <- validateLegalFileTypes(d.allowedFileTypes)
      yield ()

    override def schema = JsonSchema

    private def validateFrameOptions(frameOptions: String): Translatable.Any \/ Unit =
      OptionNZ(frameOptions)
        .filterNot(FrameOption(_).isDefined)
        .as(Translatable.Any("frameOptions key must contain a valid frame option."))
        .toLeftDisjunction(())

    private def validateFileTypeSuffices(suffixes: String): Translatable.Any \/ Unit =
      val invalid = tokenize(suffixes).filterNot(SuffixRe.matches)
      invalid.isEmpty either (()) or Translatable.Any(s"fileTypes contains invalid suffixes: ${invalid.mkString(", ")}")

    private def validateLegalFileTypes(suffixes: String): Translatable.Any \/ Unit =
      val insecure = parseFileTypes(suffixes).intersect(BannedFileTypes)
      insecure.isEmpty either (()) or Translatable.Any(
        s"fileTypes contains insecure suffixes: ${insecure.mkString(", ")}"
      )
  end config

  final class AllowedOrigins private (val origins: List[Origin]) {}
  object AllowedOrigins:
    def apply(text: DelimitedString): AllowedOrigins =
      new AllowedOrigins(tokenize(text))

  private final val SuffixRe = "[_a-zA-Z0-9]+".r

  def isAllowedFilename(fileName: String, securitySettings: SecuritySettings): Boolean =
    val permittedExtensions = parseFileTypes(securitySettings.allowedFileTypes) diff BannedFileTypes
    val suffix              = FilenameUtils.getExtension(fileName)
    (suffix.isEmpty || permittedExtensions.contains(suffix.toLowerCase)) <|! {
      logger.info(s"Rejecting file type $suffix for file $fileName")
    }

  def validateUpload(security: SecuritySettings)(upload: UploadInfo): ValidationNel[String, UploadInfo] =
    isAllowedFilename(upload.getFileName, security) either upload `orInvalidNel` upload.getFileName

  def parseFileTypes(types: DelimitedString): Set[String] =
    tokenize(types).map(_.toLowerCase).toSet

  private def tokenize(text: DelimitedString): List[String] =
    Option(text).orZero.split(", *").map(_.trim).filter(_.nonEmpty).toList

  sealed abstract class FrameOption
  object FrameOption:
    def apply(text: String): Option[FrameOption] =
      OptionNZ(text) flatMap {
        case "DENY"        => Some(Deny)
        case "SAMEORIGIN"  => Some(SameOrigin)
        case ValidURL(url) => Some(AllowFrom(url))
        case _             => None
      }
    case object Deny extends FrameOption:
      override def toString: Origin = "DENY"
    case object SameOrigin extends FrameOption:
      override def toString: Origin = "SAMEORIGIN"
    final case class AllowFrom(url: URI) extends FrameOption:
      override def toString: Origin = s"ALLOW-FROM $url"

    private object ValidURL:
      def unapply(s: String): Option[URI] =
        try Some(new URI(s))
        catch case _: URISyntaxException => None
  end FrameOption

  private final val logger = org.log4s.getLogger
end SecuritySettings
