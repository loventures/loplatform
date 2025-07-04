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

import dayjs from 'dayjs';

import template from './quizAutosave.html';

export default angular.module('lo.quizViews.quizAutosave', []).component('quizAutosave', {
  template,
  bindings: {
    hasChanges: '<',
    lastSaveFailed: '<',
    save: '<',
    lastChange: '<',
    lastSave: '<',
  },
  controller: [
    '$timeout',
    '$ngRedux',
    function ($timeout, $ngRedux) {
      this.autosaveInterval = 3 * 60 * 1000;
      this.autosaveChangeDelay = 30 * 1000;

      this.$onInit = () => {
        $ngRedux.connectToCtrl(
          state => ({
            state: state.ui.quizAttemptAutoSaveState[this.attemptId] || {},
          }),
          {}
        )(this);
      };

      this.autoSave = () => {
        if (this.hasChanges && /*this.lastSaveFailed ||*/ !this.state.loading) {
          this.save();
        }
        //prevent spamming if it fails
        this.lastSave = dayjs().valueOf();
        this.lastChange = this.lastSave;
        this.estimateNextSave();
      };

      this.$onDestroy = () => {
        $timeout.cancel(this.timeout);
      };

      this.$onChanges = ({ lastChange, lastSave, hasChanges }) => {
        if (lastChange || lastSave || hasChanges) {
          this.estimateNextSave();
        }
      };

      this.estimateNextSave = () => {
        // I don't understand any of this.
        const nextIntervalSave = this.lastSave + this.autosaveInterval;
        const nextDelayedSave = this.lastChange + this.autosaveChangeDelay;
        const nextSave = Math.min(nextIntervalSave, nextDelayedSave) - dayjs().valueOf();
        $timeout.cancel(this.timeout);
        this.timeout = $timeout(this.autoSave, Math.max(0, nextSave));
      };
    },
  ],
});
