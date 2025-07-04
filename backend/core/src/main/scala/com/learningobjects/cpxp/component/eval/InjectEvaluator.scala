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

package com.learningobjects.cpxp.component.eval

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.component.internal.DelegateDescriptor
import com.learningobjects.cpxp.component.{ComponentInstance, ComponentInterface, ComponentSupport}
import com.learningobjects.cpxp.service.ServiceContext
import com.learningobjects.cpxp.util.lang.{OptionLike, ProviderLike}
import org.apache.commons.lang3.StringUtils
import scaloi.syntax.AnyOps.*

import java.lang.annotation.Annotation
import java.lang.reflect.{Field, Type}
import java.util.concurrent.ConcurrentHashMap
import javax.ejb.Local
import javax.inject.{Inject, Named}
import scala.annotation.nowarn
import scala.collection.concurrent
import scala.compat.java8.OptionConverters.*
import scala.jdk.CollectionConverters.*

/** Evaluate fields and parameters annotated with JSR-330 annotations. This implementation looks up values by name in
  * JNDI, and a NameRegistry if annotated with @Named, then infers a suitable value by its type.
  */
@Evaluates(Array(classOf[Inject]))
class InjectEvaluator extends AbstractEvaluator:
  import InjectEvaluator.*

  private var lookerUpper: LookerUpper = scala.compiletime.uninitialized
  private var stateless: Boolean       = scala.compiletime.uninitialized

  override def init(delegate: DelegateDescriptor, name: String, tpe: Type, annotations: Array[Annotation]): Unit =
    super.init(delegate, name, tpe, annotations)
    val info = TypeInfo(tpe, AnnotationInfo(annotations))
    lookerUpper = mkLookUp(info)
    stateless = isStateless(info)

  private def mkLookUp(info: TypeInfo): LookerUpper =
    val raw = info.raw.asInstanceOf[Class[AnyRef]]
    if OptionLike.isOptionLike(raw) then
      val wrapped = mkLookUp(info.unwrapped)
      (ci: ComponentInstance, ref: AnyRef) => OptionLike.ofNullable(raw, wrapped(ci, ref))
    else if ProviderLike.isProviderLike(raw) then
      val wrapped = mkLookUp(info.unwrapped)
      (ci: ComponentInstance, ref: AnyRef) => ProviderLike.wrapLazy(raw)(wrapped(ci, ref))
    else if info.ann.contains(classOf[Named]) then
      val name = info.ann.get[Named].get.value
      (_: ComponentInstance, _: AnyRef) => registry.getOrElse(name, null)
    else if raw.isAnnotationPresent(classOf[Local]) then
      (_: ComponentInstance, _: AnyRef) => ServiceContext.getContext.getService(raw): @nowarn
    else if raw.isAnnotationPresent(classOf[Service]) then
      (_: ComponentInstance, _: AnyRef) => ComponentSupport.lookupService(raw)
    else if classOf[ComponentInterface].isAssignableFrom(raw) then
      // +1 super dodgy because this is hard to distinguish from Instance evaluation
      (_: ComponentInstance, _: AnyRef) => ComponentSupport.get(raw.asSubclass(classOf[ComponentInterface]))
    else
      val ie = new InferEvaluator <| (_.init(_delegate, _name, info.tpe, info.annArray))
      (ci: ComponentInstance, ref: AnyRef) => ie.getValue(ci, ref)
    end if
  end mkLookUp

  private def isStateless(info: TypeInfo): Boolean =
    val raw = info.raw.asInstanceOf[Class[AnyRef]]
    if OptionLike.isOptionLike(raw) then isStateless(info.unwrapped)
    else
      ProviderLike.isProviderLike(raw) ||
      info.ann.contains(classOf[Named]) ||
      raw.isAnnotationPresent(classOf[Local]) ||
      raw.isAnnotationPresent(classOf[Service]) ||
      InferEvaluator.isStatelessInferable(info.tpe).asScala.forall(_.booleanValue)

  override protected def getValue(instance: ComponentInstance, ref: AnyRef): AnyRef =
    lookerUpper(instance, ref)

  override def isStateless: Boolean = stateless
end InjectEvaluator
object InjectEvaluator:
  type LookerUpper = (ComponentInstance, AnyRef) => AnyRef

  def lookupField(field: Field): AnyRef =
    val injector = new InjectEvaluator
    injector.init(null, StringUtils.stripStart(field.getName, "_"), field.getGenericType, field.getAnnotations)
    injector.getValue(null, null)

  val registry: concurrent.Map[String, AnyRef] = new ConcurrentHashMap[String, AnyRef]().asScala
