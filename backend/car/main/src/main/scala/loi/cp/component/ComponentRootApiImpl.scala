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

package loi.cp.component

import argonaut.Argonaut.*
import com.fasterxml.jackson.databind.ObjectMapper
import com.learningobjects.cpxp.component.*
import com.learningobjects.cpxp.component.annotation.{Component, PathVariable}
import com.learningobjects.cpxp.component.query.{ApiFilter, ApiQuery, ApiQueryResults, PredicateOperator}
import com.learningobjects.cpxp.component.util.ComponentUtils
import com.learningobjects.cpxp.component.web.{NoContentResponse, WebResponse}
import com.learningobjects.cpxp.service.Current
import com.learningobjects.cpxp.service.domain.{DomainConstants, DomainDTO}
import com.learningobjects.cpxp.service.exception.ValidationException
import com.learningobjects.cpxp.service.overlord.OverlordWebService
import com.learningobjects.cpxp.service.script.ScriptService
import com.learningobjects.cpxp.startup.StartupTaskService
import de.tomcat.juli.LogMeta
import org.apache.commons.lang3.BooleanUtils
import org.log4s.Logger
import scalaz.std.list.*
import scalaz.std.string.*
import scalaz.syntax.std.boolean.*
import scalaz.syntax.std.option.*
import scaloi.syntax.OptionOps.*

import scala.jdk.CollectionConverters.*

@Component
class ComponentRootApiImpl(val componentInstance: ComponentInstance)(implicit
  componentEnvironment: ComponentEnvironment,
  domain: DomainDTO,
  environment: ComponentEnvironment,
  overlordWebService: OverlordWebService,
  scope: Class[? <: ComponentInterface],
  scriptService: ScriptService,
  startupTaskServiceProvider: () => StartupTaskService,
) extends ComponentRootApi
    with ComponentImplementation:

  import ComponentRootApi.*
  import ComponentRootApiImpl.*

  override def getComponents(query: ApiQuery): ApiQueryResults[ComponentComponent] =
    if query.getOrders.size() > 0 then throw new UnsupportedOperationException("order")
    if query.getPage.isSet then throw new UnsupportedOperationException("page")

    val baseFilters = (scope != null) ?? List(SupportedFilter(scope))

    val filters: List[ComponentFilter] = (
      query.getAllFilters.asScala.map(getComponentFilter).toList ::: baseFilters
    ).sorted

    val (firstFilter, otherFilters) = filters match
      case Nil    => (UnFilter, Nil)
      case h :: t => (h, t)

    val components: Seq[ComponentDescriptor] = firstFilter match
      case UnFilter                     =>
        componentEnvironment.getComponents.asScala.toSeq
      case IdentifierFilter(identifier) =>
        val descriptor = ComponentSupport.getComponentDescriptor(identifier)
        Option(descriptor).map(_ :: Nil).orZero
      case SupportedFilter(interface)   =>
        ComponentSupport.getComponentDescriptors(interface).asScala.toSeq

    val res = components
      .filter(c =>
        (
          (c.getInterfaces.size > 1)
            && otherFilters.forall(_(c))
        )
      )
      .map(asMetaComponent)

    new ApiQueryResults[ComponentComponent](res.asJavaCollection, null, null)
  end getComponents

  override def getComponent(identifier: String): Option[ComponentComponent] =
    val component = ComponentSupport.getComponentDescriptor(identifier)
    Option(component).map(c => asMetaComponent(c))

  /** Apply the specified configuration to the component environment, ensuring that all components in the 'enable' list
    * are enabled, and all components in the 'disable' list are disabled.
    *
    * @return
    *   A {@code Configuration} object, which can be passed back in to undo the changes that were made.
    */
  override def configureComponents(config: Configuration): Configuration =
    val componentCollection = scriptService.getComponentCollection(Current.getDomain, true)
    LogMeta.let("components" -> componentCollection.getEnabledMap.asScala.filter(node => node._2).keys.toList.asJson) {
      logger.info("Enabled components before changes")
    }
    val availability        = componentCollection.getEnabledMap.asScala
    val enable              = config.enable.diff(availability.toList.filter(tuple => tuple._2).map(tuple => tuple._1))
    val disable             = config.disable.diff(availability.toList.filter(tuple => !tuple._2).map(tuple => tuple._1))
    val changes             = Configuration(enable, disable)
    if changes.enable.nonEmpty || changes.disable.nonEmpty then
      val enabled     = config.enable.map(c => c -> Boolean.box(true)).toMap
      val disabled    = config.disable.map(c => c -> Boolean.box(false)).toMap
      val notChanging = availability.keys.toList
        .diff(config.enable ++ config.disable)
        .map(c => c -> availability.getOrElse(c, Boolean.box(false)))
        .toMap
      val enabledMap  = enabled ++ disabled ++ notChanging
      scriptService.setEnabledMap(Current.getDomain, enabledMap.asJava)
      ComponentManager.initComponentEnvironment(componentCollection)
    end if
    LogMeta.let("components" -> componentCollection.getEnabledMap.asScala.filter(node => node._2).keys.toList.asJson) {
      logger.info("Enabled components after changes")
    }
    changes
  end configureComponents

  override def getComponentRings: List[RingDTO] =
    val isOverlord           = getIsOverlord
    val componentCollection  =
      if isOverlord then scriptService.getComponentCollection(domain.id, true) else environment.getCollection
    val componentEnvironment =
      if isOverlord then ComponentManager.initComponentEnvironment(componentCollection) else environment

    val envArchives: Iterable[ComponentArchive] = componentEnvironment.getArchives.asScala
    val ring0                                   = envArchives.toList.filter(archive => archive.getSource.getCollection eq null)
    val ring1                                   =
      envArchives.filter(archive => matches(archive.getSource.getCollection, ComponentCollection.CLUSTER.getIdentifier))
    val ring2                                   = envArchives.filter((archive: ComponentArchive) =>
      matches(archive.getSource.getCollection, componentCollection.getIdentifier)
    )
    val rings                                   = if isOverlord then List(ring0, ring1) else List(ring0, ring1, ring2)
    rings.zipWithIndex.map { case (ring, i) =>
      marshalRingNode("Ring " + i, ring, componentCollection)
    }
  end getComponentRings

  override def toggleComponent(toggleDTO: ToggleDTO): WebResponse =
    val identifier: String = toggleDTO.identifier
    val enabled: Boolean   = toggleDTO.enabled
    val action             = if enabled then "Enabled " else "Disabled "
    setAvailability(identifier, Some(enabled))
    scriptService.initComponentEnvironment()
    overlordWebService.asOverlord(() => startupTaskServiceProvider().startupDomain(domain.id))
    logger.info(action + "component with identifier: " + identifier)
    NoContentResponse

  override def deleteArchive(@PathVariable("identifier") identifier: String): WebResponse =
    scriptService.removeComponentArchive(identifier, domain.id)
    setAvailability(identifier, None)
    scriptService.initComponentEnvironment()
    NoContentResponse

  private def setAvailability(identifier: String, available: Option[Boolean]): Unit =
    val componentCollection =
      scriptService.getComponentCollection(domain.id, true)
    val enabledMap          = componentCollection.getEnabledMap

    available.fold(enabledMap.remove(identifier))(enabledMap.put(identifier, _))

    scriptService.setEnabledMap(domain.id, enabledMap)

  override def installArchive(installDTO: InstallDTO): WebResponse =
    val scriptFolder = scriptService.getDomainScriptFolder(domain.id).getId
    val car          = scriptService.installComponentArchive(
      scriptFolder,
      installDTO.uploadInfo.getFile,
      installDTO.uploadInfo.getFileName
    )
    if getIsOverlord && installDTO.uninstall then scriptService.clusterRemoveComponentArchive(car.getIdentifier)
    scriptService.initComponentEnvironment
    NoContentResponse
  end installArchive

  override def setConfig(configDTO: ConfigDTO): WebResponse =
    val currConfig    = scriptService.getConfigurationMap(domain.id).getOrDefault(configDTO.id, "empty")
    logger.info("Component " + configDTO.id + "'s configuration before " + currConfig)
    scriptService.setComponentConfiguration(domain.id, configDTO.id, compactJson(configDTO.config))
    val updatedConfig = scriptService.getConfigurationMap(domain.id).getOrDefault(configDTO.id, "empty")
    logger.info("Component " + configDTO.id + "'s configuration after " + updatedConfig)
    NoContentResponse

  private def getIsOverlord = domain.`type` == DomainConstants.DOMAIN_TYPE_OVERLORD

  private def matches(collection: ComponentCollection, identifier: String)                                        =
    (collection != null) && identifier == collection.getIdentifier

  private def marshalComponentNode(
    descriptor: ComponentDescriptor,
    componentCollection: ComponentCollection
  ): ComponentDTO =
    val component = descriptor.getInstance(environment, null, null)
    val disabled  = !BooleanUtils.toBooleanDefaultIfNull(
      componentCollection
        .getComponentEnabled(descriptor.getIdentifier),
      descriptor.getComponentAnnotation.enabled
    )
    val label     = component.getName + " (" + component.getIdentifier + ")"
    ComponentDTO(
      id = component.getIdentifier,
      disabled = disabled,
      label = label,
      description = OptionNZ(component.getDescription),
      version = descriptor.getComponentAnnotation.version,
      configuration = OptionNZ(scriptService.getConfigurationMap(domain.id).get(component.getIdentifier)),
    )
  end marshalComponentNode
  private def marshalArchiveNode(archive: ComponentArchive, componentCollection: ComponentCollection): ArchiveDTO =
    val ann       = archive.getArchiveAnnotation
    val on        = componentCollection.getEnabledMap.get(archive.getIdentifier)
    val disabled  = Option(on).map(!_).getOrElse(!archive.getArchiveAnnotation.available)
    val removable =
      matches(archive.getSource.getCollection, componentCollection.getIdentifier) ||
        (getIsOverlord && (archive.getSource.getCollection eq ComponentCollection.CLUSTER))

    val packages = archive.getComponents.asScala
      .groupBy(cd =>
        val id          = cd.getIdentifier
        val idxOfFirst  = id.indexOf('.')
        val idxOfSecond = Math.max(idxOfFirst, id.indexOf('.', 1 + idxOfFirst))
        val idxOfThird  = Math.max(idxOfSecond, id.indexOf('.', 1 + idxOfSecond))
        id.substring(0, if idxOfThird < 0 then id.length else idxOfThird)
      )

    val children: List[PackageDTO] = packages.toList
      .map { case (name, components) =>
        val grandchildren: List[ComponentDTO] = components
          .map(desc => marshalComponentNode(desc, componentCollection))
          .sortBy(_.label.toLowerCase)
          .toList
        PackageDTO(
          label = name,
          children = grandchildren,
        )
      }
      .sortBy(_.label.toLowerCase)

    ArchiveDTO(
      id = archive.getIdentifier,
      label = OptionNZ(ann.name).getOrElse(archive.getIdentifier),
      version = ann.version,
      branch = ann.branch,
      revision = ann.revision,
      buildNumber = ann.buildNumber,
      buildDate = ann.buildDate,
      disabled = disabled,
      removable = removable,
      children = children,
    )
  end marshalArchiveNode

  private def marshalRingNode(
    name: String,
    archives: Iterable[ComponentArchive],
    componentCollection: ComponentCollection,
  ): RingDTO =
    val children: List[ArchiveDTO] = archives
      .map(archive => marshalArchiveNode(archive, componentCollection))
      .toList
      .sortBy(_.label.toLowerCase)
    RingDTO(
      label = name,
      children = children,
    )
  end marshalRingNode

  private val mapper = new ObjectMapper

  private def compactJson(str: String): String =
    if (str eq null) || str.isEmpty then return "{}"
    val m = mapper.readValue(str, classOf[java.util.Map[Object, Object]])
    ComponentUtils.toJson(m)
end ComponentRootApiImpl

object ComponentRootApiImpl:

  private val logger: Logger = org.log4s.getLogger

  sealed abstract class ComponentFilter(matches: ComponentDescriptor => Boolean)
      extends (ComponentDescriptor => Boolean):
    final def apply(cd: ComponentDescriptor) = matches(cd)

  final case class IdentifierFilter(identifier: String) extends ComponentFilter(_.getIdentifier == identifier)

  final case class SupportedFilter(interface: Class[? <: ComponentInterface])
      extends ComponentFilter(_.isSupported(interface))

  case object UnFilter extends ComponentFilter(_ => true)

  implicit val componentFilterOrder: Ordering[ComponentFilter] =
    Ordering.by {
      // sort identifier filters first (more precise)
      case IdentifierFilter(identifier) => (2, identifier)
      case SupportedFilter(interface)   => (1, interface.getName)
      case UnFilter                     => (0, "")
    }

  def getComponentFilter(filter: ApiFilter): ComponentFilter =
    if filter.matches(ComponentComponent.PROPERTY_INTERFACE, PredicateOperator.EQUALS) then
      try
        SupportedFilter(
          ComponentSupport
            .loadClass(filter.getValue)
            .asSubclass(classOf[ComponentInterface])
        )
      catch
        case _: Exception =>
          throw new ValidationException("filter", filter.toString, "Invalid filter interface")
    else if filter.matches(ComponentComponent.PROPERTY_IDENTIFIER, PredicateOperator.EQUALS) then
      IdentifierFilter(filter.getValue)
    else throw new ValidationException("filter", filter.toString, "Unsupported filter")

  private def asMetaComponent(component: ComponentDescriptor) =
    val meta = ComponentSupport.getInstance(classOf[ComponentComponent], classOf[MetaComponent].getName, null)
    meta.asComponent(classOf[MetaComponent]).init(component)
    meta
end ComponentRootApiImpl
