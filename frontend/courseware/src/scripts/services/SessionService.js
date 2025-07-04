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

import { loConfig } from '../bootstrap/loConfig.js';
import dayjs from 'dayjs';
import { each, get, isArray, isFunction, isNumber, map, min } from 'lodash';
import RedirectService from '../utilities/RedirectService.js';
import { rstr2b64, rstr_sha1, str2rstr_utf8 } from '../utilities/sha1.js';

import errorModal from '../modals/errorModal/errorModal.html';
import idleModal from '../modals/idleModal/idleModal.html';

export default angular
  .module('lo.services.SessionService', [RedirectService.name])
  .factory('IdleModal', [
    '$uibModal',
    '$rootScope',
    '$q',
    /**
     * @ngdoc service
     * @alias IdleModal
     * @memberof lo.services
     * @description compose and opens a modal to warn about session expiry
     */
    function IdleModal($uibModal, $rootScope, $q) {
      /** @alias IdleModal **/
      var IdleModal = {
        modalIsOpen: false,
      };

      IdleModal.open = function (expiry) {
        if (IdleModal.modalIsOpen) {
          return $q.reject();
        }

        IdleModal.modalIsOpen = true;

        var modalConfig = {
          backdrop: true,
          keyboard: true,
          backdropClick: true,
          template: idleModal,
          controller: 'IdleModalCtrl',
        };

        modalConfig.scope = $rootScope.$new();
        modalConfig.scope.expiry = expiry;

        return $uibModal.open(modalConfig).result;
      };

      IdleModal.loggedOutError = function (logoutAction) {
        var s = $rootScope.$new();
        s.error = {
          title: 'LogoutErrorModal',
          message: 'LogoutErrorModalReason',
        };
        var modalConfig = {
          template: errorModal,
          scope: s,
        };

        var modal = $uibModal.open(modalConfig);
        s.cancel = function () {
          modal.close();
          IdleModal.modalIsOpen = false;
        };
        s.ok = function () {
          logoutAction();
          modal.close();
          IdleModal.modalIsOpen = false;
        };
        return modal;
      };

      return IdleModal;
    },
  ])
  /**
   * @ngdoc service
   * @alias ChallengeResponseCreator
   * @memberof lo.services
   * @description use de.js and sha1.js to create challenge response in header
   */
  .factory('ChallengeResponseCreator', function () {
    return function (config, o) {
      // TODO: I ought to clone config and headers but let's just side-effect for now
      try {
        //THIS comes from a mix of de.js and sha1.js
        config.headers['X-Challenge-Response'] = rstr2b64(
          rstr_sha1(str2rstr_utf8(o.data.challenge))
        );
      } catch (e) {
        console.error('Cannot create challenge response');
      }

      return config;
    };
  })
  /**
   * @ngdoc service
   * @alias SessionService
   * @memberof lo.services
   * @description API service to fetch, renew and destroy session state
   */
  .factory('SessionService', [
    'RedirectService',
    '$q',
    '$rootScope',
    '$http',
    'Request',
    'PresenceService',
    function SessionService(RedirectService, $q, $rootScope, $http, Request, PresenceService) {
      /** @alias SessionService **/
      var SessionService = {};

      SessionService.fetchStatus = function () {
        return $http.get(loConfig.session, { cache: false, background: true });
      };

      SessionService.renew = function () {
        return $http.get(loConfig.noop, { cache: false }).then(function () {
          return SessionService.fetchStatus();
        });
      };

      /* This hides from the caller of this service the details of the LO standard
       * status code 202 response that conveys user errors and async/challenges.
       */
      function dehttp(config) {
        return $http(config).then(
          function success(o) {
            if (o.status == 202) {
              console.log('accepted response: ' + o.data.status);
              if (o.data.status == 'async') {
                // TODO: implement async support
                return $q.reject(o);
              } else if (o.data.status == 'challenge' && !config.headers['X-Challenge-Response']) {
                // TODO: I ought to clone config and headers but let's just side-effect for now
                try {
                  //THIS comes from a mix of de.js and sha1.js
                  config.headers['X-Challenge-Response'] = rstr2b64(
                    rstr_sha1(str2rstr_utf8(o.data.challenge))
                  );
                } catch (e) {
                  console.error('Cannot create challenge response');
                }
                return dehttp(config);
              } else {
                return $q.reject(o);
              }
            } else {
              return o;
            }
          },
          function failure(o) {
            return $q.reject(o);
          }
        );
      }

      SessionService.getLoginMechanisms = function () {
        return Request.promiseRequest(loConfig.user.loginMechanisms);
      };

      /**
       *  @params {String} search the key to search and recover account, username, useremail@email.com
       *  @params {String} searchType 'emailAddress' or 'userName'
       *  @returns {Promise} a $q
       */
      SessionService.recover = function (search, searchType) {
        var params = {
          message: '',
          properties: searchType ? searchType : 'emailAddress',
          redirect: '/#/resetPassword/',
          search: search,
        };
        var headers = {
          Accept: '*/*',
          'Content-Type': 'application/x-www-form-urlencoded',
          'X-Requested-With': 'XMLHttpRequest',
        };
        var conf = {
          url: loConfig.user.recover,
          method: 'POST',
          headers: headers,
          data: SessionService.legacyEncode(params),
        };
        return dehttp(conf);
      };

      SessionService.validateReset = function (token) {
        var conf = {
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

      SessionService.reset = function (token, password) {
        var conf = {
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

      SessionService.legacyEncode = function (params) {
        var str = ''; //CBLPROD-1781
        each(params, function (v, k) {
          k = encodeURIComponent(k);
          if (isArray(v)) {
            each(v, function (ventry) {
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
      SessionService.login = function (params) {
        var conf = {
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
        return (
          window.lo_platform && window.lo_platform.session && window.lo_platform.session.sudoed
        );
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
        $rootScope.$emit('SessionService.logout');
        PresenceService.closePresence();
        return $http.post(loConfig.user.logout, {}).then(res => res.data);
      };

      SessionService.exit = function () {
        $rootScope.$emit('SessionService.exit');
        PresenceService.closePresence();
        return $http.post(loConfig.user.exit).then(function (res) {
          window.location.href = angular.isString(res.data) ? res.data : '/';
        });
      };

      SessionService.canLogout = function () {
        // If LTI, only show logout if there's a return URL and not an iframe.
        var showLTI =
          !SessionService.isLti() ||
          (window.lo_platform.session.logoutReturnUrl && !SessionService.isIframe());

        return (
          !SessionService.isSudo() &&
          showLTI &&
          window.lo_platform.user &&
          window.lo_platform.user.status !== 'error'
        );
      };

      SessionService.logoutAndRedirect = function () {
        SessionService.logout().then(function (redirect) {
          if (redirect) {
            window.location.href = redirect;
          } else {
            RedirectService.redirectToLogin();
          }
        });
      };

      return SessionService;
    },
  ])
  .factory('SessionListener', [
    '$q',
    '$rootScope',
    'Settings',
    function ($q, $rootScope, Settings) {
      var listen = {
        backoff: 2,
        lastCheckedAt: null,
      };
      listen.request = function (config) {
        return config;
      };
      listen.response = function (response) {
        return response;
      };

      listen.responseError = function (response) {
        if (response && +response.status === 403) {
          //console.log('Do a 403 check.');
          var last = listen.lastCheckedAt;
          var backoff = Settings.getSettings('SessionListener') || listen.backoff;
          if (last && last.isAfter(dayjs().subtract(backoff, 'minutes'))) {
            return $q.reject(response);
          } else {
            console.warn('Do a 403 check.  And emit event');
            listen.lastCheckedAt = dayjs(); //This is to make sure we don't spam 403 checks
            $rootScope.$emit('DoSessionListenerCheck', {
              failed: response,
              lastChecked: listen.lastCheckedAt,
            });
          }
        }
        return $q.reject(response);
      };
      return listen;
    },
  ])
  .factory('SessionManager', [
    '$q',
    '$window',
    '$timeout',
    '$rootScope',
    'SessionService',
    'IdleModal',
    /**
     * @ngdoc service
     * @alias SessionManager
     * @memberof lo.services
     * @description manages the state and the expiry of session, giving out
     * warnings and take actions on expiry.
     */
    function SessionManager($q, $window, $timeout, $rootScope, SessionService, IdleModal) {
      /** @alias SessionManager **/
      var SessionManager = {
        defaultLogoutAction: function () {
          $window.location.href = '/';
        },
        logoutBlockers: {},
        warnPeriod: 10 * 60 * 1000,
        initAt: dayjs().add(2, 'seconds'), //THIS IS BAD AND SHOULD NOT BE IN DEV.  CBLPROD-1925
        listener: false, //Listen to all http requests looking for 403s
      };

      /**
       * @description do a session status check, take action if needed, and
       * schedule another if session can continue
       */
      SessionManager.start = function (logoutAction) {
        SessionManager.logoutAction = logoutAction;
        if (!SessionManager.listener) {
          //console.log('MAKE A LISTENER 403');
          SessionManager.listener = $rootScope.$on('DoSessionListenerCheck', function () {
            SessionManager.test(true);
          });
        }
        return SessionManager.test(false); //Loop on the session tests with a long timeout period
      };

      SessionManager.loggedOutError = function () {
        if (dayjs().isAfter(SessionManager.initAt)) {
          $rootScope.$emit('SessionService.expired');
          IdleModal.loggedOutError(function () {
            const returnUrl =
              get(window, 'lo_platform.session.logoutReturnUrl') ||
              get(window, 'lo_platform.session.returnUrl');
            if (returnUrl) {
              window.location.href = returnUrl;
            } else {
              window.location.reload(true);
            }
          });
        } else {
          console.error(
            'Logged out, but the session is not currently open long enough to throw a warn.'
          );
        }
      };

      SessionManager.test = function (singleCheck) {
        return SessionService.fetchStatus().then(function (response) {
          var expiry = response.data.expiry || response.data.expires;

          if (!isNumber(expiry)) {
            SessionManager.loggedOutError(); //Probably correct?  Maybe
            return $q.when({});
          }
          if (expiry < SessionManager.warnPeriod && dayjs().isAfter(SessionManager.initAt)) {
            return IdleModal.open(expiry).then(function (result) {
              if (result) {
                return SessionService.renew().then(function () {
                  return $timeout(SessionManager.start, expiry - SessionManager.warnPeriod);
                });
              } else {
                $rootScope.$emit('SessionService.expired');
                return SessionService.logout().then(SessionManager.logout);
              }
            });
          } else if (!singleCheck) {
            // some browsers blow up on delays that don't fit in 32 bits
            const safeDelay = min([expiry - SessionManager.warnPeriod, 2147483647]);
            return $timeout(function () {
              SessionManager.start(SessionManager.logoutAction, false);
            }, safeDelay);
          }
        }, SessionManager.loggedOutError);
      };

      SessionManager.logout = function (customRedirect) {
        var logoutAction = SessionManager.logoutAction || SessionManager.defaultLogoutAction;

        var blockerResults = map(SessionManager.logoutBlockers, function (blocker) {
          var result = isFunction(blocker) && blocker();

          if (isFunction(result.then)) {
            //is a promise
            return result;
          } else {
            return result ? $q.when() : $q.reject();
          }
        });

        return $q
          .all(blockerResults)
          .then(SessionService.logout)
          .then(function (redirect) {
            if (customRedirect) {
              window.location.href = customRedirect;
            } else if (redirect) {
              window.location.href = redirect;
            } else {
              logoutAction();
            }
          });
      };

      SessionManager.exit = SessionService.exit;

      var idCounter = 0;
      SessionManager.registerLogoutBlocker = function (blocker) {
        var id = idCounter++;
        SessionManager.logoutBlockers[id] = blocker;
        return function () {
          delete SessionManager.logoutBlockers[id];
        };
      };

      return SessionManager;
    },
  ]);
