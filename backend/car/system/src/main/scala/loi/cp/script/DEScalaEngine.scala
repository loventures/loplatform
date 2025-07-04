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

package loi.cp.script

import java.io.PrintWriter
import java.net.{URL, URLClassLoader}
import javax.script.{ScriptEngine, ScriptEngineManager}

object DEScalaEngine:
  def apply(
    classPath: Seq[URL],
    printWriter: PrintWriter,
    componentClassLoader: ClassLoader,
    debug0: Boolean,
  ): ScriptEngine =
    /*
import scala.tools.nsc.Settings
import scala.tools.nsc.interpreter.IMain
import scala.tools.nsc.interpreter.shell.Scripted
    val settings = new Settings() {
      javabootclasspath.append(
        classPath
          .map(url => new File(url.toURI).getAbsolutePath)
          .mkString(File.pathSeparator)
      )
      usejavacp.value = true

      if (debug0) {
        sys.props.put("scala.repl.debug", "true") // evil side effect
        debug.value = true
        developer.value = true
        log.value = "all" :: Nil
      }
    }
    settings.embeddedDefaults(componentClassLoader) // make it use our classloader
    val classLoader = new java.net.URLClassLoader(
      files.map(_.toURI.toURL).toArray,
      /*getClass.getClassLoader*/ null // ignoring current classpath
    )
     */

    new ScriptEngineManager(componentClassLoader).getEngineByName("scala")

  def newEngine(
    printWriter: PrintWriter,
    componentClassLoader: ClassLoader,
    debug: Boolean,
  ): ScriptEngine =
    DEScalaEngine(
      scalaClassPath,
      printWriter,
      componentClassLoader,
      debug,
    )

  def scalaClassPath = classOf[dotty.tools.repl.ScriptEngine].getClassLoader match
    case urlcl: URLClassLoader => urlcl.getURLs.toSeq
    case _                     => Seq.empty
end DEScalaEngine
