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

import { map, extend } from 'lodash';

import MessagingService from '../services/MessagingService.js';

export default angular
  .module('lo.messaging.SimpleMessage', [MessagingService.name])
  .factory('SimpleMessage', [
    'MessagingService',
    '$q',
    function (MessagingService, $q) {
      var SimpleMessage = function () {
        this.questionId = '';
        this.recipients = [];
      };

      SimpleMessage.prototype.prepareRecipients = function () {
        return $q.when(
          map(this.recipients, function (user) {
            return {
              _type: 'user',
              user: user.id,
            };
          })
        );
      };

      SimpleMessage.prototype.serialize = function (overwrite) {
        // text area produces plain text,
        // but when sent as email we send them as text/html
        // so to avoid text <foo> being trated as dom and \n being ignored
        // need to convert content to escaped html text and \n to <br>
        var body = angular.element('<p></p>').text(this.content).html().replace(/\n/g, '<br/>');

        return extend(
          {
            subject: this.title,
            body: body,
            uploads: [],
          },
          overwrite
        );
      };

      SimpleMessage.prototype.isValid = function () {
        return !!this.content;
      };

      SimpleMessage.prototype.send = function () {
        return this.prepareRecipients().then(
          function (recipients) {
            return MessagingService.sendMessage(
              this.serialize({
                recipients: recipients,
              })
            );
          }.bind(this)
        );
      };

      return SimpleMessage;
    },
  ]);
