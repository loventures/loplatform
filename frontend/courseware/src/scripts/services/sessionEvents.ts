/*
 * LO Platform copyright (C) 2007–2026 LO Ventures LLC.
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

/**
 * A tiny typed pub-sub for the session lifecycle events that used to ride `$rootScope.$emit/$on`
 * (`SessionService.logout` / `.exit` / `.expired` + the internal `DoSessionListenerCheck`). The handlers
 * are plain side effects (SCORM postMessage, presence teardown, the keepalive 403 re-check) that never
 * needed the Angular digest, so a plain emitter is a faithful, framework-free replacement.
 */
export interface SessionEventMap {
  logout: void;
  exit: void;
  expired: void;
}

type Handler<E extends keyof SessionEventMap> = (data: SessionEventMap[E]) => void;
type AnyHandler = (data: any) => void;

// Internally untyped (one Set of handlers per event name); on/emit are typed at the boundary.
const listeners = new Map<keyof SessionEventMap, Set<AnyHandler>>();

export const sessionEvents = {
  /** Subscribe; returns an unsubscribe function (mirrors the `$rootScope.$on` deregistration handle). */
  on<E extends keyof SessionEventMap>(event: E, handler: Handler<E>): () => void {
    let set = listeners.get(event);
    if (!set) listeners.set(event, (set = new Set<AnyHandler>()));
    set.add(handler as AnyHandler);
    return () => set!.delete(handler as AnyHandler);
  },

  emit<E extends keyof SessionEventMap>(event: E, data: SessionEventMap[E]): void {
    listeners.get(event)?.forEach(handler => handler(data));
  },
};
