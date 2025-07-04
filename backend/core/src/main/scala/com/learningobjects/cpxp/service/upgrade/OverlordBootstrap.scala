package com.learningobjects.cpxp.service.upgrade

import com.learningobjects.cpxp.util.PasswordUtils
import com.typesafe.config.Config

import java.util
import scala.jdk.CollectionConverters.*
import scala.util.control.NoStackTrace

object OverlordBootstrap:
  def overlordProps(config: Config): util.Map[String, String] =
    val username = configOrFail(config, "overlord.username")
    val password = configOrFail(config, "overlord.password")
    Map(
      "overlord_root.username" -> username,
      "overlord_root.password" -> PasswordUtils.encodePassword("overlord", username, password)
    ).asJava

  private def configOrFail(config: Config, key: String): String =
    if !config.hasPath(key) then throw MissingConfigError(key)
    else config.getString(key)

case class MissingConfigError(key: String)
    extends RuntimeException(s"Missing config $key in deploy/src/main/resources/user.conf")
    with NoStackTrace
