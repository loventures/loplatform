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

package loi.authoring.read.web

import argonaut.CodecJson
import cats.syntax.option.*
import com.fasterxml.jackson.databind.node.ArrayNode
import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.web.*
import com.learningobjects.cpxp.component.{BaseComponent, ComponentInstance}
import com.learningobjects.de.authorization.Secured
import loi.asset.course.model.Course
import loi.asset.survey.Survey1
import loi.authoring.ProjectId
import loi.authoring.edge.*
import loi.authoring.node.AssetNodeService
import loi.authoring.project.*
import loi.authoring.security.right.AccessAuthoringAppRight
import loi.authoring.web.AuthoringWebUtils
import loi.authoring.workspace.*
import scalaz.std.list.*
import scalaz.syntax.foldable.*
import scalaz.syntax.std.boolean.*
import scaloi.json.ArgoExtras
import scaloi.misc.Monoids.rightBiasMapMonoid
import scaloi.syntax.option.*
import scaloi.syntax.set.*

import java.util.UUID
import scala.jdk.CollectionConverters.*

@Component
@Controller(root = true, value = "authoring-read-web-controller")
@Secured(Array(classOf[AccessAuthoringAppRight]))
private[web] class ReadWebController(
  ci: ComponentInstance,
  workspaceService: WorkspaceService,
  webUtils: AuthoringWebUtils,
  edgeService: EdgeService,
  nodeService: AssetNodeService,
  commitDao2: CommitDao2,
) extends BaseComponent(ci)
    with ApiRootComponent:

  import ReadWebController.*

  /** Gets the JSON for the graph of a branch, elements, rubrics, questions, alignment, competencies.
    *
    * @param since
    *   request for a compact delta structure of only those changes that have occurred since the specified commit.
    * @param include
    *   request to also include the specified assets that may not be in the project graph
    */
  @RequestMapping(path = "authoring/{branchId}/nodes/{name}/structure", method = Method.GET)
  def loadStructure(
    @PathVariable("branchId") branchId: Long,
    @PathVariable("name") nodeName: String,
    @QueryParam(value = "since", decodeAs = classOf[Long]) sinceCommit: Option[Long],
    @QueryParam("include") include: List[UUID],
  ): StructureResponse =
    val ws   = webUtils.workspaceOrThrow404(branchId, cache = false)
    val node = webUtils.nodeOrThrow404(ws, nodeName)
    if node.is[Course] then deltaStructure(ws, sinceCommit) getOrElse courseStructure(ws, include)
    else
      val graph = edgeService.stravaigeOutGraph(
        if node.is[Survey1] then
          TraverseGraph
            .fromSource(node.info.name)
            .traverse(Group.Questions)
            .noFurther
        else
          TraverseGraph
            .fromSource(node.info.name)
            .traverse(elementGroups*)
            .traverse(elementGroups*)
            .traverse(elementGroups*)
            .traverse(elementGroups*)
        ,
        ws
      )
      StructureResponse(ws, graph)
    end if
  end loadStructure

  // If the delta since a prior commit was requested and all ops in those commits are
  // setNodeData then return just the nodes that have been edited since that commit.
  private def deltaStructure(ws: AttachedReadWorkspace, sinceCommit: Option[Long]): Option[StructureResponse] =
    for
      since  <- sinceCommit
      commits = if since == ws.commitId then Nil else loadAncestors(ws, since)
      ops     = commits.flatMap(_.ops.asInstanceOf[ArrayNode].elements.asScala)
      if commits.lastOption.mapNonNull(_.parent).forall(_.id == since) && ops.forall(
        _.path("op").textValue == "setNodeData"
      )
    yield
      val names = ops.map(_.path("name").textValue).map(UUID.fromString).distinct
      logger.info(
        s"Delta read $since -> ${ws.commitId} found ${names.length} names in ${commits.length} commits and ${ops.length} ops"
      )
      val nodes = nodeService.load(ws).byName(names).get
      StructureResponse(ws, TraversedGraph(nodes, Nil), delta = true)

  private def loadAncestors(ws: AttachedReadWorkspace, since: Long): Seq[CommitEntityish] =
    commitDao2.loadAncestorsUntilCommit(ws.commitId, since, MaxDeltaCommits)

  @RequestMapping(path = "authoring/{branchId}/commits/{commit}/structure", method = Method.GET)
  def loadStructureByCommit(
    @PathVariable("branchId") branchId: Long,
    @PathVariable("commit") commitId: Long,
  ): StructureResponse =
    val ws = webUtils.workspaceAtCommitOrThrow404(branchId, commitId)
    courseStructure(ws, Nil)

  @RequestMapping(path = "authoring/{branchId}/relatedCompetencies", method = Method.GET)
  def relatedCompetencies(
    @PathVariable("branchId") branchId: Long,
  ): StructureResponse =
    relatedNodes(
      branchId,
      ws =>
        TraverseGraph
          .fromSource(ws.rootName)
          .traverse(Group.CompetencySets)
          .traverse(Group.Level1Competencies)
          .traverse(Group.Level2Competencies)
          .traverse(Group.Level3Competencies)
    )

  @RequestMapping(path = "authoring/{branchId}/relatedCategories", method = Method.GET)
  def relatedCategories(
    @PathVariable("branchId") branchId: Long,
  ): StructureResponse =
    relatedNodes(
      branchId,
      ws =>
        TraverseGraph
          .fromSource(ws.homeName)
          .traverse(Group.GradebookCategories)
          .noFurther
    )

  // Loads assets from projects that are related (branch linked from or dependencies of) this branch
  private def relatedNodes(
    branchId: Long,
    lairdyGraph: AttachedReadWorkspace => TraverseFromSourcesAnyTargetType[UUID]
  ): StructureResponse =
    webUtils.workspaceOrThrow404(branchId) match
      case lws: LayeredWorkspace =>
        // The related nodes won't be in the workspace so load a workspace for
        // each basis project.
        val structures = for
          (projectId, dep) <- lws.depInfos.toList
          rws              <- workspaceService.loadReadWorkspaceAtCommit(projectId, dep.commitId, AccessRestriction.none)
        yield
          val graph         = edgeService.stravaigeOutGraphs(List(lairdyGraph(rws)), rws)
          val assetBranches = graph.nodesByName.keySet.mapTo(_ => projectId)
          StructureResponse(lws, graph, None, None, None, None, assetBranches.some)
        StructureResponse(lws, TraversedGraph(Nil, Nil))
          .copy(
            edges = structures.foldMap(_.edges),
            nodes = structures.foldMap(_.nodes),
            assetBranches = structures.foldMap(_.assetBranches.get).some
          )

  private def courseStructure(ws: AttachedReadWorkspace, include: List[UUID]): StructureResponse =
    val graph                                                 = courseGraph(ws, include)
    val (branchCommits, nodeOriginProjects, customizedAssets) = ws match
      case lws: ProjectWorkspace =>
        // This is espensive
        val depCommitIds       = lws.depInfos.view.mapValues(_.commitId).toMap
        val initializedCommits = commitDao2.loadWithInitializedDocs(depCommitIds.values)
        val fuzzyLayerBases    = for
          (projectId, depInfo) <- lws.depInfos
          initializedCommit    <- initializedCommits.get(depInfo.commitId)
        yield (projectId, initializedCommit.comboDoc.toFuzzyLayerBase)

        val customizedAssets   = getCustomizedAssets(lws, fuzzyLayerBases)
        val nodeOriginProjects = lws.nodeElems.filterNot(_.isLocal).map(e => e.name -> e.projectId).toMap
        (depCommitIds, nodeOriginProjects, customizedAssets.toSet)
    StructureResponse(
      ws,
      graph,
      ws.bronchId.some,
      ws.rootName.some,
      ws.homeName.some,
      branchCommits.some,
      nodeOriginProjects.some,
      customizedAssets.some,
    )
  end courseStructure

  private def getCustomizedAssets(
    lws: ProjectWorkspace,
    layerBases: Map[ProjectId, FuzzyLayerBase]
  ): Iterable[UUID] =

    val customizedNames = Set.newBuilder[UUID]

    // remote sources of local edges are deemed customized
    for
      edgeAttrs <- lws.localLayer.edgeAttrs
      src       <- lws.getNodeElem(edgeAttrs.srcName) if !src.isLocal
    do customizedNames.addOne(src.name)

    // customized remote nodes
    for
      projectId  <- lws.depInfos.keys
      layer      <- lws.getLayer(projectId)
      base       <- layerBases.get(projectId)
      (name, id) <- layer.nodeNameIds
      baseId     <- base.nodeIds.get(name)
      if id != baseId
    do customizedNames.addOne(name)

    // sources of excluded and customized remote edges
    for
      projectId  <- lws.depInfos.keys
      layer      <- lws.getLayer(projectId)
      base       <- layerBases.get(projectId)
      (name, id) <- layer.rawEdgeIds // negative ids desired
      baseId      = base.fuzzyEdgeIds.get(name)
      if !baseId.contains(id)
      attrs      <- lws.getDocEdgeAttrs(name)
    // excluded or expressed edges that don't equal baseId clearly mark the source as custom
    // but an unexpressed edge? how could its id not equal baseId? /shrug
    do customizedNames.addOne(attrs.srcName)
    end for

    customizedNames.result()
  end getCustomizedAssets

  private def courseGraph(ws: AttachedReadWorkspace, include: List[UUID]): TraversedGraph =
    edgeService.stravaigeOutGraphs(
      List(
        TraverseGraph
          .fromSource(ws.rootName)
          .traverse(Group.Courses)
          .noFurther,
        TraverseGraph
          .fromSource(ws.rootName)
          .traverse(Group.CompetencySets)
          .traverse(Group.Level1Competencies)
          .traverse(Group.Level2Competencies)
          .traverse(Group.Level3Competencies),
        TraverseGraph
          .fromSource(ws.homeName)
          .traverse(Group.Elements, Group.Image) // Course -> Unit
          .traverse(elementGroups*)              // Unit -> Module
          .traverse(elementGroups*)              // Module -> Lesson
          .traverse(elementGroups*)              // Lesson -> Content
          .traverse(elementGroups*)              // Content -> Question
          .traverse(elementGroups*)              // Question|Rubric-> Rubric|Criteria
          .traverse(elementGroups*)              // Rubric|Criteria -> Criteria|Competency
          .traverse(elementGroups*),             // Criteria -> Competency
        TraverseGraph
          .fromSource(ws.homeName)
          .traverse(Group.GradebookCategories)
          .noFurther,
      ) ::: include.nonEmpty
        .option(
          TraverseGraph
            .fromSources(include*)
            .traverse(elementGroups*)  // Unit -> Module
            .traverse(elementGroups*)  // Module -> Lesson
            .traverse(elementGroups*)  // Lesson -> Content
            .traverse(elementGroups*)  // Content -> Question
            .traverse(elementGroups*)  // Question|Rubric-> Rubric|Criteria
            .traverse(elementGroups*)  // Rubric|Criteria -> Criteria|Competency
            .traverse(elementGroups*), // Criteria -> Competency
        )
        .toList,
      ws
    )
  end courseGraph

  /** Gets the JSON for the graph of a rubric
    */
  @RequestMapping(path = "authoring/{branchId}/nodes/{name}/rubric", method = Method.GET)
  def loadRubricGraph(
    @PathVariable("branchId") branchId: Long,
    @PathVariable("name") nodeName: String
  ): StructureResponse =
    val rws   = webUtils.workspaceOrThrow404(branchId, cache = false)
    val root  = webUtils.nodeOrThrow404(rws, nodeName)
    val graph = edgeService.stravaigeOutGraph(
      TraverseGraph
        .fromSource(root.info.name)
        .traverse(Group.Criteria)
        .traverse(Group.Assesses),
      rws
    )
    StructureResponse(rws, graph)
  end loadRubricGraph

  @RequestMapping(path = "authoring/commit/{commit}/nodes/{name}/structure", method = Method.GET)
  def loadNodeStructureByCommit(
    @PathVariable("commit") commitId: Long,
    @PathVariable("name") nodeName: String,
    @QueryParam(value = "q", required = true) jsonReq: ArgoBody[ReadRequest]
  ) =
    val req      = jsonReq.decode_!.get
    val rws      = webUtils.detachedWorkspaceOrThrow404(commitId, cache = false)
    val root     = webUtils.nodeOrThrow404(rws, nodeName)
    val start    = TraverseGraph.fromSource(root.info.name).traverse(parseGroups(req.groupSteps.head)*).noFurther
    val traverse = req.groupSteps.tail.foldLeft(start)({ case (t, step) => t.traverse(parseGroups(step)*) })
    val graph    = edgeService.stravaigeOutGraph(traverse, rws)
    StructureResponse(rws, graph)
  end loadNodeStructureByCommit

  private def parseGroups(names: List[String]): List[Group] = names.map(Group.withName)
end ReadWebController

private object ReadWebController:
  private val logger = org.log4s.getLogger

  final val MaxDeltaCommits = 16

  val elementGroups =
    List(
      Group.Elements,
      Group.Questions,
      Group.Survey,
      Group.GradebookCategory,
      Group.Assesses,
      Group.Teaches,
      Group.CblRubric,
      Group.Criteria,
      Group.Hyperlinks,
      Group.Gates,
      Group.TestsOut,
      Group.Resources,        // images attached to resource.1/html.1
      Group.InSystemResource, // legacy, attachments for resource.1
      Group.Dependencies,     // legacy
      Group.Scripts,          // legacy
      Group.Stylesheets,      // legacy
    )
end ReadWebController

private case class ReadRequest(groupSteps: List[List[String]])

private object ReadRequest:
  implicit def codec: CodecJson[ReadRequest] =
    CodecJson.casecodec1(ReadRequest.apply, ArgoExtras.unapply1)("groupSteps")
