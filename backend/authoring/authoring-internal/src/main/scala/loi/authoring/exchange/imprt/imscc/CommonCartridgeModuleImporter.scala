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

package loi.authoring.exchange.imprt.imscc

import com.learningobjects.cpxp.component.annotation.Service
import loi.authoring.asset.factory.AssetTypeId
import loi.authoring.edge.Group
import loi.authoring.exchange.imprt.ImporterUtils.{buildEmptyEdgesFromAssets, guid}
import loi.authoring.exchange.imprt.{NodeExchangeBuilder, NodeFamily}

import scala.collection.mutable.ListBuffer
import scala.xml.{Elem, Node}

@Service
class CommonCartridgeModuleImporter:

  def buildModules(manifestXml: Node, resources: Map[String, NodeFamily]): Seq[NodeFamily] =
    getModuleItems(manifestXml, resources).collect { case node: Elem =>
      buildModule(node, resources)
    }

  private def getModuleItems(manifestXml: Node, resources: Map[String, NodeFamily]): Seq[Node] =
    def isIgnorableRootModule(rootItems: Seq[Node]): Boolean =
      rootItems.size == 1 && (rootItems.head \ "title").isEmpty && rootItems.head.child.nonEmpty

    /*
     Combine all neighboring root resource nodes under a module node and leave modules alone.
     */
    def combineNeighboringResources(nodes: Seq[Node]): Seq[Node] =
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
        case item: Elem if item.label == "item" && isModuleOrLesson(item)      =>
          if combiningNeighbors then
            combiningNeighbors = false
            index += 1
          buf += item
          index += 1
        case item: Elem if item.label == "item" && isResource(item, resources) =>
          if combiningNeighbors then buf(index) = appendChild(buf(index), item)
          else
            combiningNeighbors = true
            buf += <item><title>{(item \ "title").text}</title>{item}</item>
        case _                                                                 => /* Ignore non-items */
      }
      buf
    end combineNeighboringResources

    val rootItems = manifestXml \ "organizations" \ "organization" \ "item"

    /* Client manifests can contain a root item, often identified as "LearningModules", which gets ignored. */
    val newRootItems = if isIgnorableRootModule(rootItems) then rootItems.head.child else rootItems

    combineNeighboringResources(newRootItems)
  end getModuleItems

  private def buildModule(moduleNode: Node, resources: Map[String, NodeFamily]): NodeFamily =
    val title    = (moduleNode \ "title").text
    val elements = (moduleNode \ "item").collect {
      case item: Elem if isResource(item, resources) => resources(getResourceId(item))
      case item: Elem if isModuleOrLesson(item)      => buildLesson(item, resources)
    }

    val module = NodeExchangeBuilder
      .builder(guid, AssetTypeId.Module.entryName)
      .title(title)
      .edges(buildEmptyEdgesFromAssets(elements.map(_.node).toList, Group.Elements))
      .build()

    val assets = elements.flatMap(_.family).toList ::: List(module)
    NodeFamily(module, assets)
  end buildModule

  private def isResource(item: Node, resources: Map[String, NodeFamily]): Boolean =
    val resourceId = getResourceId(item)
    resourceId.nonEmpty && resources.contains(resourceId)

  private def isModuleOrLesson(item: Node): Boolean = (item \ "@identifierref").isEmpty

  private def getResourceId(item: Node): String = (item \ "@identifierref").text

  private def buildLesson(lessonNode: Node, resources: Map[String, NodeFamily]): NodeFamily =
    val title    = (lessonNode \ "title").text
    val elements = getResources(lessonNode, resources)
    val lesson   = NodeExchangeBuilder
      .builder(guid, AssetTypeId.Lesson.entryName)
      .title(title)
      .edges(buildEmptyEdgesFromAssets(elements.map(_.node).toList, Group.Elements))
      .build()
    val assets   = elements.flatMap(_.family) :+ lesson
    NodeFamily(lesson, assets)
  end buildLesson

  private def getResources(lessonNode: Node, resources: Map[String, NodeFamily]): Seq[NodeFamily] =
    (lessonNode \ "item").collect {
      case item: Elem if isResource(item, resources) => Seq(resources(getResourceId(item)))
      case item: Elem if isModuleOrLesson(item)      => getResources(item, resources)
    }.flatten
end CommonCartridgeModuleImporter
