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

import { ErrorResponseBody } from '../error/restErrorsTypes.ts';

export function isLoading<T>(l: Loadable<T>): l is Loading<T> {
  return l._tag === 'loading';
}

export function isErrored<T>(l: Loadable<T>): l is Errored<T> {
  return l._tag === 'errored';
}

export function isLoaded<T>(l: Loadable<T>): l is Loaded<T> {
  return l._tag === 'loaded';
}

export type Loadable<A> = Loading<A> | Errored<A> | Loaded<A>;

export class Loading<A> {
  _tag = 'loading' as const;
  static value: Loading<never> = new Loading();

  map<B>(_f: (a: A) => B): Loadable<B> {
    return this as any as Loading<B>;
  }

  ap<B>(_fab: Loadable<(a: A) => B>): Loadable<B> {
    return this as any as Loading<B>;
  }

  chain<B>(_f: (a: A) => Loadable<B>): Loadable<B> {
    return this as any as Loading<B>;
  }

  getOrThrow(): A {
    throw new Error("Tried to get loadable's data, but the data was still loading.");
  }
}

class Errored<A> {
  _tag = 'errored' as const;

  constructor(readonly error: ErrorResponseBody) {
    this.error = error;
  }

  map<B>(_f: (a: A) => B): Loadable<B> {
    return new Errored(this.error);
  }

  ap<B>(_fab: Loadable<(a: A) => B>): Loadable<B> {
    return new Errored(this.error);
  }

  chain<B>(_f: (a: A) => Loadable<B>): Loadable<B> {
    return new Errored(this.error);
  }

  getOrThrow(): A {
    throw new Error("Tried to get loadable's data, but the loadable errored.");
  }
}

export class Loaded<A> {
  _tag = 'loaded' as const;

  constructor(readonly data: A) {
    this.data = data;
  }

  map<B>(f: (a: A) => B): Loadable<B> {
    return new Loaded(f(this.data));
  }

  ap<B>(fab: Loadable<(a: A) => B>): Loadable<B> {
    if (isLoading(fab)) {
      return Loading.value;
    } else if (isErrored(fab)) {
      return new Errored(fab.error);
    } else {
      return new Loaded(fab.data(this.data));
    }
  }

  chain<B>(f: (a: A) => Loadable<B>): Loadable<B> {
    return f(this.data);
  }

  getOrThrow(): A {
    return this.data;
  }
}

export const loading: Loading<never> = Loading.value;
export const errored: <T>(errorMessage: any) => Loadable<T> = msg => new Errored(msg);
export const loaded: <T>(data: T) => Loadable<T> = data => new Loaded(data);
export const of = loaded;

type LoadingUnwrapped<T> = T extends Loadable<infer U> ? U : never;
type MappedLoading<T> = Loadable<{ [Y in keyof T]: LoadingUnwrapped<T[Y]> }>;

type LoadableObj<A> = { [key: string]: Loadable<A> };

export function sequenceObj<A, T extends LoadableObj<A>>(fas: T): MappedLoading<T> {
  return Object.keys(fas).reduce(
    (acc, l) => acc.ap(fas[l].map(a => (ays: any) => ({ ...ays, [l]: a }))),
    loaded<{ [key: string]: A }>({})
  ) as MappedLoading<T>;
}

export function sequence<A, T extends Loadable<A>[]>(fas: T): MappedLoading<T> {
  return fas.reduce<Loadable<A[]>>(
    (acc, l) => acc.ap(l.map(a => (ays: any) => [...ays, a])),
    loaded<A[]>([])
  ) as MappedLoading<T>;
}
