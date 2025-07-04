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

package loi.nashorn

import org.openjdk.nashorn.api.scripting.AbstractJSObject

import java.io.Reader
import javax.script.ScriptContext

class Nashorn:
  private val fac    = new org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory()
  private val engine = fac.getScriptEngine

  def bindFunction(name: String)(function: Seq[Any] => AnyRef) =
    engine
      .getBindings(ScriptContext.ENGINE_SCOPE)
      .put(
        name,
        new AbstractJSObject:
          override def call(thiz: Any, args: Any*): AnyRef =
            function.apply(args)
      )

  def eval(source: String): AnyRef =
    engine.eval(source)

  def eval(reader: Reader): AnyRef =
    engine.eval(reader)
end Nashorn
