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

export const setOf = <A>(a: A | A[]): Set<A> => new Set(Array.isArray(a) ? a : [a]);

/** Immutable set add. Returns a new set with [a] added. */
export const setAdd = <A>(as: Set<A>, a: A | A[]): Set<A> => {
  const newAs = new Set(as);
  if (Array.isArray(a)) {
    for (const aa of a) {
      newAs.add(aa);
    }
  } else {
    newAs.add(a);
  }
  return newAs;
};

/** Immutable set remove. Returns a new set with [a] removed. */
export const setRemove = <A>(as: Set<A>, a: A | A[]): Set<A> => {
  const newAs = new Set(as);
  if (Array.isArray(a)) {
    for (const aa of a) {
      newAs.delete(aa);
    }
  } else {
    newAs.delete(a);
  }
  return newAs;
};

/** Immutable set toggle. Returns a new set with [a] differently present. */
export const setToggle = <A>(as: Set<A>, a: A | A[]): Set<A> => {
  if (Array.isArray(a)) {
    return a.some(aa => !as.has(aa)) ? setAdd(as, a) : setRemove(as, a);
  } else {
    return as.has(a) ? setRemove(as, a) : setAdd(as, a);
  }
};

/** Map the values of a set. */
export const mapSet = <A>(as: Set<A>, fa: (a: A) => A) => new Set(Array.from(as).map(fa));
