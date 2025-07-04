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

package loi.cp.web.converter

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStream}
import java.util.function.Supplier

import com.fasterxml.jackson.databind.util.TokenBuffer
import com.fasterxml.jackson.databind.*
import com.learningobjects.cpxp.component.annotation.{Instrument, Service}
import com.learningobjects.cpxp.util.HttpUtils
import jakarta.servlet.http.{HttpServletRequest, HttpServletResponse}
import javax.validation.groups.Default
import loi.cp.web.mediatype.ObjectEntity
import org.apache.commons.io.IOUtils
import scala.util.Using
import scaloi.syntax.AnyOps.*

// When we eliminate our classloaders we may be able to summon the
// APM class transformer and eliminate the interface here

/** Instrumentable Jackson I/O operations. */
@Service
@Instrument
trait JacksonHttpMessageIO:

  /** Read the contents of a stream and make it available for reading from memory. */
  def read(in: InputStream, initialCapacity: Int): Supplier[InputStream]

  /** Parse a json node. */
  def parse(in: InputStream): JsonNode

  /** Parse a java type. */
  def parse(view: Option[Class[?]], tpe: JavaType, in: InputStream): AnyRef

  /** Serialize an object to JSON. */
  def serialize(o: AnyRef): Array[Byte]

  /** Serialize an object to JSON node. */
  def valueToTree(o: AnyRef): JsonNode

  /** Write a byte array to a client. */
  def write(bytes: Array[Byte], request: HttpServletRequest, response: HttpServletResponse): Unit
end JacksonHttpMessageIO

/** Default implementation of Jackson I/O. */
@Service
class JacksonHttpMessageIOImpl(mapper: ObjectMapper) extends JacksonHttpMessageIO:

  /** Read the contents of a stream and make it available for reading from memory. */
  override def read(in: InputStream, initialCapacity: Int): Supplier[InputStream] =
    new ReadableByteArrayOutputStream(initialCapacity) <| { out =>
      IOUtils.copy(in, out)
    }

  /** Parse a json node. */
  override def parse(in: InputStream): JsonNode =
    mapper.readValue(in, classOf[JsonNode])

  /** Parse a java type. */
  def parse(view: Option[Class[?]], tpe: JavaType, in: InputStream): AnyRef =
    view.fold(mapper.reader)(mapper.readerWithView).forType(tpe).readValue(in)

  /** Serialize an object to JSON. */
  override def serialize(o: AnyRef): Array[Byte] = o match
    // Work around bug in Jackson 2.12.0 where @JsonTypeInfo on an object inside @JsonValue does not get
    // its type info added...
    case oe: ObjectEntity => mapper.writerWithView(classOf[Default]).writeValueAsBytes(oe.getObject)
    case o                => mapper.writerWithView(classOf[Default]).writeValueAsBytes(o)

  /** Serialize an object to JSON node. */
  override def valueToTree(o: AnyRef): JsonNode =
    Using.resource(new TokenBuffer(mapper, false)) { buffer =>
      mapper.writerWithView(classOf[Default]).writeValue(buffer, o)
      mapper.readTree[JsonNode](buffer.asParser)
    }

  /** Write a byte array to a client. */
  override def write(bytes: Array[Byte], request: HttpServletRequest, response: HttpServletResponse): Unit =
    Using.resource(HttpUtils.getOutputStream(request, response))(_.write(bytes))
end JacksonHttpMessageIOImpl

/** A byte array output stream that can efficiently expose its contents for reading. */
class ReadableByteArrayOutputStream(initialCapacity: Int)
    extends ByteArrayOutputStream(initialCapacity)
    with Supplier[InputStream]:
  override def get(): InputStream = new ByteArrayInputStream(buf, 0, count)
