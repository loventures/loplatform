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

import Polyglot from 'node-polyglot';
import React from 'react';
import { ConnectedProps, connect } from 'react-redux';

import BusChart, { SeriesConfig } from './BusChart';
import BusTable, { BusTableButton, BusTableColumn } from './BusTable';
import * as api from './MessageBusApi';
import { MessageBus } from './MessageBusApi';
import { IoBusOutline } from 'react-icons/io5';

class App extends React.Component<Props, State> {
  readonly state: State = {
    loaded: false,
    messageBuses: [],
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
      this.setState({ loaded: true, messageBuses: buses, selectedRow: undefined });
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
    { name: 'sumDropped', color: 'black' },
    { name: 'avgMsgExecMillis', color: 'blue' },
  ];

  static readonly columns: BusTableColumn<any>[] = [
    { dataField: 'id', isKey: true },
    { dataField: 'system' },
    { dataField: 'state', width: '15%' },
    { dataField: 'scheduled' },
    { dataField: 'queueSize' },
  ];

  setSelectedRow = (bus?: MessageBus) => this.setState({ selectedRow: bus });

  buttons = [
    {
      id: 'resume',
      icon: 'play_arrow',
      disabled: bus => !bus || bus.state === 'Active',
      onClick: bus => api.resumeBus(bus.id).then(this.refresh),
    } as BusTableButton,
    {
      id: 'pause',
      icon: 'pause',
      disabled: bus => !bus || bus.state !== 'Active',
      onClick: bus => api.pauseBus(bus.id).then(this.refresh),
    } as BusTableButton,
    {
      id: 'stop',
      icon: 'stop',
      disabled: bus => !bus || bus.state === 'Disabled',
      onClick: bus => api.stopBus(bus.id).then(this.refresh),
    } as BusTableButton,
  ];

  render() {
    return (
      this.state.loaded && (
        <div className="container-fluid mb-4">
          <BusChart
            buses={this.state.messageBuses}
            seriesConfigs={App.seriesConfigs}
            entity="messageBuses"
            T={this.props.translations}
          />
          <BusTable
            buses={this.state.messageBuses}
            buttons={this.buttons}
            columns={App.columns}
            refreshBuses={this.refresh}
            selectedRow={this.state.selectedRow}
            handleRowSelect={this.setSelectedRow}
            entity="messageBuses"
            T={this.props.translations}
          />
        </div>
      )
    );
  }

  static pageInfo = {
    identifier: 'messageBuses',
    icon: IoBusOutline,
    link: '/MessageBuses',
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
const MessageBuses = connector(App);

type Props = PropsFromRedux;

type State = {
  loaded: boolean;
  messageBuses: MessageBus[];
  selectedRow?: MessageBus;
};

export default MessageBuses;
