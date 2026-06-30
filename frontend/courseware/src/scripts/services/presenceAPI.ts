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

import { makePresenceAPI } from './pure/presenceAPI.ts';
import { request } from '../utilities/request.ts';

/**
 * Native (axios) PresenceAPI singleton for the React/redux presence subsystem —
 * replaces `lojector.get('PresenceAPI')`.
 *
 * The pure factory needs an `$http`-shaped client (callable + `.post`/`.put`/`.delete`)
 * and a `delay(ms): Promise`. The native `request.http(config)` carries the same
 * interceptors (X-CSRF / X-UserId / 403 guard) as the Angular `Request` instance but
 * resolves via native Promises — no digest, which the redux-driven presence UI does
 * not need. `delay` becomes a native `setTimeout`-backed promise (was `$timeout(ms)`).
 */
const http: any = (config: any) => request.http(config);
http.post = (url: string, data: any, config: any) => request.http({ ...config, url, method: 'POST', data });
http.put = (url: string, data: any, config: any) => request.http({ ...config, url, method: 'PUT', data });
http.delete = (url: string, config: any) => request.http({ ...config, url, method: 'DELETE' });

const delay = (ms: number) => new Promise(resolve => setTimeout(resolve, ms));

export const presenceAPI = makePresenceAPI(http, delay);
