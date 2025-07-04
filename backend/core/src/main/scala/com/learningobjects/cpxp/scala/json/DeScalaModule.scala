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

package com.learningobjects.cpxp.scala.json

import com.fasterxml.jackson.module.scala.*
import com.fasterxml.jackson.module.scala.deser.ScalaNumberDeserializersModule
import com.fasterxml.jackson.module.scala.introspect.*

/** Everything but untyped deserialization.
  */
class DeScalaModule
    extends JacksonModule
    with IteratorModule
    with EnumerationModule
    with OptionModule
    with SeqModule
    with IterableModule
    with TupleModule
    with MapModule
    with SetModule
    with ScalaNumberDeserializersModule
    with ScalaAnnotationIntrospectorModule
    // with UntypedObjectDeserializerModule non-desired
    with EitherModule
    with Function0Module  // ours
    with FutureModule     // ours
    with SymbolModule     // ours
    with EnumeratumModule // ours
    with ArgonautModule   // ours
    with ScalazModule     // ours
    with TryModule        // ours
    with TreeModule // ours
