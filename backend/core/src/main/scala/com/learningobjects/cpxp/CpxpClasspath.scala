/*
 * LO Platform copyright (C) 2007–2026 LO Ventures LLC.
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

package com.learningobjects.cpxp

import io.github.classgraph.*

import _root_.scala.io as scio
import _root_.scala.jdk.CollectionConverters.*

/** A centralized repository of knowledge relating to the boot classpath. */
object CpxpClasspath:
  private val logger = org.log4s.getLogger

  private var cg: Option[ScanResult] = None

  /** ClassGraph 6 no longer loads classes, so we keep the loader to load them with. */
  @volatile private var loader: ClassLoader = getClass.getClassLoader

  def classGraph: ScanResult =
    cg.getOrElse(throw new IllegalStateException("No class graph"))

  def reflect(): ScanResult =
    if cg.isDefined then throw new IllegalStateException("Reflecteded already")
    logger info packages.mkString("Reflecting on ", ", ", ".")
    Option(Thread.currentThread.getContextClassLoader).foreach(loader = _)
    cg = Some(scan(new ClassGraph().acceptPackages(packages*)))
    cg.get

  /** Scans the classpath and visible module layers with everything enabled, as ClassGraph 4's `enableAllInfo()` did.
    */
  def scan(classGraph: ClassGraph): ScanResult =
    classGraph
      .enableClasspath()
      .enableNonSystemModules()
      .enableClassInfo()
      .enableFieldInfo()
      .enableMethodInfo()
      .enableAnnotationInfo()
      .enableStaticFinalFieldConstantInitializerValues()
      .ignoreClassVisibility()
      .ignoreFieldVisibility()
      .ignoreMethodVisibility()
      .scan()

  /** Loads a scanned class, without initializing it. */
  def loadClass(classInfo: ClassInfo): Class[?] =
    Class.forName(classInfo.getName, false, loader)

  /** Loads scanned classes, without initializing them. */
  def loadClasses(classInfos: ClassInfoList): java.util.List[Class[?]] =
    classInfos.asScala.map(loadClass).asJava

  lazy val packages: Array[String] = // for truth and unity!
    getClass.getClassLoader
      .getResources("META-INF/cpxp.classpath")
      .asScala
      .flatMap { url =>
        scio.Source.fromInputStream(url.openStream(), "UTF-8").getLines()
      }
      .filter(_.nonEmpty)
      .toArray

  // Tests may run in parallel in the jvm so should allow multi-init.. I know this should be in test scope
  def initForTesting(): Unit = if cg.isEmpty then reflect()
end CpxpClasspath
