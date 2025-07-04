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

import { Option, fromNullable } from 'fp-ts/es6/Option';
import { dropRight, findIndex, last, xor } from 'lodash';

/**
 * Extracts the type of element values from an array
 */
export type ElementValue<T> = T extends Array<infer U> ? U : never;

export function splay<T>(arr: T[]): T[][] {
  return arr.map((_a, i) => arr.slice(0, i + 1));
}

/**
 * Returns a tuple of two lists, where the first list is the set of
 * all items that match the predicate, and the second is the set of
 * all items that _don't_ match the predicate.
 * @param predicate
 */
export function split<T>(predicate: (t: T) => boolean): (arr: T[]) => [T[], T[]] {
  return arr => [arr.filter(predicate), arr.filter(a => !predicate(a))];
}

/**
 * Zips two arrays, resulting in a list of values returned from f
 * like zip, but exhuasts both arrays to their end, providing none
 * if there is no item at each index
 *
 * @param f the function to combine optional a & b values
 *
 * @example
 * zipWithOp([1, 2, 3], [1, 2], (a, b) => sequenceT(option)(a, b))
 */
export function zipWithOp<A, B, C>(fa: A[], fb: B[], f: (a: Option<A>, b: Option<B>) => C): C[] {
  const fc = [];
  const len = Math.max(fa.length, fb.length);
  for (let i = 0; i < len; i++) {
    fc[i] = f(fromNullable(fa[i]), fromNullable(fb[i]));
  }
  return fc;
}

/**
 * Zips two arrays, the resulting array will be the length
 * of the longer array, and none values filled in between
 *
 * @example
 * zipOp(["a", "b"], [1, 2, 3])
 * // results in: [[some("a"), some(1)], [none, some(2)], [none, some(3)]]
 *
 * zipOp(["a", "b", "c"], [1])
 * // results in: [[some("a"), some(1)], [some("b"), none], [some("c"), none]]
 *
 * // also works with sparse arrays:
 * zipOp([,"b"], [,,3])
 * // results in: [[none, none], [some("b"), none], [none, some(3)]]
 */
export const zipOp = <A, B>(fa: A[], fb: B[]): Array<[Option<A>, Option<B>]> =>
  zipWithOp(fa, fb, (a, b) => [a, b] as [Option<A>, Option<B>]);

export function popLast<T>(arr: T[]): [T, T[]] {
  return [last(arr)!, dropRight(arr)]; // eslint-disable-line @typescript-eslint/no-non-null-assertion
}

export function reverse<T>(list: T[]): T[] {
  // JS doesn't support tail recursion.
  // i am disgusted in myself and Javascript.
  let newArr: T[] = [];
  for (let i = 0; i < list.length; i++) {
    newArr = [list[i]].concat(newArr);
  }
  return newArr;
}

/**
 * Returns a new array with the item added at the specified index
 */
export function insert<A>(array: A[], item: A, index: number): A[] {
  return [...array.slice(0, index), item, ...array.slice(index)];
}

/**
 * Returns a new array with the specified index removed
 */
export function remove<A>(array: A[], index: number): A[] {
  return [...array.slice(0, index), ...array.slice(index + 1)];
}

/**
 * Returns a new array with the specified index replaced
 */
export function replace<A>(array: A[], index: number, element: A): A[] {
  return [...array.slice(0, index), element, ...array.slice(index + 1)];
}

/**
 * Returns a new array with the specified element found and replaced
 */
export function findAndReplace<A>(
  array: A[],
  check: (value: A, index: number) => boolean,
  element: A
): A[] {
  const index = findIndex(array, check);
  if (index !== -1) {
    return replace(array, index, element);
  } else {
    return array;
  }
}

/**
 * Returns a new array, additionally with element if not present,
 *   or Returns a new array without element if it is already present.
 * @param array
 * @param element the element to add or remove
 */
export function toggle<A>(array: A[], element: A): A[] {
  return xor(array, [element]);
}

/**
 * Compares two arrays using the supplied equals function. returns true if
 *   the arrays are the same length, and every value in the left array
 *   is found in the right array, using the supplied equality function
 * @param eq function to use to check equality
 */
export function equal<A>(lA: A[], rA: A[], eq: (l: A, r: A) => boolean): boolean {
  if (lA.length === 0 && rA.length === 0) {
    return true;
  } else if (lA.length !== rA.length) {
    return false;
  } else {
    const [l, restL] = popLast(lA);
    const i = rA.findIndex(r => eq(l, r));
    if (i === -1) {
      return false;
    } else {
      return equal(restL, remove(rA, i), eq);
    }
  }
}

export function equalWithOrder<A>(lA: A[], rA: A[], eq: (l: A, r: A) => boolean): boolean {
  if (lA.length === 0 && rA.length === 0) {
    return true;
  } else if (lA.length !== rA.length) {
    return false;
  } else {
    return lA.every((a, i) => {
      return eq(a, rA[i]);
    });
  }
}

export function move<T>(arr: T[], oldIndex: number, newIndex: number): T[] {
  const before = arr.slice(0, oldIndex);
  const [toMove, ...after] = arr.slice(oldIndex);
  const without = [...before, ...after];
  return insert(without, toMove, newIndex);
}
