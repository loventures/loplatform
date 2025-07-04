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

package de.tomcat

import java.io.File
import java.net.URL
import java.nio.file.Files
import java.util.logging.Logger

import org.apache.catalina.WebResourceRoot.ResourceSetType
import org.apache.catalina.loader.{ParallelWebappClassLoader, WebappLoader}
import org.apache.catalina.{WebResource, WebResourceRoot}

import scala.collection.mutable
import scala.util.Try

object DEWebappLoader:
  var logger         = Logger.getLogger(classOf[DEWebappLoader].getName)
  private val clHack = new DEClassLoader(null) // Do not use
class DEWebappLoader(webappClasspath: Seq[File], resourceRoot: WebResourceRoot) extends WebappLoader:
  import ClassPaths.*

  var log = Logger.getLogger("de.tomcat.DEWebappLoader")

  appClassPath = webappClasspath

  appNormalJars foreach { file =>
    resourceRoot.createWebResourceSet(
      ResourceSetType.RESOURCE_JAR,
      s"/WEB-INF/lib/${file.getName}",
      toJarURL(file),
      "/"
    )
  }
  appNormalJars filter { _.isFile } foreach { file =>
    resourceRoot.createWebResourceSet(ResourceSetType.RESOURCE_JAR, "/", toJarURL(file), "/")
  }
  setLoaderClass(classOf[DEClassLoader].getName)
end DEWebappLoader

// TODO: what is this anymore?

/** This classloader finds jars in WEB-INF/lib that are components, blacklisting them from the webapp classloader and
  * exploding them to the component directory.
  */
object DEClassLoader:
  val logger           = Logger.getLogger(classOf[DEClassLoader].getName)
  private var register = true
class DEClassLoader(parentClassLoader: ClassLoader) extends ParallelWebappClassLoader(parentClassLoader):
  import ClassPaths.*
  import DEClassLoader.*

  if register then
    if ClassLoader.registerAsParallelCapable() then logger.fine("Registered DEClassLoader as parallel capable.")
    register = false

  val localRepos = mutable.Buffer[URL]()

  localRepos ++= appNormalDirs map toURL
  localRepos ++= (appNormalJars map toJarURL)

  def deleteR(file: File): Unit =
    val path = file.toPath
    if file.isDirectory && !Files.isSymbolicLink(path) then
      file.listFiles().foreach(deleteR)
      Files.deleteIfExists(path)
      ()
    else
      Files.deleteIfExists(path)
      ()

  override def start(): Unit =

    val webInfClasses: WebResource = resources.getResource("/WEB-INF/classes")
    val componentsDir              = new File(resources.getResource("/WEB-INF/").getCanonicalPath, "components")

    if webInfClasses.isDirectory && webInfClasses.canRead && !isComponentDir(new File(webInfClasses.getCanonicalPath))
    then
      localRepos += webInfClasses.getURL
      addURL(webInfClasses.getURL)

    // Here we look for jars in WEB-INF/lib that should be exploded instead of being used directly.
    // We also blacklist those exploded jars. As they should be
    val webInfLibs: Array[WebResource] = resources.listResources("/WEB-INF/lib")
    val (webInfComponents, webInfJars) = webInfLibs map toFile filter readableArchive partition { file =>
      explodeable(file) || hasCarJson(file)
    }

    localRepos ++= webInfJars map toURL

    componentsDir.mkdirs()
    (appComponentJars ++ webInfComponents) foreach { jar =>
      val dest = new File(componentsDir, jar.getName.stripSuffix(".jar"))
      deleteR(dest)
      explodeJar(jar, dest) // TODO Strip other suffixes
    }

    appComponentDirs foreach symLinkComponentDir(componentsDir)
    localRepos filter notBlacklisted foreach addURL
  end start

  def blacklisted(url: URL): Boolean    = blackListJarPatterns exists { pattern => pattern(url.getFile) }
  def notBlacklisted(url: URL): Boolean = !blacklisted(url)

  /** It's my party and I'll load resources if I want to. * */
  override def checkStateForResourceLoading(resource: String): Unit = {}

  override def getURLs: Array[URL] =
    ((localRepos filter notBlacklisted) ++ super.getURLs).toArray[URL]

  override def copyWithoutTransformers(): DEClassLoader =
    val newLoader = new DEClassLoader(parentClassLoader)
    super.copyStateWithoutTransformers(newLoader)
    Try(newLoader.start())
    newLoader
end DEClassLoader
