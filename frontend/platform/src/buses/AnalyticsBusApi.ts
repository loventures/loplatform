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

import axios, { AxiosResponse } from 'axios';
import moment from 'moment';

import { SrsCollection } from '../srs';
import { BusState, BusStatistics, System } from './types';

export type AnalyticBus = {
  id: number;
  name: string;
  state: BusState;
  system: System | null;
  scheduledHumanString: string;
  scheduled: string;
  statistics: BusStatistics;
  windowStart: string;
  windowSize: number;
  queueSize: number;
  failureCount: number;
  lastMaterializedViewRefreshDate: string | null;
};

type DeAnalyticBus = {
  id: number;
  name: string | null;
  state: BusState;
  system: System | null;
  scheduled: string;
  statistics: BusStatistics;
  windowStart: string;
  windowSize: number;
  queueSize: number;
  failureCount: number;
  lastMaterializedViewRefreshDate: string | null;
};

export function fetchBuses(): Promise<AnalyticBus[]> {
  return axios
    .get<
      SrsCollection<DeAnalyticBus>,
      AxiosResponse<SrsCollection<DeAnalyticBus>>
    >('/api/v2/analyticBuses')
    .then(res =>
      res.data.objects.map(bus => {
        return {
          id: bus.id,
          name: bus.name === null ? '<deleted>' : bus.name,
          state: bus.state,
          system: bus.system,
          scheduledHumanString: formatScheduled(bus),
          scheduled: bus.scheduled,
          statistics: bus.statistics,
          windowStart: formatWindowStart(bus),
          windowSize: bus.windowSize,
          queueSize: bus.queueSize,
          failureCount: bus.failureCount,
          lastMaterializedViewRefreshDate: bus.lastMaterializedViewRefreshDate,
        };
      })
    );
}

function formatScheduled(bus: DeAnalyticBus): string {
  const scheduled = moment(bus.scheduled);
  return bus.state !== 'Active' ? '-' : scheduled.isBefore(moment()) ? 'now' : scheduled.fromNow();
}

function formatWindowStart(bus: DeAnalyticBus): string {
  const start = moment(bus.windowStart);
  const ancient = moment('1971-01-01');
  return start.isBefore(ancient) ? '-' : start.fromNow();
}

export function pauseBus(busId: number): Promise<void> {
  return axios.post(`/api/v2/analyticBuses/${busId}/pause`);
}

export function resumeBus(busId: number): Promise<void> {
  return axios.post(`/api/v2/analyticBuses/${busId}/resume`);
}

export function pumpBus(busId: number): Promise<void> {
  return axios.post(`/api/v2/analyticBuses/${busId}/pump`);
}
