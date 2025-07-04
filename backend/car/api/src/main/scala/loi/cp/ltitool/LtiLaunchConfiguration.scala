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

package loi.cp.ltitool

/** Holds the configuration for an Lti launch.
  *
  * @param url
  *   The launch url that this tool should POST to.
  * @param key
  *   the oauth_consumer_key to identify the referring service.
  * @param secret
  *   the oauth_consumer_secret to sign each request with.
  * @param launchStyle
  *   The presentation of the launch to the user (new window, or framed), see [[LtiLaunchStyle]]
  * @param includeUsername
  *   Include a user's username in a launch to this Tool Provider.
  * @param includeRoles
  *   Include a user's role in a launch to this Tool Provider.
  * @param includeEmailAddress
  *   Include a user's email address in a launch to this Tool Provider.
  * @param includeContextTitle
  *   Include the title of the context where this tool was launched from (Usually a course).
  * @param useExternalId
  *   Include a user's external identifier in a launch to this Tool Provider.
  * @param ltiVersion
  *   the version that should be sent to this Tool Provider
  * @param ltiMessageType
  *   the message type that should be sent to this Tool Provider
  * @param customParameters
  *   A map of custom parameters that should be sent to the Provider, where the keys are the name of the parameter, and
  *   the corresponding values will be sent as the value. Note: keys in this map will and should not be prefixed with
  *   'custom_'.
  */
final case class LtiLaunchConfiguration(
  url: Option[String] = None,
  key: Option[String] = None,
  secret: Option[String] = None,
  clientId: Option[String] = None,
  deploymentId: Option[String] = None,
  keysetUrl: Option[String] = None,
  loginUrl: Option[String] = None,
  redirectionUrls: Option[String] = None,
  deepLinkUrl: Option[String] = None,
  launchStyle: Option[LtiLaunchStyle] = None,
  includeUsername: Option[Boolean] = None,
  includeRoles: Option[Boolean] = None,
  includeEmailAddress: Option[Boolean] = None,
  includeContextTitle: Option[Boolean] = None,
  useExternalId: Option[Boolean] = None,
  isGraded: Option[Boolean] = None,
  ltiVersion: Option[String] = None,
  ltiMessageType: Option[String] = None,
  customParameters: Map[String, String] = Map.empty
):

  /** Merges this [[LtiLaunchConfiguration]] to a [[LtiToolConfiguration]], taking values from ltiToolConfiguration
    * depending on whether ltiToolConfiguration allows them to be editable.
    *
    * @param ltiToolConfiguration
    * @return
    */
  def applyDefaultLtiConfig(ltiToolConfiguration: LtiToolConfiguration): LtiLaunchConfiguration =
    val editcfg    = ltiToolConfiguration.instructorEditable
    val defaultCfg = ltiToolConfiguration.defaultConfiguration

    LtiLaunchConfiguration(
      url = resolveConfigValue(editcfg.url, this.url, defaultCfg.url),
      secret = resolveConfigValue(editcfg.secret, this.secret, defaultCfg.secret),
      ltiVersion = resolveConfigValue(true, this.ltiVersion, defaultCfg.ltiVersion),
      ltiMessageType = resolveConfigValue(true, this.ltiMessageType, defaultCfg.ltiMessageType),
      key = resolveConfigValue(editcfg.key, this.key, defaultCfg.key),
      clientId = resolveConfigValue(editcfg.clientId, this.clientId, defaultCfg.clientId),
      deploymentId = resolveConfigValue(editcfg.deploymentId, this.deploymentId, defaultCfg.deploymentId),
      keysetUrl = resolveConfigValue(editcfg.keysetUrl, this.keysetUrl, defaultCfg.keysetUrl),
      loginUrl = resolveConfigValue(editcfg.loginUrl, this.loginUrl, defaultCfg.loginUrl),
      redirectionUrls = resolveConfigValue(editcfg.redirectionUrls, this.redirectionUrls, defaultCfg.redirectionUrls),
      deepLinkUrl = resolveConfigValue(editcfg.deepLinkUrl, this.deepLinkUrl, defaultCfg.deepLinkUrl),
      launchStyle = resolveConfigValue(editcfg.launchStyle, this.launchStyle, defaultCfg.launchStyle),
      includeUsername = resolveConfigValue(editcfg.includeUsername, this.includeUsername, defaultCfg.includeUsername),
      includeRoles = resolveConfigValue(editcfg.includeRoles, this.includeRoles, defaultCfg.includeRoles),
      useExternalId = resolveConfigValue(editcfg.includeExternalId, this.useExternalId, defaultCfg.useExternalId),
      isGraded = resolveConfigValue(editcfg.isGraded, this.isGraded, defaultCfg.isGraded),
      includeEmailAddress =
        resolveConfigValue(editcfg.includeEmailAddress, this.includeEmailAddress, defaultCfg.includeEmailAddress),
      includeContextTitle =
        resolveConfigValue(editcfg.includeContextTitle, this.includeContextTitle, defaultCfg.includeContextTitle),
      customParameters = resolveCustomParameters(ltiToolConfiguration)
    )
  end applyDefaultLtiConfig

  /** Resolves the correct visibility value base on whether or not it is editable.
    */
  private def resolveVisibleConfig[A](isEditable: Boolean, a: Option[A], b: Option[A]): Option[A] =
    if isEditable then a orElse b
    else None

  /** Resolves the correct configured value base on whether or not it is editable.
    */
  private def resolveConfigValue[A](isEditable: Boolean, overRide: Option[A], default: Option[A]): Option[A] =
    if isEditable then overRide orElse default
    else default

  /** This resolves custom parameters, both Instructor Added and Instructor Edited In doing this, we have to be careful
    * to cover both cases distinctly
    *
    * If ADDING Custom Parameters is disallowed, we still want to allow instructors to EDIT built-in ones
    *
    * If ADDING Custom Parameters is permitted, we still want to be able to prevent EDIT of built-in ones
    */
  private def resolveCustomParameters(toolConfig: LtiToolConfiguration) =
    if customParameters != null then
      val allowAddedCustomParams = toolConfig.instructorEditable.customParameters

      val allowedEditableParams = toolConfig.instructorEditable.editableCustomParameters
      val defaultParams         = toolConfig.defaultConfiguration.customParameters

      // do not take local params if the param is defined in defaultConfigs, and it is not in editableConfigs
      var localCustomParams = customParameters.view.filterKeys(!toolConfig.customParamIsUneditable(_)).toMap

      if !allowAddedCustomParams then
        // Added params is off - only include the known editable params
        localCustomParams = localCustomParams.view.filterKeys(allowedEditableParams.contains(_)).toMap

      defaultParams ++ localCustomParams
    else toolConfig.defaultConfiguration.customParameters
end LtiLaunchConfiguration

object LtiLaunchConfiguration:
  final val empty = LtiLaunchConfiguration()
