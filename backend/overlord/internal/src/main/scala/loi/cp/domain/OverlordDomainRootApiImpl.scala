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

package loi.cp.domain

import argonaut.Argonaut.*
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.learningobjects.cpxp.ServiceMeta
import com.learningobjects.cpxp.async.async.{AsyncLogHandler, AsyncOperationActor}
import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.query.*
import com.learningobjects.cpxp.component.web.{NoResponse, WebResponse}
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance, ComponentService}
import com.learningobjects.cpxp.controller.domain.DomainAppearance
import com.learningobjects.cpxp.filter.CurrentFilter
import com.learningobjects.cpxp.scala.cpxp.Component.*
import com.learningobjects.cpxp.scala.cpxp.Facade.*
import com.learningobjects.cpxp.scala.cpxp.Item.*
import com.learningobjects.cpxp.scala.json.JacksonCodecs.*
import com.learningobjects.cpxp.service.Current
import com.learningobjects.cpxp.service.attachment.AttachmentService
import com.learningobjects.cpxp.service.data.{DataService, DataSupport, DataTypes}
import com.learningobjects.cpxp.service.domain.*
import com.learningobjects.cpxp.service.domain.DomainConstants.*
import com.learningobjects.cpxp.service.dump.DumpService
import com.learningobjects.cpxp.service.email.EmailService
import com.learningobjects.cpxp.service.enrollment.EnrollmentWebService
import com.learningobjects.cpxp.service.exception.{
  BusinessRuleViolationException,
  ResourceNotFoundException,
  ValidationException,
  ValidationInfo
}
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.item.ItemService
import com.learningobjects.cpxp.service.overlord.OverlordWebService
import com.learningobjects.cpxp.service.query.{Comparison, Projection, QueryService, Function as QBFunction}
import com.learningobjects.cpxp.service.script.ScriptService
import com.learningobjects.cpxp.service.session.SessionService
import com.learningobjects.cpxp.service.user.{UserDTO, UserWebService}
import com.learningobjects.cpxp.startup.StartupTaskService
import com.learningobjects.cpxp.util.*
import com.learningobjects.cpxp.util.logging.{LogCapture, ServletThreadLogWriter}
import com.typesafe.config.Config
import de.tomcat.juli.LogMeta
import jakarta.servlet.http.{HttpServletRequest, HttpServletResponse}
import loi.authoring.startup.AuthoringStartupService
import loi.cp.analytics.redshift.RedshiftSchemaService
import loi.cp.bootstrap.BootstrapInstance
import loi.cp.cors.AmazonS3CorsService
import loi.cp.devops.DevOps
import loi.cp.presence.PresenceActor.DeliverMessage
import loi.cp.presence.SessionsActor.DomainMessage
import loi.cp.presence.{ClusterBroadcaster, MaintenanceEvent}
import loi.cp.role.RoleService
import loi.cp.tx.DEIEIO
import loi.cp.user.{OldUserService, UserComponent}
import loi.db.Redshift
import loi.doobie.log.*
import loi.nashorn.Nashorn
import org.apache.commons.io.IOUtils
import org.apache.pekko.actor.ActorSystem
import scalaz.*
import scalaz.std.list.*
import scalaz.syntax.applicative.*
import scalaz.syntax.either.*
import scalaz.syntax.std.boolean.*
import scalaz.syntax.validation.*
import scaloi.syntax.AnyOps.*

import java.lang.{Boolean as JBoolean, Long as JLong}
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.util.{Locale, Properties}
import scala.annotation.tailrec
import scala.compat.java8.OptionConverters.*
import scala.concurrent.duration.*
import scala.jdk.CollectionConverters.*
import scala.util.control.{NoStackTrace, NonFatal}

@Component
class OverlordDomainRootApiImpl(val componentInstance: ComponentInstance, domainDTO: DomainDTO, user: UserDTO)(implicit
  actorSystem: ActorSystem,
  appearance: DomainAppearance,
  as: AttachmentService,
  ass: AuthoringStartupService,
  cs: ComponentService,
  config: Config,
  dataSvc: DataService,
  ds: DumpService,
  dws: DomainWebService,
  emailSvc: EmailService,
  ews: EnrollmentWebService,
  fs: FacadeService,
  is: ItemService,
  mapper: ObjectMapper,
  ows: OverlordWebService,
  qs: QueryService,
  roleSvc: RoleService,
  redshiftSchemaService: RedshiftSchemaService,
  ss: ScriptService,
  sessionSvc: SessionService,
  sm: ServiceMeta,
  sts: StartupTaskService,
  userSvc: OldUserService,
  uws: UserWebService,
) extends OverlordDomainRootApi
    with ComponentImplementation:

  private implicit final val logger: org.log4s.Logger = org.log4s.getLogger

  override def get(q: ApiQuery): ApiQueryResults[OverlordDomain] =
    ApiQuerySupport.query(queryDomains, q, classOf[OverlordDomain])

  override def get(id: Long): Option[OverlordDomain] =
    get(ApiQuery.byId(id)).asOption

  override def create(d: OverlordDomain): OverlordDomain =
    logger.info(s"Create domain ${d.getDomainId}, ${d.getName}, ${d.getPrimaryHostName}")
    checkDuplicate(d.getDomainId, d.getHostNames, None)
    val domain = dws.addDomain()
    domain.setDomainId(d.getDomainId)
    domain.setName(d.getName)
    domain.setShortName(d.getShortName)
    domain.setPrimaryHostName(d.getPrimaryHostName)
    domain.setHostNames(d.getHostNames.asJava)
    domain.setLocale(Locale.forLanguageTag(d.getLocale).toString)
    domain.setTimeZone(d.getTimeZone)
    domain.setSecurityLevel(d.getSecurityLevel)
    domain.setLoginRequired(false)
    domain.setLicenseRequired(false)
    domain.setStartDate(DataSupport.MIN_TIME)
    domain.setEndDate(DataSupport.MAX_TIME)
    domain.setSessionTimeout(DateUtils.Unit.hour.getValue(3))
    domain.setRememberTimeout(DateUtils.Unit.week.getValue(1))

    /*
    dto.setUserLimit(Long users)
    dto.setGroupLimit(Long groups)
    dto.setMembershipLimit(Long membershipLimit)
    dto.setEnrollmentLimit(Long enrollments)
    dto.setMaximumFileSize(Long maximumFileSize)
    dto.setMessage(String message)
    dto.setUserUrlFormat(String userUrlFormat)
    dto.setGroupUrlFormat(String groupUrlFormat)
    dto.setGuestPolicy(GuestPolicy guestPolicy)
     */

    dws.invalidateHostnameCache()

    domain.component[OverlordDomain]
  end create

  /** Validate a domain creation request. Checks for duplicate identifiers, valid hostname etc.
    *
    * @param request
    *   the creation request
    */
  override def validate(request: DomainProperties): Unit =
    validateRequest(request).swap foreach { errors =>
      throw new ValidationException(errors.list.toList.asJava)
    }

  /** Provision a PLE domain.
    *
    * @param request
    *   the provision request
    * @return
    *   provisioning results
    */
  override def provision(request: ProvisionRequest): ProvisionResponse =
    logger info s"Provision domain: $request"
    LogCapture.captureLogs(new AsyncLogHandler(classOf[OverlordDomainRootApiImpl])) {
      AsyncOperationActor.tellWarning("create")
      val id = createDomain(request.domain)
      ManagedUtils.commit()

      AsyncOperationActor.tellWarning("init")
      initializeDomain(id)
      EntityContext.flushClearAndCommit()

      AsyncOperationActor.tellWarning("profile")
      bootstrapDomain(id, PCP, mapper.createObjectNode())
      ManagedUtils.commit()

      AsyncOperationActor.tellWarning("account")
      val password = createAdminAccount(request.account)
      ManagedUtils.commit()

      AsyncOperationActor.tellWarning("appearance")
      applyAppearance(request.appearance)
      dws.getDomain(id).setState(DomainState.Normal)
      ManagedUtils.commit()

      val dns = if DevOps.isConfigured then
        AsyncOperationActor.tellWarning("dns")
        requestDns(request.domain.hostName).isRight
      else false

      AsyncOperationActor.tellWarning("done")

      emailDomainCreator(request, password, dns)

      ProvisionResponse(request.domain, request.account, request.appearance, password, dns)
    }
  end provision

  /** Create a domain. Doesn't perform any initialization.
    */
  private def createDomain(domain: DomainProperties): Long =
    logger.info(s"Create domain ${domain.domainId}, ${domain.name}, ${domain.hostName}")
    checkDuplicate(domain.domainId, Seq(domain.hostName), None)
    val d = dws.addDomain()
    d.setDomainId(domain.domainId)
    d.setName(domain.name)
    d.setShortName(domain.shortName)
    d.setPrimaryHostName(domain.hostName)
    d.setHostNames(List(domain.hostName).asJava)
    d.setLocale("en_US")
    d.setTimeZone("US/Eastern")
    d.setSecurityLevel(SecurityLevel.SecureAlways)
    d.setLoginRequired(false)
    d.setLicenseRequired(false)
    d.setStartDate(DataSupport.MIN_TIME)
    d.setEndDate(DataSupport.MAX_TIME)
    d.setState(DomainState.Init)
    d.setSessionTimeout(DateUtils.Unit.hour.getValue(3))
    d.setRememberTimeout(DateUtils.Unit.week.getValue(1))
    dws.invalidateHostnameCache()
    d.getId
  end createDomain

  /** Create an admin account in the current domain (the target domain) and return the account password.
    */
  private def createAdminAccount(admin: AccountProperties): String =
    logger.info(s"Create account $admin")
    val user      = userSvc.createUser(admin.userName)
    user.setGivenName(admin.givenName)
    user.setMiddleName(admin.middleName)
    user.setFamilyName(admin.familyName)
    user.setEmailAddress(admin.emailAddress)
    user.updateFullName()
    val adminRole = roleSvc.getRoleByRoleId("administrator").getId
    ews.setSingleEnrollment(Current.getDomain, adminRole, user.getId, "Provisioning")
    val password  = NumberUtils.toBase48Encoding(new BigInteger(50, NumberUtils.getSecureRandom))
    user.component[UserComponent].setPassword(password)
    password
  end createAdminAccount

  /** Apply appearance settings to the current domain (the target domain).
    */
  private def applyAppearance(properties: AppearanceProperties): Unit =
    logger.info(s"Apply appearance: $properties")
    // See DomainSettingsComponent
    appearance
      .setColors(properties.primaryColor, properties.secondaryColor, properties.accentColor)
    val domain = domainDTO.facade[DomainFacade]
    properties.favicon foreach { favicon =>
      logger.info("Apply favicon")
      Option(domain.getFavicon) foreach {
        _.delete()
      }
      dws.setImage(DATA_TYPE_FAVICON, favicon.getFileName, favicon.getWidth, favicon.getHeight, favicon.getFile)
    }
    properties.logo foreach { logo =>
      logger.info("Apply logo")
      Option(domain.getLogo) foreach {
        _.delete()
      }
      dws.setImage(DataTypes.DATA_TYPE_LOGO, logo.getFileName, logo.getWidth, logo.getHeight, logo.getFile)
    }
  end applyAppearance

  /** Send email to the overlord user, not the new admin, about the domain.
    */
  private def emailDomainCreator(request: ProvisionRequest, password: String, dns: Boolean): Unit =
    Option(user.emailAddress).filter(_.nonEmpty) foreach { email =>
      val body =
        s"""Domain Name: ${request.domain.name}
            |Short Name: ${request.domain.shortName}
            |URL: https://${request.domain.hostName}/
            |Administrator: ${request.account.givenName} ${request.account.familyName}
            |Email Address/Username: ${request.account.emailAddress}
            |Password: ${password}
            |DNS: ${if dns then "Success" else "Failure"}
            |""".stripMargin
      try
        val fullName = s"${user.givenName} ${user.familyName}"
        emailSvc.sendTextEmail(
          "noreply@difference-engine.com",
          "Overlörde",
          email,
          fullName,
          s"Provisioned: ${request.domain.name}",
          body,
          false
        )
      catch case e: Exception => logger.warn(e)("Error sending email")
      end try
    }

  // should these be domain routes or domainroot routes?

  override def initialize(id: Long): Unit =
    id.facade_?[DomainFacade] match // domain has no type yet so is not findable by get
      case None =>
        throw new ResourceNotFoundException(s"Domain $id")

      case Some(d) if d.getState != null =>
        throw new BusinessRuleViolationException("Invalid domain state")

      case Some(d) =>
        LogCapture.captureLogs(new AsyncLogHandler(classOf[OverlordDomainRootApiImpl])) {
          initializeDomain(Left(d))
          d.setState(DomainState.Normal) // TODO: this should happen after successful bootstrap
        }

  private def initializeDomain(id: Long): Unit = initializeDomain(Right(id))

  private def initializeDomain(d: Either[DomainFacade, Long]): Unit =
    val id         = d.fold(f => f.getId.longValue(), id => id)
    val domain     = dws.getDomainDTO(id)
    Current.setDomainDTO(domain)
    val locale     = ClassUtils.parseLocale(domain.locale)
    val domainDump = ClassUtils.getLocalizedZipResourceAsTempFile("/zips/dump-domain.zip", locale, "Dump", null)
    try
      ds.restoreInto(id, "", domainDump.getFile)
    finally
      domainDump.deref()
    AmazonS3CorsService.updateS3CorsConfiguration(d.swap.toOption)
  end initializeDomain

  override def bootstrap(id: Long, profile: String, config: JsonNode): Unit = id.facade_?[DomainFacade] match
    case None =>
      throw new ResourceNotFoundException(s"Domain $id")

    case _ =>
      LogCapture.captureLogs(new AsyncLogHandler(classOf[OverlordDomainRootApiImpl])):
        try bootstrapDomain(id, profile, config)
        catch
          case NonFatal(e) =>
            logger.warn(e)("Bootstrap domain error")
            throw e

  private def bootstrapDomain(id: Long, profile: String, config: JsonNode): Unit =
    // deliberate minimal context setup to avoid component environment load
    Current.setDomainDTO(dws.getDomainDTO(id))
    Current.setUserDTO(uws.getUserDTO(uws.getRootUser))
    LogMeta.let(LogMeta.Domain := id, LogMeta.User := Current.getUser) {
      SupportedProfiles.find(_.profile.identifier == profile).fold(throw new Exception(s"Unknown profile: $profile")) {
        profile =>
          executeBootstrap(id, parseBootstrap(profile.profile.name, profile.script, config))
      }
    }
  end bootstrapDomain

  private def parseBootstrap(name: String, script: BootstrapScript, config: JsonNode): DomainBootstrap = script match
    case NoBootstrap => DomainBootstrap(name, None, None, None, List.empty)

    case SimpleBootstrap(file) =>
      val json = evalSimple(file, config)
      mapper.readValue(json, classOf[DomainBootstrap])

    case DestrapBootstrap(files) =>
      transformDestrap(DomainBootstrap(name, None, None, None, List.empty), evalDestrap(files, config))

    case SimpleAndDestrapBootstrap(file, additionalDestraps) =>
      val json               = evalSimple(file, config)
      val domainBootstrap    = mapper.readValue(json, classOf[DomainBootstrap])
      val destraps           = evalDestrap(additionalDestraps, config)
      val newDomainBootstrap = transformDestrap(domainBootstrap, destraps)
      newDomainBootstrap.copy(bootstrap = domainBootstrap.bootstrap ++ destraps)

  /* This function transforms a list of destrap commands into a domain bootstrap. */
  private def transformDestrap(initialBootstrap: DomainBootstrap, destraps: Seq[Destrap]): DomainBootstrap =
    destraps.foldRight(initialBootstrap) { (destrap, bootstrap) =>
      destrap match // you enter a strange place full of magic and constants
        case Destrap("bootstrap.configure", Some(config), _)      =>
          bootstrap.copy(s3 = Some(S3Config(config.get("s3Identity").textValue, config.get("s3Credential").textValue)))
        case Destrap("core.domain.create", Some(config), _)       =>
          Option(config.get("favicon")).fold(bootstrap) { favicon =>
            val settings = mapper.getNodeFactory.objectNode.set[ObjectNode]("icon", favicon)
            bootstrap.copy(bootstrap = bootstrap.bootstrap :+ Destrap("core.domain.settings", Some(settings), None))
          }
        case Destrap("core.domain.delete", _, _)                  => bootstrap
        case Destrap("core.car.enable", Some(config), _)          =>
          bootstrap.copy(
            archives = Some(bootstrap.archives.getOrElse(Map.empty) ++ jsonArrayToArchiveConfig(config, true))
          )
        case Destrap("core.car.disable", Some(config), _)         =>
          bootstrap.copy(
            archives = Some(bootstrap.archives.getOrElse(Map.empty) ++ jsonArrayToArchiveConfig(config, false))
          )
        case Destrap(
              "core.car.install",
              Some(config),
              _
            ) => // bootstrap expects an archive id, but not destrap so just make one up
          bootstrap.copy(
            archives = Some(
              bootstrap.archives.getOrElse(Map.empty) +
                (s"synthetic-${bootstrap.archives.fold(0)(_.size)}" -> ArchiveBootstrap(true, Some(config.textValue)))
            )
          )
        case Destrap("core.component.toggle", Some(config), _)    =>
          configureComponent(bootstrap, config.get("identifier").textValue) {
            _.copy(enabled = config.get("enabled").booleanValue)
          }
        case Destrap("core.component.configure", Some(config), _) =>
          configureComponent(bootstrap, config.get("identifier").textValue) {
            _.copy(configuration = Some(config.get("configuration")))
          }
        case Destrap("core.component.init", _, _)                 => bootstrap
        case d                                                    =>
          bootstrap.copy(bootstrap = d :: bootstrap.bootstrap)
    }

  private def configureComponent(bootstrap: DomainBootstrap, identifier: String)(
    f: ComponentBootstrap => ComponentBootstrap
  ): DomainBootstrap =
    val components = bootstrap.components.getOrElse(Map.empty)
    bootstrap.copy(
      components =
        Some(components + (identifier -> f(components.getOrElse(identifier, ComponentBootstrap(true, None)))))
    )

  private def jsonArrayToArchiveConfig(node: JsonNode, enabled: Boolean): Iterator[(String, ArchiveBootstrap)] =
    node.elements.asScala.map { archive =>
      s"com.learningobjects.${archive.textValue}" -> ArchiveBootstrap(enabled, None)
    }

  private def evalSimple(file: String, config: JsonNode): String =
    val source = loadDestrapResource(file)
    if file.endsWith(".json") then source else evalJavascript(source, config)

  private val ListType = mapper.getTypeFactory.constructCollectionLikeType(classOf[Seq[?]], classOf[Destrap])

  private def evalDestrap(files: Seq[String], config: JsonNode): Seq[Destrap] =
    if files.isEmpty then Seq.empty
    else
      val sources = files map { file =>
        loadDestrapResource(file)
      }
      val json    = evalJavascript(sources.mkString, config)
      mapper.readValue[Seq[Destrap]](json, ListType)

  private def evalJavascript(source: String, config: JsonNode): String =
    val cfjson  = mapper.writeValueAsString(config)
    val nashorn = new Nashorn
    nashorn.bindFunction("getenv"): args =>
      configOrFail(args.head.asInstanceOf[String])
    nashorn.eval(s"var config = $cfjson;\n$source") match
      case str: String => str
      case other       => mapper.writeValueAsString(other)

  private def loadDestrapResource(file: String): String =
    Option(getClass.getResource(s"bootstrap/$file")).fold(throw new Exception(s"Unknown file: $file")): url =>
      IOUtils.toString(url, StandardCharsets.UTF_8)

  private def configOrFail(key: String): String =
    if !config.hasPath(key) then throw MissingConfigError(key)
    else config.getString(key)

  private def executeBootstrap(id: Long, bootstrap: DomainBootstrap): Unit =
    logger.info(s"Bootstrap ${bootstrap.name}")
    bootstrap.s3 foreach { config =>
      BootstrapInstance.initS3(config.identity, config.credential)
    }
    bootstrap.archives foreach { archives =>
      archives.values.flatMap(_.car) foreach { url =>
        logger.info("Installing archive: " + url)
        val upload = BootstrapInstance.download(url)
        val folder = ss.getDomainScriptFolder(id).getId
        ss.installComponentArchive(folder, upload.getFile, upload.getFileName)
      }
    }
    val availabilities = bootstrap.archives.fold(Map.empty[String, JBoolean]) { archives =>
      archives.view.mapValues(a => JBoolean.valueOf(a.enabled)).toMap
    } ++ bootstrap.components.fold(Map.empty[String, JBoolean]) { components =>
      components.view.mapValues(c => JBoolean.valueOf(c.enabled)).toMap
    }
    LogMeta.let("availability" -> availabilities.asJson)(logger.info("Availability map"))
    val configurations = bootstrap.components.fold(Map.empty[String, JsonNode]) { c =>
      c.view.mapValues(_.configuration).toMap collect { case (k, Some(v)) =>
        k -> v
      }
    }
    LogMeta.let("components" -> configurations.asJson)(logger.info("Configuration map"))
    ss.setConfigurationMap(id, configurations.view.mapValues(_.toString).toMap.asJava)
    ss.setEnabledMap(id, availabilities.asJava)
    ManagedUtils.commit()
    sts.startupDomain(id) leftMap { e =>
      throw new Exception("Startup task error", e)
    }
    dws.setupContext(id)
    destrap(bootstrap.bootstrap)
  end executeBootstrap

  private def destrap(d: Seq[Destrap], context: JLong = null): Unit = d foreach { case Destrap(phase, config, setup) =>
    logger.info(s"Bootstrap phase $phase")
    Option(BootstrapInstance.lookup(phase)).fold(throw new Exception(s"Unknown bootstrap phase: $phase")) { function =>
      if config.exists(_.isArray) && !function.hasCollectionParameter then
        config.get.asScala foreach { cf =>
          function.invoke(context, cf)
        }
        ManagedUtils.commit()
      else
        val subcontext = function.invoke(context, config.getOrElse(mapper.nullNode))
        ManagedUtils.commit()
        setup foreach { destrap(_, subcontext) }
    }
  }

  override def requestDns(id: Long): String \/ String = id.facade_?[DomainFacade] match
    case None    => throw new ResourceNotFoundException(s"Domain $id")
    case Some(d) => requestDns(d.getPrimaryHostName)

  private def requestDns(hostName: String): String \/ String =
    DevOps.requestCName(hostName) flatMap { changeId =>
      AsyncOperationActor.tellProgress(0, 0, s"Request submitted: $hostName ~> $changeId")
      val deadline                        = 3.minutes.fromNow
      @tailrec def poll: String \/ String =
        if deadline.isOverdue() then s"Aborted after poll deadline expired".left[String]
        else
          Thread.sleep(12.seconds.toMillis)
          logger.info(s"Polling for DNS: $changeId")
          DevOps.pollCName(changeId) match
            case \/-(false)   =>
              logger.info("DNS request still in progress")
              AsyncOperationActor.tellProgress(0, 0, "Change pending")
              poll
            case \/-(true)    =>
              logger.info("DNS request complete")
              hostName.right[String]
            case -\/(failure) =>
              logger.info(s"DNS request failed: $failure")
              failure.left[String]
          end match
      poll
    }

  override def updateDomain(id: Long, d: OverlordDomain): OverlordDomain =
    get(id).fold(throw new ResourceNotFoundException(s"Domain $id")) { _ =>
      checkDuplicate(d.getDomainId, d.getHostNames, Some(id))
      val domain = dws.getDomain(id)
      domain.setDomainId(d.getDomainId)
      domain.setName(d.getName)
      domain.setShortName(d.getShortName)
      domain.setPrimaryHostName(d.getPrimaryHostName)
      domain.setHostNames(d.getHostNames.asJava)
      domain.setLocale(Locale.forLanguageTag(d.getLocale).toString)
      domain.setTimeZone(d.getTimeZone)
      domain.setSecurityLevel(d.getSecurityLevel)
      dws.invalidateHostnameCache()

      AmazonS3CorsService.updateS3CorsConfiguration()

      id.component[OverlordDomain]
    }

  // non-locking checking
  private def checkDuplicate(domainId: String, hostNames: Iterable[String], existing: Option[Long]): Unit =
    Option(dws.getDomainById(domainId)).map(_.longValue).filterNot(existing.contains) foreach { d2 =>
      throw new BusinessRuleViolationException(s"Duplicate domain id ${domainId}")
    }
    hostNames foreach { hn =>
      Option(dws.getDomainByExactHost(hn)).map(_.getId.longValue).filterNot(existing.contains) foreach { d2 =>
        throw new BusinessRuleViolationException(s"Duplicate host name $hn")
      }
    }

  override def transitionDomain(id: Long, s: StateChange): Unit =
    get(id).fold(throw new ResourceNotFoundException(s"Domain $id")) { _ =>
      dws.setState(id, s.state, s.message.orNull)
    }

  // TODO: destroy the component environment?
  override def deleteDomain(id: Long, hard: JBoolean): Unit =
    get(id).fold(throw new ResourceNotFoundException(s"Domain $id")) { _ =>
      if Option(hard).exists(_.booleanValue) then dws.removeDomain(id)
      else dws.setState(id, DomainState.Deleted, null)
      // remove hostnames from data that later domains might reuse it
      dataSvc.clear(id.item, DataTypes.DATA_TYPE_HOST_NAME)
      // and evict the cache that later requests might ignore it
      dws.invalidateHostnameCache()
    }

  override def manageDomain(id: Long, request: HttpServletRequest, response: HttpServletResponse): String =
    get(id).fold(throw new ResourceNotFoundException(s"Domain $id")) { _ =>
      val currentUser       = Current.getUser
      val currentDomain     = Current.getDomain
      val targetDomain      = dws.getDomainDTO(id)
      val sudo              = new OverlordDomainSudo
      val login: sudo.Login = (user: UserComponent) =>
        IO {
          val userDTO                = uws.getUserDTO(user.getId)
          Current.setUserDTO(userDTO)
          val properties: Properties = new Properties
          properties.setProperty(SessionUtils.SESSION_ATTRIBUTE_SUDOER, currentUser.toString)
          CurrentFilter.login(request, response, userDTO, false, properties)
          ()
        }

      val lowerRoles = Map(
        "overlord"  -> EnrollmentWebService.HOSTING_ADMIN_ROLE_ID,
        "underlord" -> EnrollmentWebService.HOSTING_STAFF_ROLE_ID,
        "support"   -> EnrollmentWebService.HOSTING_SUPPORT_ROLE_ID
      )

      val persistNewUser: sudo.Persist = newUser =>
        for
          parent    <- sudo.overlordUserFolder(targetDomain)
          roles     <- IO(ews.getActiveUserRoles(currentUser, currentDomain).asScala)
          userRoles <- IO(roles.map(_.getRoleId).toList)
          peonRoles <- IO {
                         userRoles
                           .flatMap(lowerRoles.get)
                           .flatMap(role => roleSvc.findRoleByRoleId(targetDomain, role).asScala)
                           .map(_.getId)
                       }
          // Add an Hosting role if it doesn't exists.
          _         <- IO {
                         ews.setEnrollment(targetDomain.id, peonRoles.asJava, newUser.getId)
                       }
        yield parent

      val runSudo = for
        _   <- sudo.infiltrate(sudo.replicate, login, persistNewUser)(currentUser.component[UserComponent], targetDomain)
        url <- IO(ows.getAdministrationUrl(id))
      yield url

      DEIEIO.tx(runSudo)(using targetDomain, dws).unsafeRunSync()
    }

  override def profiles: OverlordProfiles =
    OverlordProfiles(DevOps.isConfigured, SupportedProfiles map { _.profile })

  override def redshiftSchemaNames: List[String] =
    Redshift.isConfigured ?? {
      val xa = Redshift.buildTransactor(useBusUser = false)
      redshiftSchemaService.queryAllSchemaNames(xa)
    }

  override def maintenance(m: MaintenanceMode): MaintenanceMode =
    if m.enabled then
      ClusterBroadcaster.broadcast(
        DomainMessage(None, DeliverMessage(MaintenanceEvent("System entering maintenance mode immediately.", m.end)))
      )
    // entering maintenance mode => update normal and maintenance domains, leaving => just maintenance domains
    val states = m.enabled.fold(Seq(DomainState.Maintenance, DomainState.Normal), Seq(DomainState.Maintenance))
    queryDomains
      .addCondition(DATA_TYPE_DOMAIN_STATE, Comparison.in, states)
      .setProjection(Projection.ID)
      .getValues[Long] foreach { id =>
      val state = m.enabled.fold(DomainState.Maintenance, DomainState.Normal)
      dws.setState(id, state, m.end.map(_.toInstant.toString).orNull)
      if m.enabled then sessionSvc.closeDomainSessions(id)
    }
    m
  end maintenance

  override def itDomain(did: String, response: HttpServletResponse): WebResponse =
    HttpUtils.setExpired(response)
    response.setContentType(s"${MimeUtils.MIME_TYPE_TEXT_PLAIN}${MimeUtils.CHARSET_SUFFIX_UTF_8}")
    response.setStatus(HttpServletResponse.SC_OK)
    val writer = response.getWriter
    LogCapture.captureLogs(new ServletThreadLogWriter(response, writer, classOf[OverlordDomainRootApiImpl])) {
      Option(dws.getDomainById(did)).map(dws.getDomain) match
        case Some(d) if d.getState == DomainState.Normal =>
          logger info s"Domain $did exists"
          writer `println` "OK"
        case Some(d)                                     =>
          logger info s"Domain $did is in invalid state: ${d.getState}}"
          writer `println` "Fail"
        case None                                        =>
          try
            logger info s"Creating domain $did"
            val name   = StringUtils.toSeparateWords(did.stripPrefix("it-").capitalize)
            val id     = createDomain(DomainProperties(did, s"Integration Test - $name", s"IT $name", s"$did.it"))
            ManagedUtils.commit()
            initializeDomain(id)
            ManagedUtils.commit()
            val config = mapper.createObjectNode() <| { o =>
              o.put("testData", true)
              o.put("configureForIntegrationTests", true)
            }
            bootstrapDomain(id, PCP, config)
            ManagedUtils.commit()
            ass.startup()
            ManagedUtils.commit()
            dws.getDomain(id).setState(DomainState.Normal)
            ManagedUtils.commit()
            writer.println("OK")
          catch
            case NonFatal(e) =>
              logger.warn(e)(s"Could not create integration test domain $did")
              writer.println("Fail")
    }
    writer.close()
    NoResponse
  end itDomain

  private def queryDomains =
    qs.queryAllDomains(ITEM_TYPE_DOMAIN)
      .addCondition(DataTypes.DATA_TYPE_TYPE, Comparison.eq, DOMAIN_TYPE_DUMP)
      .addCondition(DATA_TYPE_DOMAIN_STATE, Comparison.ne, DomainState.Deleted)

  final val PCP = "PCP"

  // Listing these all here is .. offensive. Defining components is .. offensive. Should I
  // just scan the classpath?
  val SupportedProfiles = List(
    OverlordProfileConfig(OverlordProfile("Default", "Default", Nil), NoBootstrap),
    OverlordProfileConfig(
      OverlordProfile(
        PCP,
        "Basic Domain",
        List(
          BooleanConfig("testData", "Test Data", default = false),
          BooleanConfig("configureForIntegrationTests", "Configure for Integration Tests", default = false)
        )
      ),
      SimpleBootstrap("pcp.js")
    ),
    OverlordProfileConfig(
      OverlordProfile(
        "QAAutoProj",
        "QA Automation Domain",
        List(StringConfig("redshiftSchemaName", "Redshift Schema Name", default = "qa0"))
      ),
      SimpleBootstrap("qa.js")
    )
  )

  // Domain creation request validation logic. This should be extracted into a friend.

  /** Validates a domain creation request
    * @param request
    *   the domain creation request
    * @return
    *   either the request or a list of validation errors
    */
  private def validateRequest(request: DomainProperties): Validated[DomainProperties] =
    (validate("domainId", request.domainId, unusedDomainId)
      |@| validate("name", request.name, unusedFullName)
      |@| validate("hostName", request.hostName, unusedHostName, validHostName)) { (domainId, name, hostName) =>
      request
    }

  /** A validated type is either the type or a list of validation errors. */
  private type Validated[A] = ValidationNel[ValidationInfo, A]

  /** A simple validator returns an error message if the value is invalid. */
  private type Validator = String => Option[String]

  /** Validate a property value against a sequence of validators.
    * @param property
    *   the property name
    * @param value
    *   the property value
    * @param validators
    *   the validators to apply
    * @return
    *   either the property value or a list of validation errors
    */
  private def validate(property: String, value: String, validators: Validator*): Validated[String] =
    validators
      .flatMap(validator => validator(value).map(error => new ValidationInfo(property, value, error)))
      .toList match
      case head :: tail => NonEmptyList.fromSeq(head, tail).failure
      case Nil          => value.successNel

  private def unusedDomainId(did: String): Option[String] =
    domainExists(DATA_TYPE_DOMAIN_ID, did) option s"Domain identifier already in use"

  private def unusedFullName(name: String): Option[String] =
    domainExists(DATA_TYPE_NAME, name) option s"Tenant name already in use"

  private def unusedHostName(hn: String): Option[String] = // hostname is a special search
    Option(dws.getDomainByExactHost(hn)).nonEmpty option s"Host name already in use"

  private def validHostName(hn: String): Option[String] =
    hostnameRegex.findFirstIn(hn).isEmpty option s"Host name is not valid"

  private def domainExists(dataType: String, value: String): Boolean =
    queryDomains.addCondition(dataType, Comparison.eq, value, QBFunction.LOWER).getValues[Any].nonEmpty

  val hostnameRegex =
    """^(([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\-]*[a-zA-Z0-9])\.)*([A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\-]*[A-Za-z0-9])$""".r
end OverlordDomainRootApiImpl

case class OverlordProfileConfig(
  profile: OverlordProfile,
  script: BootstrapScript
)

sealed trait BootstrapScript

case object NoBootstrap extends BootstrapScript

case class SimpleBootstrap(url: String) extends BootstrapScript

case class DestrapBootstrap(urls: Seq[String]) extends BootstrapScript

case class SimpleAndDestrapBootstrap(baseSimpleUrl: String, additionalDestrapUrls: Seq[String]) extends BootstrapScript

case class DomainBootstrap(
  name: String,
  s3: Option[S3Config],
  archives: Option[Map[ArchiveIdentifier, ArchiveBootstrap]],
  components: Option[Map[ComponentIdentifier, ComponentBootstrap]],
  bootstrap: List[Destrap]
)

case class S3Config(
  identity: String,
  credential: String
)

case class ArchiveBootstrap(
  enabled: Boolean,
  car: Option[String]
)

case class ComponentBootstrap(
  enabled: Boolean,
  configuration: Option[JsonNode]
)

case class Destrap(
  phase: String,
  config: Option[JsonNode],
  setup: Option[Seq[Destrap]]
)

case class MissingConfigError(key: String)
    extends RuntimeException(s"Missing config $key in deploy/src/main/resources/user.conf")
    with NoStackTrace
