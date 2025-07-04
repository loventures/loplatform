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

package com.learningobjects.cpxp.dto

import com.learningobjects.cpxp.service.finder.Finder
import com.learningobjects.cpxp.service.item.Item
import com.learningobjects.cpxp.util.entity.FinderManipulator
import net.bytebuddy.description.`type`.TypeDescription
import net.bytebuddy.dynamic.loading.ClassInjector
import org.objectweb.asm.*
import org.objectweb.asm.util.TraceClassVisitor
import scalaz.syntax.std.boolean.*
import scalaz.syntax.std.map.*

import java.lang.invoke.MethodHandles
import java.lang.reflect as jlr
import scala.jdk.CollectionConverters.*

object ManipulatorSynthesis:
  import Opcodes.*

  val lookup = MethodHandles.lookup()

  def spin(finder: Class[? <: Finder]): FinderManipulator =
    val manipInternalName          = Type.getInternalName(finder) + "_Manipulator"
    val manipName                  = finder.getName + "_Manipulator"
    val properties: List[PropInfo] =
      finder.getMethods.toList.collect { case ScalaSetter(m) =>
        PropInfo(
          name = m.getName.stripSuffix(SetterSuffix),
          tpe = Type.getArgumentTypes(m).head,
          isFinder = classOf[Finder] `isAssignableFrom` m.getParameterTypes.apply(0),
        )
      }

    val writer0 = new ClassWriter(ASM7 | ClassWriter.COMPUTE_FRAMES)
    val writer  =
      if sys.props contains "com.learningobjects.cpxp.dto.ManipulatorSynthesis.trace" then
        new TraceClassVisitor(writer0, new java.io.PrintWriter(System.out))
      else writer0
    writer.visit(V1_8, ClassFlags, manipInternalName, NoSig, Object, Array(FinderManipulator))

    spinGetter(Type.getType(finder), properties) {
      writer.visitMethod(ACC_PUBLIC, GetterName, GetterDesc, NoSig, NoExns)
    }
    spinSetter(Type.getType(finder), properties) {
      writer.visitMethod(ACC_PUBLIC, SetterName, SetterDesc, NoSig, NoExns)
    }
    val ctor = writer.visitMethod(ACC_PUBLIC, "<init>", "()V", NoSig, NoExns)
    ctor.visitCode()
    ctor.visitVarInsn(ALOAD, 0)
    ctor.visitMethodInsn(INVOKESPECIAL, Object, "<init>", "()V", false)
    ctor.visitInsn(RETURN)
    ctor.visitMaxs(-1, -1)
    ctor.visitEnd()

    writer.visitEnd()

    // Here we need to use different class injection strategies for different JDKs(8 and 11+)
    // while still being able to compile from either JDK
    // So we test to see if it's possible to use MethodHandle Lookups on JDK 11, then reflectively call
    // privateLookupIn to get a privileged lookup that can define manipulators in the finder's package.
    // If on JDK 8, we use sun.misc.Unsafe to define the class.
    val injector              = if ClassInjector.UsingLookup.isAvailable() then
      val methodHandles   = Class.forName("java.lang.invoke.MethodHandles")
      val privateLookupIn = methodHandles.getMethod(
        "privateLookupIn",
        classOf[Class[?]],
        Class.forName("java.lang.invoke.MethodHandles$Lookup")
      )
      val privateLookup   = privateLookupIn.invoke(null, finder, lookup)
      ClassInjector.UsingLookup.of(privateLookup)
    else new ClassInjector.UsingUnsafe(finder.getClassLoader)
    val manipulatorDescriptor = TypeDescription.ForLoadedType.of(classOf[FinderManipulator])
    val description           = new TypeDescription.AbstractBase.OfSimpleType.WithDelegation:
      override def delegate(): TypeDescription = manipulatorDescriptor
      override def getName: String             = manipName
    val newClazzes            = injector.inject(Map(description -> writer0.toByteArray).asJava)
    val (_, newClazz)         = newClazzes.asScala.head
    newClazz
      .getDeclaredConstructor()
      .newInstance()
      .asInstanceOf[FinderManipulator]
  end spin

  final val ItemParam = 1; final val PropParam = 2; final val ValueParam = 3

  def spinGetter(finder: Type, props: List[PropInfo])(mv: MethodVisitor): Unit =
    spinPropLookup(mv, finder, props) { case PropInfo(name, tpe, isFinder) =>
      val descr = "()" + tpe.getDescriptor
      mv.visitMethodInsn(INVOKEVIRTUAL, finder.getInternalName, name, descr, false)
      if isFinder then definder(mv)
      else spinBoxingConversion(mv, tpe)
      mv.visitInsn(ARETURN)
    }

    mv.visitInsn(ACONST_NULL) // unreachable
    mv.visitInsn(ARETURN)
    mv.visitMaxs(-1, -1)
    mv.visitEnd()
  end spinGetter

  def spinSetter(finder: Type, props: List[PropInfo])(mv: MethodVisitor): Unit =
    spinPropLookup(mv, finder, props) { case PropInfo(name, tpe, isFinder) =>
      mv.visitVarInsn(ALOAD, ValueParam)
      if isFinder then refinder(mv, tpe)
      else spinUnboxingConversion(mv, tpe)
      val descr = s"(${tpe.getDescriptor})V"
      mv.visitMethodInsn(INVOKEVIRTUAL, finder.getInternalName, name + "_$eq", descr, false)
      mv.visitInsn(RETURN)
    }

    mv.visitInsn(RETURN)
    mv.visitMaxs(-1, -1)
    mv.visitEnd()
  end spinSetter

  def spinPropLookup(mv: MethodVisitor, finder: Type, props: List[PropInfo])(
    body: PropInfo => Unit
  ): Unit =

    mv.visitCode()
    mv.visitVarInsn(ALOAD, PropParam)
    mv.visitMethodInsn(INVOKEVIRTUAL, Object, HashCodeName, HashCodeDesc, false)

    val hashed  = props.groupBy(_.name.##).mapKeys((_, new Label)).toList.sortBy(_._1._1)
    val hashes  = hashed.map(_._1._1)
    val labels  = hashed.map(_._1._2)
    val default = new Label

    mv.visitLookupSwitchInsn(default, hashes.toArray, labels.toArray)

    hashed.foreach { case ((_, label), cases) =>
      mv.visitLabel(label)
      var nextLabel = new Label
      cases.foreach { case pi @ PropInfo(prop, _, _) =>
        mv.visitVarInsn(ALOAD, PropParam)
        mv.visitLdcInsn(prop)
        mv.visitMethodInsn(INVOKEVIRTUAL, Object, EqualsName, EqualsDesc, false)
        mv.visitJumpInsn(IFEQ, nextLabel)
        mv.visitVarInsn(ALOAD, ItemParam)
        mv.visitTypeInsn(CHECKCAST, finder.getInternalName)
        body(pi)
        mv.visitLabel(nextLabel)
        nextLabel = new Label
      }
      mv.visitJumpInsn(GOTO, default)
    }

    mv.visitLabel(default)
    mv.visitVarInsn(ALOAD, PropParam)
    mv.visitVarInsn(ALOAD, ItemParam)
    mv.visitMethodInsn(INVOKESTATIC, FinderUtil, FailName, FailDesc, false)
  end spinPropLookup

  def definder(mv: MethodVisitor): Unit = // ensure we always return an Item
    val done = new Label

    mv.visitInsn(DUP)
    mv.visitTypeInsn(INSTANCEOF, Finder)
    mv.visitJumpInsn(IFEQ, done)
    // if we have a finder, get its owner
    mv.visitTypeInsn(CHECKCAST, Finder)
    mv.visitMethodInsn(INVOKEINTERFACE, Finder, GetOwnerName, GetOwnerDesc, true)
    mv.visitJumpInsn(GOTO, done)
    mv.visitLabel(done)
  end definder

  def refinder(mv: MethodVisitor, tpe: Type): Unit = // we always get an Item; convert to finder if needed
    val done = new Label

    mv.visitInsn(DUP)
    mv.visitTypeInsn(INSTANCEOF, Item)
    mv.visitJumpInsn(IFEQ, done)
    mv.visitTypeInsn(CHECKCAST, Item)
    mv.visitMethodInsn(INVOKEVIRTUAL, Item, GetFinderName, GetFinderDesc, false)
    mv.visitJumpInsn(GOTO, done)
    mv.visitLabel(done)
    mv.visitTypeInsn(CHECKCAST, tpe.getInternalName)
  end refinder

  def spinBoxingConversion(mv: MethodVisitor, tpe: Type): Unit =
    val owner = tpe.getDescriptor match
      case "Z" => "java/lang/Boolean"
      case "S" => "java/lang/Short"
      case "I" => "java/lang/Integer"
      case "J" => "java/lang/Long"
      case "F" => "java/lang/Float"
      case "D" => "java/lang/Double"
      case "C" => "java/lang/Character"
      case _   =>
        mv.visitTypeInsn(CHECKCAST, tpe.getInternalName)
        return
    val desc  = s"(${tpe.getDescriptor})L$owner;"
    mv.visitMethodInsn(INVOKESTATIC, owner, "valueOf", desc, false)
  end spinBoxingConversion

  def spinUnboxingConversion(mv: MethodVisitor, tpe: Type): Unit =
    val (owner, name) = tpe.getDescriptor match
      case "Z" => "java/lang/Boolean"   -> "booleanValue"
      case "S" => "java/lang/Short"     -> "shortValue"
      case "I" => "java/lang/Integer"   -> "intValue"
      case "J" => "java/lang/Long"      -> "longValue"
      case "F" => "java/lang/Float"     -> "floatValue"
      case "D" => "java/lang/Double"    -> "doubleValue"
      case "C" => "java/lang/Character" -> "charValue"
      case _   =>
        mv.visitTypeInsn(CHECKCAST, tpe.getInternalName)
        return
    val desc          = s"()${tpe.getDescriptor}"
    mv.visitTypeInsn(CHECKCAST, owner)
    mv.visitMethodInsn(INVOKEVIRTUAL, owner, name, desc, false)
  end spinUnboxingConversion

  final val Item              = Type.getInternalName(classOf[Item])
  final val Finder            = Type.getInternalName(classOf[Finder])
  final val FinderUtil        = Type.getInternalName(classOf[FinderUtil])
  final val FinderManipulator = Type.getInternalName(classOf[FinderManipulator]) // in case it moves
  final val Object            = "java/lang/Object"
  final val String            = "java/lang/String"

  final val SetterSuffix  = reflect.NameTransformer.SETTER_SUFFIX_STRING
  final val ClassFlags    = ACC_PUBLIC | ACC_FINAL | ACC_SYNTHETIC
  final val GetterName    = "get"
  final val GetterDesc    = s"(L$Object;L$String;)L$Object;"
  final val SetterName    = "set"
  final val SetterDesc    = s"(L$Object;L$String;L$Object;)V"
  final val NoSig         = null: String
  final val NoExns        = Array.empty[String]
  final val EqualsName    = "equals"
  final val EqualsDesc    = s"(L$Object;)Z"
  final val HashCodeName  = "hashCode"
  final val HashCodeDesc  = "()I"
  final val GetFinderName = "getFinder"
  final val GetFinderDesc = s"()L$Finder;"
  final val GetOwnerName  = "getOwner"
  final val GetOwnerDesc  = s"()L$Item;"
  final val FailName      = "fail"
  final val FailDesc      = s"(L$String;L$Object;)V"

  private object ScalaSetter:
    def unapply(m: jlr.Method): Option[jlr.Method] =
      (m.getName.endsWith(SetterSuffix)
        && m.getParameterCount == 1
        && m.getReturnType == Void.TYPE) option m

  final case class PropInfo(name: String, tpe: Type, isFinder: Boolean)
end ManipulatorSynthesis
