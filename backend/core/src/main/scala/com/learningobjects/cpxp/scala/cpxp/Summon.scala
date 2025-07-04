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

package com.learningobjects.cpxp.scala.cpxp

import com.learningobjects.cpxp.component.eval.InjectEvaluator

import java.lang.annotation.Annotation

object Summon:

  def summon[A](implicit jlrType: JlrType[A]): A =
    val evalr = new InjectEvaluator
    evalr.init(null, "summon", jlrType.tpe, Array.empty[Annotation])
    evalr.getValue(null, null, null).asInstanceOf[A]

  /*
  import language.experimental.macros

  def summon(what: String): AnyRef = macro Summon.summon_impl

  def summon(what: Symbol): AnyRef = macro Summon.summon_impl

  import reflect.macros._

  def summon_impl(c: whitebox.Context)(what: c.Tree): c.Tree = {
    val c0 = c.asInstanceOf[contexts.Context] // unleash the kraken
    import c0.universe._

    val name = what.asInstanceOf[Tree] match {
      case Literal(Constant(name: String))                  => name
      case Apply(_, Literal(Constant(name: String)) :: Nil) => name // Symbol.apply("name")
      case o                                                => c.abort(what.pos, "expected a literal string" + o.getClass.getName)
    }

    def computeClasspath: Map[String, Symbol] = {
      // loop over packages, collecting classes within them
      def loop(sym: Symbol): List[(String, Symbol)] = {
        if (sym.isPackageClass) {
          val decls          = sym.info.decls.toList                             // the immediate children of this package
          val pkgs           = decls.filter(p => p.hasPackageFlag && p.isModule) // the children that are packages
          val clses          = decls.filter(c => c.isClass && !c.isPackageClass && !c.isModuleClass)
          // index them as FooService and blah.FooService for silly disambiguation
          val asSimpleName   = clses.map(sym => sym.name.toString -> sym)
          val asPrefixedName = clses.map(sym => (sym.owner.name.toString + "." + sym.name) -> sym)
          asSimpleName ::: asPrefixedName ::: pkgs.map(_.asTerm.referenced).flatMap(loop)
        } else Nil
      }
      @nowarn def inPkg(name: String)               = // referenced gives us the package class symbol
        loop(rootMirror.getPackage(name).referenced).toMap
      // these are our packages; summon probably doesn't work with much else
      inPkg("loi") ++ inPkg("com.learningobjects") ++ inPkg("de")
    }

    val classes = computeClasspath

    val selection = classes.getOrElse(
      name, {
        c.abort(c.enclosingPosition, s"can't find summonable $name")
      }
    )

    val SummonMod     = symbolOf[Summon.type]
    val Summon_summon = SummonMod.info
      .member(TermName("summon"))
      .suchThat(_.typeParams.nonEmpty) // select the other summon

    val resJlrTpe = appliedType(symbolOf[JlrType[_]], selection.tpe)
    val resJlrTag = c0.inferImplicitValue(resJlrTpe, silent = false, withMacrosDisabled = false)

    atPos(c0.enclosingPosition) {
      val qual = gen.mkAttributedSelect(gen.mkAttributedIdent(SummonMod), Summon_summon)
      gen.mkMethodCall(Summon_summon, List(selection.tpe), List(resJlrTag))
    }.asInstanceOf[c.Tree]
  }
   */
