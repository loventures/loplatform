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

package loi.cp.overlord

import com.learningobjects.cpxp.BaseServiceMeta
import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance, ComponentSupport}
import com.learningobjects.cpxp.util.ManagedUtils
import com.typesafe.config.Config
import loi.cp.ldap.{LdapLoginSystem, LdapLoginSystemComponent}
import loi.cp.login.{DirectFallbackLoginProvider, DirectLoginSystem}
import loi.cp.session.FallbackLoginProviderComponent

/** This component provides the login mechanism used by the overlord domain which has no login system configured in the
  * database.
  *
  * This looks at the context.xml cluster type property (Cluster/type) and if "Local" or unset, it uses direct login
  * (database auth, with the default account from the overlord dump.xml). Otherwise it returns a synthetic LDAP login
  * provider. If the cluster type is "Production" (i.e. an actual production cluster) then it returns a production LDAP
  * profile, otherwise a lower-environment LDAP profile.
  */
@Component(suppresses = Array(classOf[DirectFallbackLoginProvider]))
class OverlordFallbackLoginProvider(
  val componentInstance: ComponentInstance,
  config: Config
) extends FallbackLoginProviderComponent
    with ComponentImplementation:
  override def fallbackLoginSystem =
    if BaseServiceMeta.getServiceMeta.isLocal then ComponentSupport.get(classOf[DirectLoginSystem])
    else
      val olls = new OverlordLdapLoginSystem(BaseServiceMeta.getServiceMeta.isProduction, config)
      ManagedUtils.di(olls, true)
      olls
end OverlordFallbackLoginProvider

/** A synthetic LDAP login system. This authenticates users against the LO JumpCloud provider.
  *
  * For production clusters, users must be members of either the lorde-over LDAP group (full overlord access) or the
  * lorde-under LDAP group (underlord access, for tenant provisioning). Users who are members of neither group will get
  * an auth failure error.
  *
  * For non-production clusters, users must be members of the lorde-lower LDAP group (full overlord access).
  *
  * For more-specific groups, configure a context.xml Ldap/groupSuffix property value such as "ple". Authentication will
  * then look for membership in lorde-{over|under|lower}-ple as appropriate.
  *
  * @param prod
  *   whether this is a production cluster
  */
class OverlordLdapLoginSystem(prod: Boolean, config: Config) extends LdapLoginSystem with LdapLoginSystemComponent:

  private val ldapConf = config.getConfig("loi.cp.overlord.ldap")
  // While this config doesn't belong in code, rolling it out to production systems
  // is not straightforward.

  private def groupSuffix: String =
    ldapConf.getString("groupSuffix") match
      case "" => ""
      case g  => s"-$g"

  override def getUrl: String =
    ldapConf.getString("url")

  override def getUsername: String =
    ldapConf.getString("authDN")

  override def getPassword: String =
    ldapConf.getString("authPassword")

  // append context.xml config to the default config, will overwrite the defaults
  override def getConfig: String =
    envConfig + "\n" + ldapConf.getString("config")

  def envConfig: String = if prod then ProductionConfig else LowerEnvConfig

  val BaseDN = ldapConf.getString("baseDN")

  val BaseConfig = s"""connect_timeout=15
                       |operation_timeout=15
                       |credential_cache=-1
                       |local_accounts=false""".stripMargin

  val LowerEnvConfig = s"""$BaseConfig
                          |user_types=over,under,support
                          |${UserConfig("over", "overlord", "lorde-lower")}
                          |${UserConfig("under", "underlord", "lorde-lower-under")}
                          |${UserConfig("support", "support", "lorde-lower-support")}""".stripMargin

  val ProductionConfig = s"""$BaseConfig
                            |user_types=over,under,support
                            |${UserConfig("over", "overlord", "lorde-over" + groupSuffix)}
                            |${UserConfig("under", "underlord", "lorde-under" + groupSuffix)}
                            |${UserConfig("support", "support", "lorde-support" + groupSuffix)}""".stripMargin

  def UserConfig(prefix: String, role: String, group: String): String =
    s"""${prefix}_base=$BaseDN
       |${prefix}_scope=one
       |${prefix}_filter=(&(uid={userName})(objectClass=inetOrgPerson)(memberOf=cn=$group,$BaseDN))
       |${prefix}_create=true
       |${prefix}_userName={uid}
       |${prefix}_familyName={sn}
       |${prefix}_givenName={givenName}
       |${prefix}_emailAddress={mail}
       |${prefix}_role=$role""".stripMargin
end OverlordLdapLoginSystem
