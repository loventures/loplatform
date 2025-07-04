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

export type Option<A> = A | null | undefined;

export type OptionUnwrapped<A> = A extends Option<infer U> ? U : never;
export type MappedOption<A> = Option<{ [Y in keyof A]: OptionUnwrapped<A[Y]> }>;

type OptionObj<A> = { [key: string]: Option<A> };

export function isPresent<A>(o: Option<A>): o is A {
  return o !== null && typeof o !== 'undefined';
}

function ap<A, B, Z>(fa: Option<A>, fb: Option<B>, g: (a: A, b: B) => Z): Option<Z> {
  if (!isPresent(fa)) {
    return fa;
  } else if (!isPresent(fb)) {
    return fb;
  } else {
    return g(fa, fb);
  }
}

export function isUndefined<A>(o: Option<A>): o is undefined {
  return typeof o !== 'undefined';
}

export function fold<A, Z>(o: Option<A>, z: () => Z, f: (a: A) => Z): Z {
  return isPresent(o) ? f(o) : z();
}

export function orElse<A>(o: Option<A>, def: Option<A>): Option<A> {
  return isPresent(o) ? o : def;
}

export function getOrElse<A>(o: Option<A>, a: A): A {
  return isPresent(o) ? o : a;
}

export function map<A, B>(o: Option<A>, f: (a: A) => B): Option<B> {
  return isPresent(o) ? f(o) : o;
}

export function chain<A, B>(o: Option<A>, f: (a: A) => Option<B>): Option<B> {
  return isPresent(o) ? f(o) : o;
}

export function filter<A>(o: Option<A>, f: (a: A) => boolean): Option<A> {
  return isPresent(o) && f(o) ? o : undefined;
}

export function sequenceObj<A, T extends OptionObj<A>>(fas: T): MappedOption<T> {
  return Object.keys(fas).reduce<Option<any>>(
    (acc, key) => ap(acc, fas[key], (ays, a) => ({ ...ays, [key]: a })),
    {} as Option<any>
  ) as MappedOption<T>;
}

export function sequence<A, B>(fa: Option<A>, fb: Option<B>): Option<[A, B]>;
export function sequence<A, B, C>(fa: Option<A>, fb: Option<B>, fc: Option<C>): Option<[A, B, C]>;
export function sequence<A, B, C, D>(
  fa: Option<A>,
  fb: Option<B>,
  fc: Option<C>,
  fd: Option<D>
): Option<[A, B, C, D]>;
export function sequence<T extends Option<any>[]>(...fas: T): any {
  return fas.reduce<Option<any[]>>((acc, fa) => ap(acc, fa, (ays, a) => [...ays, a]), []);
}
