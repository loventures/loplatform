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

import { each, get, isArray, isString } from 'lodash';

import { loConfig } from '../../bootstrap/loConfig.ts';
import { request } from '../../utilities/request.ts';
import { presenceService } from '../../presence/presenceServiceImpl.ts';
import { challengeResponse } from '../challengeResponse.ts';
import { sessionEvents } from '../sessionEvents.ts';

/**
 * Pure-TS port of the AngularJS `SessionService` factory: the session HTTP API (fetch / renew / login /
 * recover / reset / logout / exit) plus the sudo/LTI/iframe predicates. The Angular deps are replaced with
 * pure equivalents — `$http`/`Request` → the native axios `request` (whose `.http(config)` is the `$http`
 * escape hatch through the X-CSRF / X-UserId / 403 interceptors), `$q.reject` → native `Promise.reject`,
 * `PresenceService` → the pure `presenceService` singleton, and the trivial `RedirectService.redirectToLogin`
 * inlined as `goToLogin`. Every endpoint/URL and `sessionEvents` emission is preserved verbatim. The thin
 * Angular `.factory('SessionService', () => sessionApi)` adapter in `SessionService.js` re-exports this.
 */

/** The `RedirectService.redirectToLogin` / `goToLogin` rule, inlined (the only RedirectService use here). */
function goToLogin() {
  const features = window.lo_platform.features;
  if (features && features.LoginPage && features.LoginPage.value != null) {
    window.location.href = features.LoginPage.value;
  } else {
    window.location.href = '';
  }
}

const SessionService: any = {};

// `request.http(config)` is the axios escape hatch with the full `$http(config)` contract (resolves
// `{ data, status, … }`, rejects with `.data` = the body) through the Request instance's interceptors
// (X-CSRF / X-UserId / 403 session guard) — replacing the raw `$http` this service used to inject.
SessionService.fetchStatus = function () {
  return request.http({ url: loConfig.session, method: 'GET', cache: false, background: true });
};

SessionService.renew = function () {
  return request.http({ url: loConfig.noop, method: 'GET', cache: false }).then(function () {
    return SessionService.fetchStatus();
  });
};

/* This hides from the caller of this service the details of the LO standard
 * status code 202 response that conveys user errors and async/challenges.
 */
function dehttp(config: any): any {
  return request.http(config).then(
    function success(o: any) {
      if (o.status == 202) {
        console.log('accepted response: ' + o.data.status);
        if (o.data.status == 'async') {
          // TODO: implement async support
          return Promise.reject(o);
        } else if (o.data.status == 'challenge' && !config.headers['X-Challenge-Response']) {
          challengeResponse(config, o);
          return dehttp(config);
        } else {
          return Promise.reject(o);
        }
      } else {
        return o;
      }
    },
    function failure(o: any) {
      return Promise.reject(o);
    }
  );
}

SessionService.getLoginMechanisms = function () {
  return request.promiseRequest(loConfig.user.loginMechanisms);
};

/**
 *  @params {String} search the key to search and recover account, username, useremail@email.com
 *  @params {String} searchType 'emailAddress' or 'userName'
 *  @returns {Promise} a $q
 */
SessionService.recover = function (search: string, searchType?: string) {
  const params = {
    message: '',
    properties: searchType ? searchType : 'emailAddress',
    redirect: '/#/resetPassword/',
    search: search,
  };
  const headers = {
    Accept: '*/*',
    'Content-Type': 'application/x-www-form-urlencoded',
    'X-Requested-With': 'XMLHttpRequest',
  };
  const conf = {
    url: loConfig.user.recover,
    method: 'POST',
    headers: headers,
    data: SessionService.legacyEncode(params),
  };
  return dehttp(conf);
};

SessionService.validateReset = function (token: string) {
  const conf = {
    headers: {
      Accept: '*/*',
    },
    url: loConfig.user.reset,
    method: 'GET',
    params: {
      token: token,
    },
  };
  return dehttp(conf);
};

SessionService.reset = function (token: string, password: string) {
  const conf = {
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
      Accept: '*/*',
    },
    url: loConfig.user.reset,
    method: 'POST',
    data: SessionService.legacyEncode({
      token: token,
      password: password,
    }),
  };
  return dehttp(conf);
};

SessionService.legacyEncode = function (params: any) {
  let str = ''; //CBLPROD-1781
  each(params, function (v: any, k: string) {
    k = encodeURIComponent(k);
    if (isArray(v)) {
      each(v, function (ventry: any) {
        if (str) {
          str += '&';
        }
        str += k + '=' + encodeURIComponent(ventry);
      });
    } else {
      if (str) {
        str += '&';
      }
      str += k + '=' + encodeURIComponent(v);
    }
  });
  return str;
};

//Server does NOT handle the angular json encoded params so make it happy
//as it also rejects application json logins for some reason...
SessionService.login = function (params: any) {
  const conf = {
    url: loConfig.user.login,
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
      Accept: '*/*',
      'X-Interactive': 'true',
    },
    data: SessionService.legacyEncode(params),
  };
  return dehttp(conf);
};

SessionService.isSudo = function () {
  return window.lo_platform && window.lo_platform.session && window.lo_platform.session.sudoed;
};

SessionService.isLti = function () {
  return get(window, 'lo_platform.session.integrated');
};

SessionService.isIframe = function () {
  try {
    return window.self !== window.top;
  } catch (err) {
    return true;
  }
};

SessionService.logout = function () {
  sessionEvents.emit('logout', undefined);
  presenceService.closePresence();
  return request
    .http({ url: loConfig.user.logout, method: 'POST', data: {} })
    .then((res: any) => res.data);
};

SessionService.exit = function () {
  sessionEvents.emit('exit', undefined);
  presenceService.closePresence();
  return request.http({ url: loConfig.user.exit, method: 'POST' }).then(function (res: any) {
    window.location.href = isString(res.data) ? res.data : '/';
  });
};

SessionService.canLogout = function () {
  // If LTI, only show logout if there's a return URL and not an iframe.
  const showLTI =
    !SessionService.isLti() ||
    (window.lo_platform.session.logoutReturnUrl && !SessionService.isIframe());

  return (
    !SessionService.isSudo() &&
    showLTI &&
    window.lo_platform.user &&
    (window.lo_platform.user as any).status !== 'error'
  );
};

SessionService.logoutAndRedirect = function () {
  SessionService.logout().then(function (redirect: any) {
    if (redirect) {
      window.location.href = redirect;
    } else {
      goToLogin();
    }
  });
};

export const sessionApi = SessionService;

export default sessionApi;
