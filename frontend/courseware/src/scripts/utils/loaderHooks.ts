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

import { Loadable, errored, isLoaded, loaded, loading } from '../types/loadable';
import { useEffect, useMemo, useState } from 'react';

type LoaderFn<TData> = () => Promise<TData>;
type LoadableReload<TData> = (l: LoaderFn<TData>) => void;
export type LoadableSetDataWithCB<TData> = (cb: (d: TData) => TData) => void;

type LoadableTuple<TData> = [Loadable<TData>, LoadableReload<TData>, LoadableSetDataWithCB<TData>];

const useLoadableLoader = <TData>(setState: any) => {
  return useMemo(() => {
    return (loader: LoaderFn<TData>) => {
      setState(loading);
      loader()
        .then(data => setState(loaded(data)))
        .catch(error => setState(errored(error)));
    };
  }, [setState]);
};

const useLoadableInitialLoad = (state: any, doLoad: any) => {
  useEffect(() => {
    //null means not loaded
    if (state === null) {
      doLoad();
    }
  });
};

const useSetDataWithCB = (state: any, setState: any) => {
  return useMemo(() => {
    return (callback: any) => {
      if (isLoaded(state)) {
        const newData = callback(state.data);
        return setState(loaded(newData));
      }
    };
  }, [state, setState]);
};

export const useLoadable = <TData>(loader: LoaderFn<TData>): LoadableTuple<TData> => {
  const [state, setState] = useState<Loadable<TData> | null>(null);

  const doLoad = useLoadableLoader<TData>(setState);
  const setDataWithCB = useSetDataWithCB(state, setState);
  useLoadableInitialLoad(state, () => doLoad(loader));

  return [state || loading, doLoad, setDataWithCB];
};

export const useKeyedLoadable = <TData>(
  key: string | number | undefined,
  loader: { (): Promise<TData> }
): LoadableTuple<TData> => {
  const [stateMap, setStateMap] = useState<{ [k: string]: Loadable<TData> }>({});

  const state: Loadable<TData> | null = (key !== undefined && stateMap[key]) || null;
  const setState =
    key !== undefined
      ? (newState: Loadable<TData>) =>
          setStateMap({
            ...stateMap,
            [key]: newState,
          })
      : // eslint-disable-next-line @typescript-eslint/no-empty-function
        () => {};

  const doLoad = useLoadableLoader<TData>(setState);
  const setDataWithCB = useSetDataWithCB(state, setState);
  useLoadableInitialLoad(state, () => doLoad(loader));

  return [state || loading, doLoad, setDataWithCB];
};
