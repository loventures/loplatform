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

import { reduce, map, pickBy, isArray } from 'lodash';

import SessionService from './SessionService.js';
import confirmModal from '../directives/confirmModal/index.js';
import uibModal from 'angular-ui-bootstrap/src/modal';
import { history } from '../utilities/history.js';

export default angular
  .module('lo.services.NavBlockerService', [uibModal, SessionService.name, confirmModal.name])
  .service('NavBlockerService', [
    '$q',
    '$translate',
    '$uibModal',
    function ($q, $translate, $uibModal) {
      const NavBlockerService = {
        blockers: {},
        ignoreIntercept: false,
      };

      NavBlockerService.getActiveBlockerMessages = concernedMessageKeys => {
        const concerenedBlockers =
          concernedMessageKeys && isArray(concernedMessageKeys)
            ? pickBy(
                NavBlockerService.blockers,
                (fn, key) => concernedMessageKeys.indexOf(key) !== -1
              )
            : NavBlockerService.blockers;

        return reduce(
          concerenedBlockers,
          (allMsg, conditionFn, i18nMsgKey) => {
            return conditionFn() ? allMsg.concat(i18nMsgKey) : allMsg;
          },
          []
        );
      };

      NavBlockerService.confirmNavByModal = concernedMessageKeys => {
        const msgs = NavBlockerService.getActiveBlockerMessages(concernedMessageKeys);

        if (msgs.length === 0) {
          return $q.when();
        }

        return $uibModal.open({
          component: 'confirmModal',
          resolve: {
            message: [
              '$translate',
              $translate => $q.all(map(msgs, key => $translate(key))).then(msgs => msgs.join('; ')),
            ],
          },
        }).result;
      };

      NavBlockerService.blockBeforeUnload = event => {
        const msgs = NavBlockerService.getActiveBlockerMessages();
        if (msgs.length === 0) {
          return;
        }

        //Note: default msg can't actually be altered anymore...
        //See https://www.chromestatus.com/feature/5349061406228480
        let msg = $translate.instant(msgs.join('; '));
        event.returnValue = msg;
        return msg;
      };

      NavBlockerService.blockLogout = () => {
        return NavBlockerService.confirmNavByModal();
      };

      NavBlockerService.register = (navBlockConditionFn, i18nMsgKey) => {
        NavBlockerService.blockers[i18nMsgKey] = navBlockConditionFn;
        const unblock = history.block(() => {
          if (navBlockConditionFn()) {
            return i18nMsgKey;
          }
        });
        return () => {
          delete NavBlockerService.blockers[i18nMsgKey];
          unblock();
        };
      };

      return NavBlockerService;
    },
  ])
  .run([
    'NavBlockerService',
    'SessionManager',
    '$window',
    function (NavBlockerService, SessionManager, $window) {
      $window.addEventListener('beforeunload', NavBlockerService.blockBeforeUnload);

      SessionManager.registerLogoutBlocker(NavBlockerService.blockLogout);
    },
  ]);
