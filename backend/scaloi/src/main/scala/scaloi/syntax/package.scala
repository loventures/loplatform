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

package scaloi

import scalaz.std.OptionInstances

import scala.util.{Success, Try}

package object syntax:

  /** The partial function that takes any value to `()`.
    */
  private[syntax] val constUnit: PartialFunction[Any, Unit] = { case _ => () }

  /** A successful [[Try]] containing no meaningful value.
    */
  private[syntax] val successUnit: Try[Unit] = Success(())

  object align          extends ToAlignOps
  object annotation     extends ToAnnotationOps
  object any            extends ToAnyOps
  object boolean        extends ToBooleanOps
  object box            extends ToBoxOps
  object claѕѕ          extends ToClassOps
  object `class`        extends ToClassOps
  object classTag       extends ToClassTagOps with ClassTagFns
  object cobind         extends ToCobindOps
  object boxes          extends ToCollectionBoxOps with OptionInstances
  object collection     extends ToCollectionOps
  object date           extends ToDateOps with DateInstances
  object ⋁              extends ToDisjunctionOps with DisjunctionFns
  object disjunction    extends ToDisjunctionOps with DisjunctionFns
  object double         extends ToDoubleOps with DoubleVals
  object eStream        extends ToEStreamOps
  object either         extends ToEitherOps
  object entry          extends ToEntryOps
  object enumeratum     extends ToEnumEntryOps
  object finiteDuration extends ToFiniteDurationOps
  object fauxnad        extends ToBifauxnadOps:

    /** Considered dangerous because may fauxnad things you might expect to support filtering. */
    object dangerous extends ToFauxnadAnyOps with ToBifauxnadAnyOps
  object foldable        extends ToFoldableOps
  object functor         extends ToFunctorOps
  object hypermonad      extends ToHypermonadOps
  object instant         extends ToInstantOps
  object int             extends ToIntOps
  object jEnum           extends ToJEnumOps
  object listTree        extends ToListTreeOps
  object localDateTime   extends ToLocalDateTimeOps
  object lock            extends ToLockOps
  object map             extends ToMapOps
  object monad           extends ToMonadOps with ToFunctorOps
  object monadPlus       extends ToMonadPlusOps with ToMonadOps with ToFunctorOps
  object monoid          extends ToMonoidOps
  object mutableMap      extends ToMutableMapOps
  object option          extends ToOptionOps
  object partialFunction extends ToPartialFunctionOps
  object =∂>             extends ToPartialFunctionOps
  object putty           extends PuttyOps
  object readWriteLock   extends ToReadWriteLockOps
  object regex           extends ToRegexOps
  object seq             extends ToSeqOps
  object set             extends ToSetOps
  object string          extends ToStringOps
  object strictTree      extends ToStrictTreeOps
  object tree            extends ToTreeOps
  object `try`           extends ToTryOps with ToTryAnyOps with ToTryCompanionOps
  object ʈry             extends ToTryOps with ToTryAnyOps with ToTryCompanionOps
  object validation      extends ToValidationOps
  object vectorMap       extends ToVectorMapOps
  object zero            extends ToZeroOps
end syntax
