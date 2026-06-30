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

import axios from 'axios';
import cookies from 'browser-cookies';

//since our app is always a fresh load after login, this has no stale issue
axios.defaults.headers.post['X-CSRF'] = cookies.get('CSRF');
axios.defaults.headers.put['X-CSRF'] = cookies.get('CSRF');
// DELETE needs X-CSRF too — the server 403s state-changing requests without it. The old Angular `Request.js`
// set it on EVERY method; global axios only had post/put, so native DELETEs (e.g. enrolledUserService.dropUsers)
// were unprotected. (No native DELETE existed until the bulkActions drop was drained off the Angular Request.)
axios.defaults.headers.delete['X-CSRF'] = cookies.get('CSRF');
