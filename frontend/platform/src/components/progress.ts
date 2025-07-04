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

import axios from 'axios';
import { BProgress, BProgressOptions } from '@bprogress/core';

const calculatePercentage = (loaded: number, total: number) => Math.floor(loaded * 1.0) / total;

type AugmentedAxiosConfig = { hideProgress?: boolean };

export function loadProgressBar(config: Partial<BProgressOptions>) {
  let requestsCounter = 0;

  const setupStartProgress = () => {
    axios.interceptors.request.use(
      config => {
        if (!(config as AugmentedAxiosConfig).hideProgress) {
          const update = (e: any) => {
            const percentage = calculatePercentage(e.loaded, e.total);
            BProgress.inc(percentage);
          };
          requestsCounter++;
          BProgress.start();
          return { ...config, onUploadProgress: update, onDownloadProgress: update };
        } else {
          return config;
        }
      },
      error => {
        if (!error.config.hideProgress) {
          if (--requestsCounter === 0) {
            BProgress.done();
          }
        }
        return Promise.reject(error);
      }
    );
  };

  const setupStopProgress = () => {
    axios.interceptors.response.use(
      response => {
        if (!(response.config as AugmentedAxiosConfig).hideProgress) {
          if (--requestsCounter === 0) {
            BProgress.done();
          }
        }
        return response;
      },
      error => {
        if (!error.config.hideProgress) {
          if (--requestsCounter === 0) {
            BProgress.done();
          }
        }
        return Promise.reject(error);
      }
    );
  };

  BProgress.configure(config);
  setupStartProgress();
  setupStopProgress();
}
