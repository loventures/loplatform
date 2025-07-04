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
import React, { ReactNode } from 'react';
import { Col, Input, Row } from 'reactstrap';
import { CartesianGrid, Label, Legend, Line, LineChart, Tooltip, XAxis, YAxis } from 'recharts';
import { AxisDomain } from 'recharts/types/util/types';

import { inCurrTimeZone } from '../services/moment';
import { Bus, BusStatistics } from './types';

type BusChartProps = {
  buses: Bus[];
  seriesConfigs: SeriesConfig[];
  entity: string;
  T: Polyglot;
};

type BusChartState = {
  clientWidth: number;
  bus: number;
  span: number;
};

class BusChart extends React.Component<BusChartProps, BusChartState> {
  constructor(props: BusChartProps) {
    super(props);
    this.state = {
      clientWidth: document.documentElement.clientWidth,
      bus: -1,
      span: 1,
    };
  }

  componentDidMount(): void {
    window.addEventListener('resize', this.handleResize);
  }

  componentWillUnmount(): void {
    window.removeEventListener('resize', this.handleResize);
  }

  handleResize = (): void => {
    this.setState({ clientWidth: document.documentElement.clientWidth });
  };

  timeSpans: TimeSpan[] = [
    {
      unit: 'minute',
      tickFormat: 'h:mma',
      labelFormat: 'h:mma z (MMM D, YYYY)',
      format: 'YYYY-MM-DDTHH:mm',
      field: 'byMinute',
      entries: 60,
    },
    {
      unit: 'hour',
      tickFormat: 'ha',
      labelFormat: 'ha z (MMM D, YYYY)',
      format: 'YYYY-MM-DDTHH',
      field: 'byHour',
      entries: 24,
    },
    {
      unit: 'day',
      tickFormat: 'MMM',
      labelFormat: 'MMM D z, YYYY',
      format: 'YYYY-MM-DD',
      field: 'byDay',
      entries: 365,
    },
  ];

  computeChartData = (): ChartData => {
    const now = moment();
    const dataPoints: DataPoint[] = [];
    const ticks: number[] = [];
    const timeSpan = this.timeSpans[this.state.span];
    const selectedBus = this.state.bus;
    for (let i = 0; i < timeSpan.entries; ++i) {
      const when = now.clone().subtract(timeSpan.entries - i - 1, timeSpan.unit);
      const time = when.toDate().getTime();
      ticks.push(time);
      const lookup = when.utc().format(timeSpan.format);

      const points = this.props.buses
        .filter((_bus, i) => selectedBus < 0 || selectedBus === i)
        .map(bus => bus.statistics[timeSpan.field][lookup])
        .filter(x => x !== undefined);

      if (points.length > 0) {
        const point = points.reduce<DataPoint>(
          (acc, point) => {
            acc.sumDelivered += point.delivered;
            acc.sumFailed += point.failed;
            acc.sumDropped += point.dropped;
            acc.sumQueued += point.queued;
            acc.sumMillis += point.millis;
            acc.sumExecutions += point.executions;
            return acc;
          },
          {
            sumDelivered: 0,
            sumFailed: 0,
            sumDropped: 0,
            sumQueued: 0,
            sumMillis: 0,
            sumExecutions: 0,
            avgMsgExecMillis: 0,
            avgBusExecMillis: 0,
            time,
          }
        );

        const sumMsgs = point.sumDelivered + point.sumFailed + point.sumDropped;
        point.avgMsgExecMillis = sumMsgs > 0 ? Math.round(point.sumMillis / sumMsgs) : 0;
        point.avgBusExecMillis =
          point.sumExecutions > 0 ? Math.round(point.sumMillis / point.sumExecutions) : 0;
        dataPoints.push(point);
      }
    }

    return {
      dataPoints,
      // xDomain still necessary despite us setting tick marks explicitly /shrug
      xDomain: [
        now
          .clone()
          .subtract(timeSpan.entries - 1, timeSpan.unit)
          .toDate()
          .getTime(),
        now.toDate().getTime(),
      ],
      xTicks: ticks,
      // when no data, it looks better to render arbitrary ranges on the yAxes
      yLeftDomain: dataPoints.length === 0 ? [0, 1000] : [0, 'auto'],
      yRightDomain: dataPoints.length === 0 ? [0, 100] : [0, 'auto'],
    };
  };

  renderSelects = (): ReactNode => {
    const { entity, T } = this.props;
    const baseName = `adminPage.${entity}`;
    return [
      <Col
        key="host"
        xs={6}
        md={{ offset: 6, size: 3 }}
        className="pe-2"
      >
        <Input
          type="select"
          onChange={e => this.setHost(e.target.value)}
          value={this.state.bus}
        >
          <option value={-1}>{T.t(`${baseName}.option.allBuses`)}</option>
          {this.props.buses.map((bus, idx) => (
            <option
              key={bus.id}
              value={idx}
            >
              {bus.name}
            </option>
          ))}
        </Input>
      </Col>,
      <Col
        key="span"
        xs={6}
        md={3}
        className="ps-2"
      >
        <Input
          type="select"
          onChange={e => this.setSpan(e.target.value)}
          value={this.state.span}
        >
          {this.timeSpans.map((span, idx) => (
            <option
              key={span.unit}
              value={idx}
            >
              {T.t(`${baseName}.byTime.${span.unit}`)}
            </option>
          ))}
        </Input>
      </Col>,
    ];
  };

  setHost = (host: string): void => this.setState({ bus: parseInt(host, 10) });

  setSpan = (span: string): void => this.setState({ span: parseInt(span, 10) });

  renderCharts = (): ReactNode => {
    const { seriesConfigs, entity, T } = this.props;
    const baseName = `adminPage.${entity}`;
    const chartData = this.computeChartData();

    const width = this.state.clientWidth - 78;
    const height = Math.round(width / 3);
    const margins = { top: 10, right: 80, bottom: 30, left: 80 };
    const timeSpan = this.timeSpans[this.state.span];

    const formatTick = (time: number) => inCurrTimeZone(moment(time)).format(timeSpan.tickFormat);
    const formatLabel = (time: number) => inCurrTimeZone(moment(time)).format(timeSpan.labelFormat);
    const formatTooltip = (value: number, name: keyof DataPoint) => {
      const formattedValue = name.endsWith('Millis') ? value + ' ms' : value;
      return [formattedValue, T.t(`${baseName}.seriesName.${name}`)];
    };
    const formatLegend = (value: unknown) => T.t(`${baseName}.seriesName.${value}`);

    return (
      <Col
        style={{ position: 'relative', height: height }}
        className="mx-4"
      >
        <LineChart
          width={width}
          height={height}
          margin={margins}
          data={chartData.dataPoints}
        >
          <XAxis
            dataKey="time"
            type="number"
            scale="time"
            domain={chartData.xDomain}
            ticks={chartData.xTicks}
            tickFormatter={formatTick}
          />
          <YAxis
            yAxisId="left"
            domain={chartData.yLeftDomain}
          >
            <Label
              value={T.t(`${baseName}.axisName.count`)}
              angle={-90}
              position="left"
            />
          </YAxis>
          <YAxis
            yAxisId="right"
            domain={chartData.yRightDomain}
            orientation="right"
          >
            <Label
              value={T.t(`${baseName}.axisName.time`)}
              angle={90}
              position="right"
            />
          </YAxis>
          <CartesianGrid strokeDasharray="3 3" />
          <Tooltip
            formatter={formatTooltip}
            labelFormatter={formatLabel}
          />
          <Legend formatter={formatLegend} />
          {seriesConfigs
            .filter(({ name }) => !name.endsWith('Millis'))
            .map(({ name, color }) => (
              <Line
                type="monotone"
                dataKey={name}
                stroke={color}
                key={name}
                dot={false}
                yAxisId="left"
              />
            ))}
          {seriesConfigs
            .filter(({ name }) => name.endsWith('Millis'))
            .map(({ name, color }) => (
              <Line
                type="monotone"
                dataKey={name}
                stroke={color}
                key={name}
                dot={false}
                yAxisId="right"
              />
            ))}
        </LineChart>
      </Col>
    );
  };

  render(): ReactNode {
    const { entity, T } = this.props;
    return (
      <>
        <Row className="mb-4 g-0">{this.renderSelects()}</Row>
        <Row>
          <Col>
            <h5 style={{ textAlign: 'center' }}>{T.t(`adminPage.${entity}.chartTitle`)}</h5>
          </Col>
        </Row>
        <Row
          key="chart"
          className="mb-3"
        >
          {this.renderCharts()}
        </Row>
      </>
    );
  }
}

type TimeSpan = {
  unit: moment.unitOfTime.DurationConstructor;
  tickFormat: string;
  labelFormat: string;
  format: string;
  field: keyof BusStatistics;
  entries: number;
};

export type SeriesConfig = {
  name: keyof DataPoint;
  color: string;
};

type DataPoint = {
  sumDelivered: number;
  sumFailed: number;
  sumDropped: number;
  sumQueued: number;
  sumMillis: number;
  sumExecutions: number;
  avgMsgExecMillis: number;
  avgBusExecMillis: number;
  time: number;
};

type ChartData = {
  dataPoints: DataPoint[];
  xDomain: AxisDomain;
  xTicks: number[];
  yLeftDomain: AxisDomain;
  yRightDomain: AxisDomain;
};

export default BusChart;
