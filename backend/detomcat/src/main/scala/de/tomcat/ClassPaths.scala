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

import java.io.{File, FileInputStream}
import java.net.URL
import java.nio.file.{Files, StandardCopyOption}
import java.util.jar.{JarFile, Manifest as JManifest}
import java.util.logging.Logger
import java.util.zip.ZipInputStream
import org.apache.catalina.WebResource
import scaloi.json.ArgoExtras

import scala.jdk.CollectionConverters.*
import scala.io.Source

object ClassPaths:
  var log = Logger.getLogger("de.tomcat.ClassPaths")

  val archiveSuffixes = List("jar", "war", "ear", "par", "car")

  /** A Full class path of artifacts from the current build. These may or may not be components, but can change at any
    * time and trigger classloader/component environment reloads.
    */
  private[tomcat] var appClassPath: Seq[File] = Seq.empty

  /** An internal dependency that is a directory.
    */
  private[tomcat] def appDirs: Seq[File] = appClassPath filter { _.isDirectory }

  /** An internal dependency dir that is not a Component
    */
  private[tomcat] def appNormalDirs: Seq[File] = appDirs filterNot isComponentDir

  /** An internal dependency that is a Component Dir
    */
  private[tomcat] def appComponentDirs: Seq[File] = appDirs filter isComponentDir

  /** A internal dependency is in a archive format, possibly a component.
    */
  private[tomcat] def appArchives: Seq[File] = appClassPath filter { _.isFile }

  /** An internal archive that is a component, and needs to be exploded.
    */
  private[tomcat] def appComponentJars: Seq[File] =
    val (components, _) = findComponents(appArchives)
    components

  /** An internal archive that is not a component.
    */
  private[tomcat] def appNormalJars: Seq[File] =
    val (_, jars) = findComponents(appArchives)
    jars

  private[tomcat] def findComponents(classPath: Seq[File]) =
    classPath filter readableArchive partition { file => explodeable(file) || hasCarJson(file) }

  private[tomcat] def toFile(webResource: WebResource) =
    new File(webResource.getURL.getFile)
  private[tomcat] def toURL(file: File)                = file.toURI.toURL
  private[tomcat] def toJarURL(file: File): URL        =
    val url = file.toURI.toURL
    // if (!(url.toString startsWith "jar:"))
    //  new URL(s"jar:$url!/")
    // else
    url

  def readableArchive(file: File) =
    archiveSuffixes.exists(suffix => file.getName.endsWith("." + suffix)) && file.isFile && file.canRead

  def explodeable(file: File): Boolean =
    val jar      = new JarFile(file.getCanonicalPath)
    val manifest = jar.getManifest
    // TODO: There must be some simpler way to express this.
    val result   =
      if manifest != null then
        manifest.getMainAttributes.asScala
          .exists { case (key, value) =>
            key.toString == "Implementation-Explodeable" && value == "true"
          }
      else false
    jar.close()
    result
  end explodeable

  def hasCarJson(file: File): Boolean =
    val jar    = new JarFile(file.getCanonicalPath)
    val entry  = jar.getEntry("car.json")
    val result = entry != null && !entry.isDirectory
    jar.close()
    result

  def explodeJar(file: File, dest: File): Unit =
    dest.mkdirs()
    log.config(s"Unzipping $file to $dest")
    val zis   = new ZipInputStream(new FileInputStream(file))
    var entry = zis.getNextEntry
    while entry != null do
      val fileName   = entry.getName
      val outputFile = new File(dest, fileName)
      if entry.isDirectory then outputFile.mkdirs()
      else
        val parent = outputFile.getParentFile
        parent.mkdirs()
        Files.copy(zis, outputFile.toPath, StandardCopyOption.REPLACE_EXISTING)
      entry = zis.getNextEntry
    zis.closeEntry()
    zis.close()
  end explodeJar

  def symLinkComponentDir(componentDir: File)(workingDir: File) =
    import argonaut.*
    import Argonaut.*
    implicit def componentJsonCodec: CodecJson[ComponentJson] =
      casecodec2(ComponentJson.apply, ArgoExtras.unapply)("identifier", "implementation")
    implicit def carJsonCodec: CodecJson[CarJson]             =
      casecodec3(CarJson.apply, ArgoExtras.unapply)("identifier", "dependencies", "components")
    def createSymLink(car: CarJson)                           =
      val link = new File(componentDir, car.identifier)
      log.config(s"Symlinking $link to $workingDir")
      Files.deleteIfExists(link.toPath)
      Files.createSymbolicLink(link.toPath, workingDir.toPath)
    val source                                                = Source.fromFile(new File(workingDir, "car.json"))
    val carString                                             =
      try source.mkString
      finally source.close
    val carJson                                               = Parse.decodeEither[CarJson](carString)
    carJson.fold(println, createSymLink)
  end symLinkComponentDir

  // Determines if a directory is a component directory.
  def isComponentDir(componentDir: File): Boolean =
    val manifestFile = new File(componentDir, "META-INF/Manifest.MF")
    val carJsonFile  = new File(componentDir, "car.json")
    if carJsonFile.exists() && carJsonFile.isFile then true
    else if manifestFile.isFile && manifestFile.canRead then
      val manifest = new JManifest(new FileInputStream(manifestFile))
      manifest.getMainAttributes.asScala.exists { case (key, value) =>
        key.toString == "Implementation-ComponentArchive" && value == "true"
      }
    else false
  end isComponentDir

  val blackListJarPatterns: Seq[String => Boolean] = Seq(
    (name: String) => name `contains` "tomcat-",
    (name: String) => name `contains` "javax.el",
    (name: String) => name `contains` "javaee-api",
    (name: String) => name `contains` "servlet-api",
    (name: String) => name `contains` "car-compile-classes"
  )
end ClassPaths

case class CarJson(identifier: String, dependencies: List[String], components: List[ComponentJson])

case class ComponentJson(identifier: String, implementation: String)
