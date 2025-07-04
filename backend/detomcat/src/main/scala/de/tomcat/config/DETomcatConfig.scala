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

package de.tomcat.config

import com.typesafe.config.*
import scopt.OptionParser

import java.io.File

/** Configuration options for DE Tomcat.
  * @param baseDirectory
  *   A directory for tomcat to use a working directory.
  * @param logDirectory
  *   A directory for tomcat to write log files.
  * @param port
  *   Port number for tomcat to list for http connections.
  * @param ssl
  *   Enable/disable SSL.
  * @param sslPort
  *   Port number for tomcat to list to https connection.
  * @param color
  *   Enable/disable ANSI color escapes characters on output to the console logger.
  * @param banner
  *   Banner text to use when tomcat starts up.
  * @param logger
  *   logging format to use in the log file.
  * @param executor
  *   Thread pool executor configuration
  */
case class DETomcatConfig(
  baseDirectory: File,
  logDirectory: File,
  port: Int,
  ssl: Boolean,
  sslPort: Int,
  color: Boolean,
  banner: String,
  logger: String,
  maxHttpHeaderSize: Int,
  executor: ExecutorConfig,
  sslConfig: SSLConfig
)

object DETomcatConfig:
  def parser = new OptionParser[DETomcatConfig]("de-tomcat"):
    head("de-tomcat", "0.1-SNAPSHOT", "A command line frontend for Difference Engine web applications based on Tomcat.")
    opt[Int]('p', "port") text "The port for listening to http request." action { (port, config) =>
      require(port > 0)
      config.copy(port = port)
    }
    opt[Int]("sslPort") text "The port for listening to SSL http request." action { (port, config) =>
      require(port > 0 && port != config.port)
      config.copy(sslPort = port)
    }
    opt[Boolean]("ssl") text "Enable ssl." action { (enabled, config) =>
      config.copy(ssl = enabled)
    }
    opt[File]('b', "base") text "The tomcat base directory to use." action { (path, config) =>
      config.copy(baseDirectory = path)
    }
    opt[File]('l', "logdir") text "The tomcat log directory to use." action { (path, config) =>
      config.copy(logDirectory = path)
    }
    help("help") text "Prints this usage text."
    version("version")

  def load(base: Config): DETomcatConfig =
    val config            = base.getConfig("de.tomcat")
    val color             = config.getBoolean("color")
    val baseDir           = config.getString("baseDirectory")
    val logDirectory      = config.getString("logDirectory")
    val port              = config.getInt("port")
    val ssl               = config.getBoolean("ssl")
    val sslPort           = config.getInt("sslPort")
    val logger            = config.getString("logger") // TODO: Make Enum
    val maxHttpHeaderSize = config.getInt("maxHttpHeaderSize")
    val banner            =
      import de.tomcat.Banner.*
      config.getString("banner").toLowerCase match
        case "shadowcat"                     => shadowCat
        case "cat" | "nyancat"               => cat
        case "doomcat"                       => doomCat
        case "electrocat"                    => electroCat
        case "3d" | "threed" | "threedeecat" => ThreeDeeCat
        case "catslevania"                   => catslevania
        case "flamecat"                      => flameCat
        case "random" | "randocat"           => randomCat
        case n                               => config.getString("banner")
      end match
    end banner
    val executor          = ExecutorConfig.fromConfig(config.getConfig("executorPool"))
    val sslConfig         = SSLConfig.load(base)
    DETomcatConfig(
      color = color,
      baseDirectory = new File(baseDir),
      logDirectory = new File(logDirectory),
      port = port,
      ssl = ssl,
      sslPort = sslPort,
      logger = logger,
      maxHttpHeaderSize = maxHttpHeaderSize,
      executor = executor,
      banner = banner,
      sslConfig = sslConfig
    )
  end load
end DETomcatConfig
