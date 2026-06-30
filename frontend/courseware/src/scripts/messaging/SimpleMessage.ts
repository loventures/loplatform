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

import $ from 'jquery';
import { extend, map } from 'lodash';

import messagingService from '../services/messagingService.ts';
import { nativeQ as $q } from '../utilities/nativeQ.ts';

/**
 * The at-risk / direct-message model, ported verbatim from the AngularJS
 * `SimpleMessage` `.factory` to a plain TS constructor function + prototype.
 * The injected `MessagingService`/`$q` are now direct imports of the pure
 * singleton / native-$q shim.
 */
export const SimpleMessage: any = function (this: any) {
  this.questionId = '';
  this.recipients = [];
};

SimpleMessage.prototype.prepareRecipients = function () {
  return $q.when(
    map(this.recipients, function (user: any) {
      return {
        _type: 'user',
        user: user.id,
      };
    })
  );
};

SimpleMessage.prototype.serialize = function (overwrite: any) {
  // text area produces plain text,
  // but when sent as email we send them as text/html
  // so to avoid text <foo> being trated as dom and \n being ignored
  // need to convert content to escaped html text and \n to <br>
  var body = $('<p></p>').text(this.content).html().replace(/\n/g, '<br/>');

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
    function (this: any, recipients: any) {
      return messagingService.sendMessage(
        this.serialize({
          recipients: recipients,
        })
      );
    }.bind(this)
  );
};
