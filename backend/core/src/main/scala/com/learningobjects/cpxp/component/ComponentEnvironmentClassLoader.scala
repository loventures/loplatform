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

import java.net.URL
import scala.jdk.CollectionConverters.*

/* This is something of a crap class loader. It loads classes from the component
 * environment by scanning the archive class loaders, but they have no linkage
 * with this class loader, they simply defer to the thread-local ComponentRing
 * to see where to load classes they need. Really, all the archive class loaders
 * should be children of this and then the standard parent delegation might, in
 * some way, work. With such a configuration, when you asked an archive class
 * loader for a class, it would call the environment class loader which would
 * in turn ask all the archive class loaders (including you). If we really
 * cared, the environment class loader would in fact be a ring class loader. */
class ComponentEnvironmentClassLoader(
  environment: BaseComponentEnvironment,
  parent: ClassLoader
) extends ClassLoader(parent):
  override def findClass(name: String): Class[?] =
    val found = for
      archive <- environment.getArchives.asScala.to(LazyList)
      loader   = archive.getClassLoader
      if loader `containsClass` name
    yield loader.loadClass(name)
    found.headOption.getOrElse(throw new ClassNotFoundException(name))

  override def findResource(name: String): URL =
    val found = for
      archive  <- environment.getArchives.asScala.to(LazyList)
      loader    = archive.getClassLoader
      resource <- Option(loader `findResource` name)
    yield resource
    found.headOption.orNull
end ComponentEnvironmentClassLoader
