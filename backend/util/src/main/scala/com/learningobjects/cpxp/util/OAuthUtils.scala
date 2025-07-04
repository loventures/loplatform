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

package com.learningobjects.cpxp.util

import loi.net.oauth.{OAuthAccessor, OAuthConsumer, OAuthMessage}
import org.apache.commons.codec.binary.Base64
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.lang3.StringUtils
import org.apache.commons.validator.routines.UrlValidator

import java.net.URLEncoder
import java.util.Properties
import scala.jdk.CollectionConverters.*

object OAuthUtils:
  private val LTI_MESSAGE_TYPE = "basic-lti-launch-request"
  private val LTI_VERSION      = "LTI-1p0"
  private val OAUTH_CALLBACK   = "about:blank"

  @throws(classOf[Exception])
  def getOAuthParameters(
    toolUrl: String,
    toolKey: String,
    toolSecret: String,
    properties: Properties
  ): List[(String, String)] = getOAuthParameters("POST", toolUrl, toolKey, toolSecret, properties)

  @throws(classOf[Exception])
  def getOAuthParameters(
    method: String,
    toolUrl: String,
    toolKey: String,
    toolSecret: String,
    properties: Properties
  ): List[(String, String)] =
    val props         = new Properties
    if properties != null then props.putAll(properties)
    props.setProperty("lti_message_type", LTI_MESSAGE_TYPE)
    props.setProperty("lti_version", LTI_VERSION)
    props.setProperty("oauth_callback", OAUTH_CALLBACK)
    val oAuthConsumer = new OAuthConsumer(OAUTH_CALLBACK, toolKey, toolSecret, null)
    val oAuthAccessor = new OAuthAccessor(oAuthConsumer)
    val oAuthMessage  = oAuthAccessor.newRequestMessage(method, toolUrl, props.entrySet)
    oAuthMessage.getParameters.asScala.map(e => e.getKey -> e.getValue).toList
  end getOAuthParameters

  @throws(classOf[Exception])
  def getOAuthParameterMap(
    initParams: Map[String, String],
    connectionUrl: String,
    consumerKey: String,
    secret: String,
    formMethod: String
  ): Map[String, String] =
    // ensure proper params
    if StringUtils.isEmpty(connectionUrl) then throw new IllegalArgumentException("Missing connectionUrl")
    val schemes      = Array("http", "https")
    val urlValidator = new UrlValidator(schemes, UrlValidator.ALLOW_LOCAL_URLS)
    if !urlValidator.isValid(connectionUrl) then
      throw new IllegalArgumentException("OAuth connectionUrl is invalid: " + connectionUrl)
    if StringUtils.isEmpty(consumerKey) then throw new IllegalArgumentException("Missing consumerKey")
    if StringUtils.isEmpty(secret) then throw new IllegalArgumentException("Missing secret")

    // defaults to GET
    val method        = if "POST".equals(formMethod) then OAuthMessage.POST else OAuthMessage.GET
    val oAuthMessage  = new OAuthMessage(method, connectionUrl, initParams.asJava.entrySet)
    val oAuthConsumer = new OAuthConsumer(null, consumerKey, secret, null)
    val oAuthAccessor = new OAuthAccessor(oAuthConsumer)
    oAuthMessage.addRequiredParameters(oAuthAccessor)
    oAuthMessage.getParameters.asScala.map(e => e.getKey -> e.getValue).toMap
  end getOAuthParameterMap

  @throws(classOf[Exception])
  private def getOAuthParameterMap(
    initParams: Map[String, String],
    connectionUrl: String,
    consumerKey: String,
    secret: String,
    body: String,
    formMethod: String
  ): Map[String, String] =
    val bodyHash = new String(Base64.encodeBase64(DigestUtils.sha1(body))) // base 64 encoded sha-1 hash

    val params = initParams + ("oauth_body_hash" -> bodyHash)
    getOAuthParameterMap(params, connectionUrl, consumerKey, secret, formMethod)
  end getOAuthParameterMap

  /** For use signing non application/x-www-form-urlencoded encoded posts
    */
  @throws(classOf[Exception])
  def getOAuthHeaderString(
    connectionUrl: String,
    consumerKey: String,
    secret: String,
    body: String,
    formMethod: String
  ): String =
    val oauthParams   = getOAuthParameterMap(Map(), connectionUrl, consumerKey, secret, body, formMethod)
    val realmed       = oauthParams + ("realm" -> connectionUrl)
    val encodedParams = realmed.map { case (key, value) => s"$key=\"${URLEncoder.encode(value, "UTF-8")}\"" }

    s"OAuth ${encodedParams.mkString(", ")}"
  end getOAuthHeaderString
end OAuthUtils
