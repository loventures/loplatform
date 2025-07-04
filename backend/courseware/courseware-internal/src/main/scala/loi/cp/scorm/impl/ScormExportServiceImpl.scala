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

package loi.cp.scorm.impl

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.service.CurrentUrlService
import com.learningobjects.cpxp.util.{FileInfo, HttpUtils, LocalFileInfo, MimeUtils}
import loi.authoring.asset.factory.AssetTypeId
import loi.cp.content.{CourseContent, CourseContents}
import loi.cp.course.lightweight.LightweightCourse
import loi.cp.reference.EdgePath
import loi.cp.scorm.*
import loi.cp.scorm.scalaxb.DataRecord
import loi.cp.scorm.v12.*
import org.apache.commons.io.output.CloseShieldOutputStream
import scala.util.Using
import scalaz.syntax.std.option.*
import scaloi.syntax.any.*

import java.io.{FileOutputStream, OutputStream}
import java.nio.charset.StandardCharsets
import java.util.zip.{ZipEntry, ZipOutputStream}

@Service
class ScormExportServiceImpl(
  urlService: CurrentUrlService,
) extends ScormExportService:
  import ScormExportServiceImpl.*

  override def exportScorm(
    oﬀering: LightweightCourse,
    contents: CourseContents,
    packageId: String,
    format: ScormFormat,
  ): FileInfo =
    LocalFileInfo.tempFileInfo <| { zipFile =>
      Using.resource(new FileOutputStream(zipFile.getFile)) { fos =>
        exportScorm(oﬀering, contents, packageId, format, fos)
      }
      zipFile.setContentType(MimeUtils.MIME_TYPE_APPLICATION_ZIP)
      zipFile.setDisposition(
        HttpUtils.getDisposition(HttpUtils.DISPOSITION_ATTACHMENT, s"${oﬀering.getName}_${packageId}.zip")
      )
    }

  override def exportScorm(
    oﬀering: LightweightCourse,
    contents: CourseContents,
    packageId: String,
    format: ScormFormat,
    os: OutputStream
  ): Unit =
    Using.resource(new ZipOutputStream(os)) { zos =>
      zos.putEntry("imsmanifest.xml", manifestXml(oﬀering, contents, format))
      zos.putDirectory(HtmlPath)
      format match
        case ScormFormat.CourseEntry =>
          zos.putEntry(resourcePath(contents.tree.rootLabel.edgePath), scormContent(packageId, None))

        case ScormFormat.CourseWithNavigation =>
          contents.nonRootElements.filter(leafy) foreach { content =>
            zos.putEntry(resourcePath(content.edgePath), scormContent(packageId, content.some))
          }
    }

  private def scormContent(packageId: String, content: Option[CourseContent]): xml.Node =
    <html>
      <head>
        <script type="text/javascript">{
      s"""
          window.lo_pageId = ${content.cata(c => s"'${c.edgePath}'", "null")};
        """
    }</script>
        <script type="text/javascript" src={scriptUrl(packageId)}></script>
      </head>
      <body style="padding: 0; margin: 0"></body>
    </html>

  // Can't serve this via CDN because our CDN URL may change and this is shipped software
  private def scriptUrl(packageId: String): String =
    urlService.getUrl(s"/scorm/player/$packageId.js")

  private def leafy(content: CourseContent): Boolean = !ContainerTypes.contains(content.asset.assetType.id)

  private def resourcePath(edgePath: EdgePath): String = s"$HtmlPath$edgePath.html"

  private def manifestXml(oﬀering: LightweightCourse, contents: CourseContents, format: ScormFormat): xml.Node =
    scalaxb
      .toXML[ManifestType](manifestType(oﬀering, contents, format), "manifest", ScormScope)
      .head

  private def manifestType(
    oﬀering: LightweightCourse,
    contents: CourseContents,
    format: ScormFormat
  ): ManifestType =
    ManifestType(
      metadata = MetadataType(
        schema = "ADL SCORM".some,
        schemaversion = "1.2".some,
        grpu46anySequence3 = Grpu46anySequence(),
      ).some,
      organizations = OrganizationsType(
        organization = OrganizationType(
          title = oﬀering.getName.some,
          item = format match
            case ScormFormat.CourseEntry          =>
              itemType(oﬀering) :: Nil
            case ScormFormat.CourseWithNavigation =>
              contents.tree.foldTree(itemType).item
          ,
          metadata = None,
          grpu46anySequence4 = Grpu46anySequence(),
          attributes = Map(
            "@identifier" -> DataRecord("cp_org"),
          ),
        ) :: Nil,
        grpu46anySequence2 = Grpu46anySequence(),
        attributes = Map(
          "@default" -> DataRecord("cp_org"),
        ),
      ),
      resources = ResourcesType(
        resource = format match
          case ScormFormat.CourseEntry          =>
            resourceType(contents.tree.rootLabel) :: Nil
          case ScormFormat.CourseWithNavigation =>
            contents.nonRootElements.filter(leafy).map(resourceType)
        ,
        grpu46anySequence2 = Grpu46anySequence(),
        attributes = Map.empty,
      ),
      manifest = Nil,
      grpu46anySequence5 = Grpu46anySequence(),
      attributes = Map(
        "@version"    -> DataRecord("1"),
        "@identifier" -> DataRecord("org.example.scorm.12"),
      ),
    )

  private def itemType(oﬀering: LightweightCourse): ItemType =
    ItemType(
      title = oﬀering.getName.some,
      item = Nil,
      metadata = None,
      grpu46anySequence4 = Grpu46anySequence(),
      attributes = Map(
        "@identifier"    -> DataRecord("item__root_"),
        "@identifierref" -> DataRecord("resource__root_")
      ),
    )

  private def itemType(content: CourseContent, children: List[ItemType]): ItemType =
    ItemType(
      title = content.title.some,
      item = children,
      metadata = None,
      grpu46anySequence4 = Grpu46anySequence(),
      attributes = Map(
        "@identifier" -> DataRecord(s"item_${content.edgePath}")
      ) ++ (if leafy(content) then Map("@identifierref" -> DataRecord(s"resource_${content.edgePath}")) else Map.empty),
    )

  private def resourceType(content: CourseContent): ResourceType =
    ResourceType(
      metadata = None,
      file = FileType(
        metadata = None,
        grpu46anySequence2 = Grpu46anySequence(),
        attributes = Map(
          "@href" -> DataRecord(new java.net.URI(resourcePath(content.edgePath))),
        ),
      ) :: Nil,
      dependency = Nil,
      grpu46anySequence4 = Grpu46anySequence(),
      attributes = Map(
        "@identifier"     -> DataRecord(s"resource_${content.edgePath}"),
        "@type"           -> DataRecord("webcontent"),
        "@href"           -> DataRecord(new java.net.URI(resourcePath(content.edgePath))),
        "adlcp:scormtype" -> DataRecord("http://www.adlnet.org/xsd/adlcp_rootv1p2".some, "scormtype".some, "sco"),
      ),
    )
end ScormExportServiceImpl

object ScormExportServiceImpl:
  final val ScormScope = scalaxb.toScope(
    None         -> "http://www.imsproject.org/xsd/imscp_rootv1p1p2",
    "adlcp".some -> "http://www.adlnet.org/xsd/adlcp_rootv1p2",
    "xml".some   -> "http://www.w3.org/XML/1998/namespace",
    "xs".some    -> "http://www.w3.org/2001/XMLSchema",
    "xsi".some   -> "http://www.w3.org/2001/XMLSchema-instance",
  )

  final val ContainerTypes: Set[AssetTypeId] =
    Set(AssetTypeId.Course, AssetTypeId.Lesson, AssetTypeId.Module, AssetTypeId.Unit)

  final val HtmlPath = "html/"

  final implicit class ZipOutputStreamOps(private val self: ZipOutputStream) extends AnyVal:
    def putEntry(path: String, node: xml.Node): Unit          =
      putNextEntry(path)
      write(node)
      self.closeEntry()
    def putEntry(path: String, f: OutputStream => Unit): Unit =
      putNextEntry(path)
      f(CloseShieldOutputStream.wrap(self))
      self.closeEntry()
    def putDirectory(path: String): Unit                      =
      putNextEntry(path)
      self.closeEntry()
    def putNextEntry(path: String): Unit                      = self.putNextEntry(new ZipEntry(path))
    def write(node: xml.Node): Unit                           = write(format(node))
    def write(str: String): Unit                              = self.write(str.getBytes(StandardCharsets.UTF_8))
    private def format(node: xml.Node): String                = new scala.xml.PrettyPrinter(200, 2).format(node) // stately shame
  end ZipOutputStreamOps
end ScormExportServiceImpl
