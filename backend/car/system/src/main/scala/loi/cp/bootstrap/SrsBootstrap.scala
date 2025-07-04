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

package loi.cp.bootstrap

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.learningobjects.cpxp.component.*
import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.registry.SchemaRegistry
import com.learningobjects.cpxp.component.web.{ApiRootComponent, WebRequest}
import com.learningobjects.cpxp.util.ClassUtils

import java.lang.Long as JLong
import java.lang.annotation.Annotation
import scala.reflect.ClassTag

/** This phase allows you to invoke SRS endpoints during the bootstrap process.
  *
  * Typical usage: <pre> { "phase": "POST /api/v2/taxonomies", "config": { "body": { "name": "New Taxomoy" } } } </pre>
  *
  * The payload o a POST should be included as the "body" attribute of the phase configuration. The schema, if any,
  * should be included as the "schema" attribute.
  *
  * If an SRS endpoint returns an entity, the PK of that entity may be used as the path variable of a nested phase by
  * the use of the token _: <pre> { "phase": "POST /api/v2/taxonomies", "config": { "body": { "name": "New Taxomoy" } }
  * "setup": [ { "phase": "GET /api/v2/taxonomies/_", "config": { } } ] } </pre>
  */
@Component
class SrsBootstrap(
  val componentInstance: ComponentInstance,
  @Init request: WebRequest
) extends ComponentInterface
    with ComponentImplementation:
  @Bootstrap("srs")
  def srsBootstrap(context: JLong, srs: Srs): AnyRef =
    // This is a relatively crude implementation of enough of the behaviour of ApiDispatcherServlet
    // to invoke basic post endpoints
    val apiRoot              = ComponentSupport
      .lookup(classOf[ApiRootComponent], request.getPath, request.getMethod, srs.schema.orNull)
      .getComponentInstance
    val classMapping         = Option(apiRoot.getComponent.getAnnotation(classOf[RequestMapping]))
    val rmi                  = apiRoot.getFunctionInstance(
      classOf[RequestMappingInstance],
      request.getMethod,
      request.getDePathSegments,
      srs.schema.orNull
    )
    val method               = rmi.getFunction.getMethod
    val parameterAnnotations = method.getParameterAnnotations
    val parameterTypes       = method.getGenericParameterTypes
    val mapper               = ComponentSupport.getObjectMapper
    val args                 = (0 until method.getParameterCount) map { i =>
      parameterAnnotations(i) match
        case RequestBodyAnnotation(_) =>
          for body <- srs.body
          yield
            val javaType = srs.schema.fold(mapper.getTypeFactory.constructType(parameterTypes(i))) { schema =>
              val reg = ComponentSupport.lookupResource(classOf[Schema], classOf[SchemaRegistry.Registration], schema)
              mapper.getTypeFactory.constructType(reg.getSchemaClass)
            }
            mapper.readValue[AnyRef](mapper.treeAsTokens(body), javaType)

        case PathVariableAnnotation(p) =>
          if !classOf[JLong].equals(parameterTypes(i)) && !classOf[Long]
              .equals(parameterTypes(i))
          then throw new RuntimeException(s"Unsupported path variable parameter: $p")
          Option(context) // only support the context as a path variable

        case annotations =>
          val annotationTypes =
            annotations.map(_.annotationType.getSimpleName).mkString
          throw new RuntimeException(s"Unsupported parameter annotations: $annotationTypes")
    }
    method.invoke(rmi.getObject, args.flatten*)
  end srsBootstrap
end SrsBootstrap

case class Srs(
  schema: Option[String],
  body: Option[JsonNode],
  parameters: Option[ObjectNode]
)

class AnnotationMatcher[T <: Annotation](implicit tt: ClassTag[T]):
  def unapply(annotations: Array[Annotation]): Option[T] =
    Option(ClassUtils.getAnnotation(tt.runtimeClass.asInstanceOf[Class[T]], annotations))

object RequestBodyAnnotation extends AnnotationMatcher[RequestBody]

object PathVariableAnnotation extends AnnotationMatcher[PathVariable]
