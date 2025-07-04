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

import FeedbackManager from '../assignmentFeedback/FeedbackManager.js';
import Course from '../bootstrap/course.js';
import { extend, find, map } from 'lodash';
import MessagingService from '../services/MessagingService.js';

export default angular
  .module('lo.messaging.FullMessage', [MessagingService.name, FeedbackManager.name])
  .factory('FullMessage', [
    'MessagingService',
    '$q',
    'FeedbackManager',
    function (MessagingService, $q, FeedbackManager) {
      var FullMessage = function () {
        this.questionId = '';
        this.feedbackManager = new FeedbackManager();
        this.recipients = [];
      };

      FullMessage.prototype.prepareRecipients = function () {
        if (this.selectingEntireClass) {
          return $q.when([
            {
              _type: 'context',
              context: Course.id,
            },
          ]);
        } else {
          return $q.when(
            map(this.recipients, function (user) {
              return {
                _type: 'user',
                user: user.id,
              };
            })
          );
        }
      };

      FullMessage.prototype.serialize = function (overwrite) {
        return extend(
          {
            subject: this.title,
            body: this.content,
            recipients: this.recipients,
            uploads: this.feedbackManager.getFilesInStaging(),
          },
          overwrite
        );
      };

      FullMessage.prototype.isValid = function () {
        return true;
      };

      FullMessage.prototype.isSelected = function (user) {
        return find(this.recipients, function (selected) {
          return selected.id === user.id;
        });
      };

      /**
       * Returns true if it has selections or if it was set to send to
       * the entire class (assumes there is at least one person in the class)
       */
      FullMessage.prototype.hasRecipients = function () {
        return !!this.selectingEntireClass || this.recipients.length > 0;
      };

      FullMessage.prototype.setSelection = function (selection) {
        this.recipients = selection || [];
      };

      FullMessage.prototype.addSelection = function (user) {
        var index = this.recipients.indexOf(user);
        if (index === -1) {
          this.recipients.push(user);
        }
      };

      FullMessage.prototype.removeSelection = function (user) {
        var index = this.recipients.indexOf(user);
        if (index !== -1) {
          this.recipients.splice(index, 1);
        }
      };

      FullMessage.prototype.selectEntireClass = function (isSelected) {
        this.selectingEntireClass = isSelected;
      };

      FullMessage.prototype.send = function () {
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

      return FullMessage;
    },
  ]);
