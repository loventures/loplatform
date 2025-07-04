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

import { BProgress } from '@bprogress/core';

import '@bprogress/core/css';

const calculatePercentage = (loaded, total) => Math.floor(loaded * 1.0) / total;

export function loadProgressBar(config) {
  let requestsCounter = 0;

  const setupStartProgress = () => {
    // axios.interceptors.request.use(
    //   config => {
    //     if (!config.hideProgress) {
    //       const update = e => {
    //         const percentage = calculatePercentage(e.loaded, e.total);
    //         BProgress.inc(percentage);
    //       };
    //       requestsCounter++;
    //       BProgress.start();
    //       return { ...config, onUploadProgress: update, onDownloadProgress: update };
    //     } else {
    //       return config;
    //     }
    //   },
    //   error => {
    //     if (!error.config.hideProgress) {
    //       if (--requestsCounter === 0) {
    //         BProgress.done();
    //       }
    //     }
    //     return Promise.reject(error);
    //   }
    // );
  };

  const setupStopProgress = () => {
    // axios.interceptors.response.use(
    //   response => {
    //     if (!response.config.hideProgress) {
    //       if (--requestsCounter === 0) {
    //         BProgress.done();
    //       }
    //     }
    //     return response;
    //   },
    //   error => {
    //     if (!error.config.hideProgress) {
    //       if (--requestsCounter === 0) {
    //         BProgress.done();
    //       }
    //     }
    //     return Promise.reject(error);
    //   }
    // );
  };

  BProgress.configure(config);
  setupStartProgress();
  setupStopProgress();
}
