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

package com.learningobjects.cpxp.component

import com.learningobjects.cpxp.component.ManifestURL.{DirectoryURL, JarURL}
import com.learningobjects.cpxp.scala.util.PathOps.*
import com.learningobjects.cpxp.util.ParallelStartup

import java.io.File
import java.net.{URI, URL}
import java.nio.file.*
import java.util as ju
import javax.tools.JavaFileObject
import scala.collection.mutable
import scala.compat.java8.OptionConverters.*
import scala.compat.java8.StreamConverters.*
import scala.concurrent.Future
import scala.jdk.CollectionConverters.*

/** This Ring is immutable. This Ring does no loading. This ring will always return the same set of ComponentArchives.
  * This ring cannot fail.
  */
final class ClassLoaderRing(classLoader: ClassLoader) extends ComponentRing:
  import ParallelStartup.ec

  val archives: List[ComponentArchive] = ParallelStartup.mapF(
    classLoader
      .getResources("car.json")
      .asScala
      .toList
  ) { r =>
    Future {
      val source = ManifestURL(r) match
        case d: DirectoryURL => new ComponentDirectory(new File(d.archiveRoot))
        case j: JarURL       => new JarSource(j)
      new BaseComponentArchive(source)
    }
  }

  // The archive dependency order here is quite linear so we have to parallelize some of
  // the loading within each archive in order to achieve our goals.

  if ParallelStartup.parallel then
    ParallelStartup.foreach(archives) { a =>
      a.scan(this)
    }
    // Compose the futures in dependency order
    def loader(loaders: mutable.Map[ComponentArchive, Future[Unit]])(a: ComponentArchive): Future[Unit] =
      loaders.getOrElseUpdate(
        a,
        Future
          .sequence(a.getDependencies.asScala.map(loader(loaders)))
          .flatMap(_ => Future(a.load(this)))
      )
    ParallelStartup.mapF(archives)(loader(mutable.Map.empty))
  else
    archives.foreach(_.scan(this))
    archives.foreach(_.load(this))
  end if

  override def getParent: ComponentRing = this

  override def addArchive(archive: ComponentArchive): Unit =
    throw new UnsupportedOperationException("Ring 0 is immutable. A new archive cannot be added to it.")

  override def getArchive(identifier: String): ComponentArchive = findArchive(identifier)

  override def findArchive(identifier: String): ComponentArchive = archives.find(_.getIdentifier == identifier).orNull

  override def getArchives: java.lang.Iterable[ComponentArchive] = archives.asJava

  override def getLocalArchives: java.lang.Iterable[ComponentArchive] = getArchives

  override def load(): Unit = ()

  override def isFailed: Boolean = false

  override def setFailed(): Unit = ()

  override def findClassFiles(
    current: ComponentArchive,
    packageName: String,
    files: java.util.List[JavaFileObject]
  ): Unit = ()

  override def getClass(current: ComponentArchive, className: String): Class[?] =
    classLoader.loadClass(className)
end ClassLoaderRing
object ClassLoaderRing:

  /** A proper Ring0 for hosting Component Archives on the Java System Classpath.
    */
  val Ring0 =
    try new ClassLoaderRing(ClassLoader.getSystemClassLoader)
    catch
      case wat: Throwable =>
        wat.printStackTrace()
        throw wat
end ClassLoaderRing

/** Tags if a URL to a CAR manifest is in a Jar or in a directory.
  */
sealed trait ManifestURL:
  def archiveRoot: URI
object ManifestURL:
  def apply(url: URL): ManifestURL =
    url.getProtocol match
      case "jar"  => JarURL(url)
      case "file" => DirectoryURL(url)
  case class JarURL(url: URL) extends ManifestURL:
    override def archiveRoot: URI =
      URI.create(url.toExternalForm.replace("!/car.json", "!/"))
  case class DirectoryURL(url: URL) extends ManifestURL:
    override def archiveRoot: URI =
      URI.create(url.toExternalForm.replace(s"${File.separator}car.json", ""))
end ManifestURL

final class JarSource(manifestURL: JarURL) extends ComponentSource:
  private val jfs =
    val uri = manifestURL.archiveRoot
    try FileSystems.getFileSystem(uri)
    catch
      case fse: FileSystemNotFoundException => FileSystems.newFileSystem(uri, ju.Collections.emptyMap[String, String]())

  private def resources: Vector[Path] = {
    for
      path <- jfs.getRootDirectories.asScala
      file <- Files.walk(path).toScala[Iterator]
    yield file
  }.to(Vector)

  override def getCollection: ComponentCollection = null

  override def getIdentifier: String = manifestURL.archiveRoot.toString

  override def getVersion = "0.0"

  override def getLastModified: Long = Files.getLastModifiedTime(Paths.get(manifestURL.archiveRoot)).toMillis

  override def getResources: ju.Map[String, Path] =
    resources.map(path => jfs.getPath("/").relativize(path).toString -> path).toMap.asJava

  override def getResource(name: String): ju.Optional[Path] =
    Option(jfs.getPath(name))
      .filter(_.exists)
      .asJava

  override def getLastModified(name: String): Long =
    getResource(name).asScala
      .map(path => Files.getLastModifiedTime(path).toMillis)
      .getOrElse(-1L)
end JarSource
