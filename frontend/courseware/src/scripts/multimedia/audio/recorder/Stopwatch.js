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

export default angular.module('lo.multimedia.Stopwatch', []).factory('Stopwatch', [
  '$interval',
  function ($interval) {
    class Stopwatch {
      constructor(interval, tickCallback) {
        this.increment = interval;
        this.elapsed = 0;
        this.tickCallback = tickCallback;
      }
      start() {
        if (this.running) {
          return;
        } else {
          this.before = new Date();
          this.running = true;
          this.active = true;
          this.timer = $interval(() => this.$tick(), this.increment);
        }
      }
      pause() {
        $interval.cancel(this.timer);
        this.running = false;
      }
      stop() {
        $interval.cancel(this.timer);
        delete this.timer;
        this.running = false;
        this.active = false;
        return this.elapsed;
      }
      reset() {
        this.stop();
        this.elapsed = 0;
      }
      $tick() {
        this.intervals++;
        this.now = new Date();
        var elapsed = this.now.getTime() - this.before.getTime();
        this.elapsed += elapsed;
        this.before = new Date();
        if (this.tickCallback) {
          this.tickCallback(this);
        }
      }
      timeElapsed() {
        return this.elapsed;
      }
    }

    return Stopwatch;
  },
]);
