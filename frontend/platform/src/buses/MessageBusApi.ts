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

export type MessageBus = {
  id: number;
  name: string;
  state: BusState;
  system: string;
  scheduled: string;
  statistics: BusStatistics;
  queueSize: number;
};

type DeMessageBus = {
  id: number;
  name: string;
  state: BusState;
  system: System | null;
  scheduled: string;
  statistics: BusStatistics;
  queueSize: number;
};

export function fetchBuses(): Promise<MessageBus[]> {
  return axios
    .get<
      SrsCollection<DeMessageBus>,
      AxiosResponse<SrsCollection<DeMessageBus>>
    >('/api/v2/messageBuses')
    .then(res =>
      res.data.objects.map(bus => {
        return {
          id: bus.id,
          name: bus.name,
          state: bus.state,
          system: bus.system === null ? '<deleted>' : `${bus.system.name} (${bus.system.systemId})`,
          scheduled: formatScheduled(bus),
          statistics: bus.statistics,
          queueSize: bus.queueSize,
        } as MessageBus;
      })
    );
}

function formatScheduled(bus: DeMessageBus): string {
  const scheduled = moment(bus.scheduled);
  return bus.state !== 'Active' ? '-' : scheduled.isBefore(moment()) ? 'now' : scheduled.fromNow();
}

export function pauseBus(busId: number): Promise<void> {
  return axios.put(`/api/v2/messageBuses/${busId}/pause`);
}

export function resumeBus(busId: number): Promise<void> {
  return axios.put(`/api/v2/messageBuses/${busId}/resume`);
}

export function stopBus(busId: number): Promise<void> {
  return axios.put(`/api/v2/messageBuses/${busId}/stop`);
}
