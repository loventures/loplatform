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

import moment from 'moment-timezone';
import Polyglot from 'node-polyglot';
import React from 'react';
import { ConnectedProps, connect } from 'react-redux';

import { inCurrTimeZone } from '../services/moment';
import * as api from './AnalyticsBusApi';
import { AnalyticBus } from './AnalyticsBusApi';
import BusChart, { SeriesConfig } from './BusChart';
import BusTable, { BusTableButton, BusTableColumn } from './BusTable';
import { IoTrainOutline } from 'react-icons/io5';

const MaxCount = 10000;

class App extends React.Component<Props, State> {
  readonly state: State = {
    loaded: false,
    analyticBuses: [],
  };

  componentDidMount() {
    this.refresh();
  }

  componentWillUnmount() {
    if (this.poller) {
      window.clearTimeout(this.poller);
    }
  }

  poller: number | null = null;
  pollCount = 0;

  poll = () => {
    this.poller = null;
    ++this.pollCount;
    this.refresh();
  };

  refresh = () => {
    return api.fetchBuses().then(buses => {
      this.setState({ loaded: true, analyticBuses: buses, selectedRow: undefined });
      if (!this.poller) {
        if (this.pollCount < 60) {
          // poll for an hour, just for kicks
          this.poller = window.setTimeout(this.poll, 60000);
        } else {
          this.pollCount = 0;
        }
      }
    });
  };

  static readonly seriesConfigs: SeriesConfig[] = [
    { name: 'sumQueued', color: 'orange' },
    { name: 'sumDelivered', color: 'green' },
    { name: 'sumFailed', color: 'red' },
    { name: 'avgBusExecMillis', color: 'blue' },
  ];

  static readonly columns: BusTableColumn<any>[] = [
    { dataField: 'id', isKey: true },
    { dataField: 'name' },
    { dataField: 'state', width: '10%' },
    { dataField: 'scheduledHumanString' },
    { dataField: 'scheduled' },
    { dataField: 'windowStart' },
    { dataField: 'failureCount', width: '10%' },
    {
      dataField: 'queueSize',
      width: '10%',
      dataFormat: sz => (sz > MaxCount ? '> ' + MaxCount : sz),
    } as BusTableColumn<number>,
    {
      dataField: 'lastMaterializedViewRefreshDate',
      dataFormat: d => inCurrTimeZone(moment(d)).format('H:mm:ss z YYYY-MM-DD'),
    },
  ];

  setSelectedRow = (bus?: AnalyticBus) => this.setState({ selectedRow: bus });

  buttons: BusTableButton[] = [
    {
      id: 'resume',
      icon: 'play_arrow',
      disabled: bus => !bus || bus.state === 'Active',
      onClick: bus => api.resumeBus(bus.id).then(this.refresh),
    },
    {
      id: 'pause',
      icon: 'pause',
      disabled: bus => !bus || bus.state !== 'Active',
      onClick: bus => api.pauseBus(bus.id).then(this.refresh),
    },
    {
      id: 'pump',
      icon: 'room_service',
      disabled: bus => !bus || bus.state !== 'Active',
      onClick: bus => api.pumpBus(bus.id).then(this.refresh),
    },
    {
      id: 'help',
      icon: 'help_outline',
      disabled: bus => !bus || bus.name !== 'RedshiftEventSender',
      onClick: () => (window.location.href = '/doc/redshift-schema'),
    },
  ];

  render() {
    return (
      this.state.loaded && (
        <div className="container-fluid mb-4">
          <BusChart
            buses={this.state.analyticBuses}
            seriesConfigs={App.seriesConfigs}
            entity="analyticBuses"
            T={this.props.translations}
          />
          <BusTable
            buses={this.state.analyticBuses}
            buttons={this.buttons}
            columns={App.columns}
            refreshBuses={this.refresh}
            selectedRow={this.state.selectedRow}
            handleRowSelect={this.setSelectedRow}
            entity="analyticBuses"
            T={this.props.translations}
          />
        </div>
      )
    );
  }

  static pageInfo = {
    identifier: 'analyticBuses',
    icon: IoTrainOutline,
    link: '/AnalyticBuses',
    group: 'integrations',
    right: 'loi.cp.admin.right.IntegrationAdminRight',
  };
}

function mapStateToProps(state: any) {
  return {
    translations: state.main.translations as Polyglot,
  };
}

const connector = connect(mapStateToProps);
type PropsFromRedux = ConnectedProps<typeof connector>;

const AnalyticBuses = connector(App);

type Props = PropsFromRedux;

type State = {
  loaded: boolean;
  analyticBuses: AnalyticBus[];
  selectedRow?: AnalyticBus;
};

export default AnalyticBuses;
