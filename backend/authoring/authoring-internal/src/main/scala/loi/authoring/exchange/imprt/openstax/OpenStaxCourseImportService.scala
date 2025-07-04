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

import com.learningobjects.cpxp.component.annotation.Service
import loi.asset.license.License
import loi.authoring.asset.factory.AssetTypeId
import loi.authoring.edge.Group
import loi.authoring.exchange.imprt.ImporterUtils.*
import loi.authoring.exchange.imprt.NodeExchangeBuilder
import loi.authoring.exchange.model.NodeExchangeData

import scala.xml.{Elem, Node}

@Service
class OpenStaxCourseImportService:

  def buildAssetsFromCollection(
    collection: Elem,
    licenseAndAuthor: (License, String),
    assetMap: Map[String, List[NodeExchangeData]] = Map()
  ): List[NodeExchangeData] =
    val descendants = (collection \ "content").head.child
      .collect {
        case subcollection: Elem if subcollection.label == "subcollection" =>
          buildModuleAndDescendantsFromSubcollection(subcollection, assetMap, licenseAndAuthor)
        case module: Elem if module.label == "module"                      =>
          buildModuleAndDescendantsFromOpenStaxModule(module, assetMap, licenseAndAuthor)
      }
      .toList
      .flatten
    val modules     = descendants.filter(_.typeId == AssetTypeId.Module.entryName)
    val course      = buildCourseAssetFromCollection(collection, modules, licenseAndAuthor)
    descendants ::: List(course)
  end buildAssetsFromCollection

  def getAuthorAndLicense(collection: Elem): (License, String) =
    val licenseUrl = (collection \ "license").text
    val authorId   = (collection \ "roles").find(role => (role \ "@type").text == "author").map(_.text)
    val actors     = collection \\ "person"

    val personNode = authorId
      .map(id => actors.find(actor => (actor \ "@userid").text == id).getOrElse(actors.head))
      .getOrElse(actors.head)

    (License.CC_BY, (personNode \ "fullname").text)
  end getAuthorAndLicense

  private def buildModuleAndDescendantsFromSubcollection(
    subcollection: Node,
    assetMap: Map[String, List[NodeExchangeData]],
    licenseAndAuthor: (License, String)
  ): List[NodeExchangeData] =
    val children                = (subcollection \ "content").head.child
      .collect {
        case subcollection: Elem if subcollection.label == "subcollection" =>
          getChildrenForSubcollection(subcollection, assetMap, licenseAndAuthor)
        case module: Elem if module.label == "module"                      => getChildrenForModule(module, assetMap)
      }
      .toList
      .flatten
    val asset: NodeExchangeData =
      buildAssetFromSubcollection(subcollection, children, AssetTypeId.Module, licenseAndAuthor)
    val playlists               = children.filterNot(c => assetMap.values.flatten.exists(a => a.id == c.id))
    playlists ::: List(asset)
  end buildModuleAndDescendantsFromSubcollection

  private def buildModuleAndDescendantsFromOpenStaxModule(
    module: Node,
    assetMap: Map[String, List[NodeExchangeData]],
    licenseAndAuthor: (License, String)
  ): List[NodeExchangeData] =
    val title       = (module \ "title").text
    val assets      = getChildrenForModule(module, assetMap)
    val edges       = buildEmptyEdgesFromAssets(assets, Group.Elements)
    val moduleAsset = NodeExchangeBuilder
      .builder(guid, AssetTypeId.Module.entryName)
      .title(title)
      .edges(edges)
      .licenseAndAuthor(licenseAndAuthor)
      .build()
    List(moduleAsset)
  end buildModuleAndDescendantsFromOpenStaxModule

  private def getChildrenForModule(
    module: Node,
    assetMap: Map[String, List[NodeExchangeData]]
  ): List[NodeExchangeData] =
    val moduleId = module.attribute("document").head.text
    assetMap.getOrElse(moduleId, List()).filter(_.typeId == AssetTypeId.Lesson.entryName)

  private def getChildrenForSubcollection(
    collection: Node,
    assetMap: Map[String, List[NodeExchangeData]],
    licenseAndAuthor: (License, String)
  ): List[NodeExchangeData] =
    // TODO prefix subcollection title before the module title
    (collection \ "content").head.child
      .collect {
        case subcollection: Elem if subcollection.label == "subcollection" =>
          getChildrenForSubcollection(subcollection, assetMap, licenseAndAuthor)
        case module: Elem if module.label == "module"                      => getChildrenForModule(module, assetMap)
      }
      .toList
      .flatten

  private def buildCourseAssetFromCollection(
    collection: Node,
    children: List[NodeExchangeData],
    licenseAndAuthor: (License, String)
  ): NodeExchangeData =
    val title = (collection \ "metadata" \ "title").text
    val edges = buildEmptyEdgesFromAssets(children, Group.Elements)
    NodeExchangeBuilder
      .builder(guid, AssetTypeId.Course.entryName)
      .title(title)
      .licenseAndAuthor(licenseAndAuthor)
      .edges(edges)
      .build()
  end buildCourseAssetFromCollection

  private def buildAssetFromSubcollection(
    subcollection: Node,
    children: List[NodeExchangeData],
    assetTypeId: AssetTypeId,
    licenseAndAuthor: (License, String)
  ): NodeExchangeData =
    val title = (subcollection \ "title").text
    val edges = buildEmptyEdgesFromAssets(children, Group.Elements)
    NodeExchangeBuilder
      .builder(guid, assetTypeId.entryName)
      .title(title)
      .licenseAndAuthor(licenseAndAuthor)
      .edges(edges)
      .build()
  end buildAssetFromSubcollection
end OpenStaxCourseImportService
