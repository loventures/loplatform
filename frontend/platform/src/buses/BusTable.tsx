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

import classNames from 'classnames';
import Polyglot from 'node-polyglot';
import React, { ReactNode } from 'react';
import { Button, ButtonGroup, Col, Popover, PopoverBody, Row } from 'reactstrap';

import { Bus } from './types';
import Table from '../components/reactTable/Table';

class BusTable extends React.Component<Props, State> {
  readonly state: State = {};

  renderButtonBar = (): ReactNode => {
    const { popover } = this.state;
    const { refreshBuses, buttons, selectedRow, entity, T } = this.props;
    const baseName = `adminPage.${entity}.toolBar`;
    return (
      <Col
        xs={12}
        lg={4}
        className="actionButtons"
      >
        <ButtonGroup>
          {buttons.map((btn, i) => (
            <Button
              key={btn.id}
              id={`table-${btn.id}-button` as string}
              className={classNames({
                glyphButton: true,
                lastButton: i === buttons.length - 1,
              })}
              disabled={btn.disabled(selectedRow)}
              onMouseOver={() => this.togglePopover(btn.id)}
              onMouseOut={() => this.togglePopover()}
              onClick={() => selectedRow && btn.onClick(selectedRow)}
            >
              <i
                className="material-icons md-18"
                aria-hidden="true"
              >
                {btn.icon}
              </i>
            </Button>
          ))}
          <Button
            id="table-refresh-button"
            size="sm"
            onClick={refreshBuses}
            className="ms-2 hidden-sm-down refreshButton"
          >
            <i
              className="material-icons md-18"
              aria-hidden="true"
            >
              refresh
            </i>
          </Button>
          {popover && (
            <Popover
              placement="bottom"
              isOpen={true}
              target={`table-${popover}-button` as string}
            >
              <PopoverBody>{T.t(`${baseName}.${popover}Button`)}</PopoverBody>
            </Popover>
          )}
        </ButtonGroup>
      </Col>
    );
  };

  togglePopover = (btnId?: string): void => this.setState({ popover: btnId });

  renderTable = (): ReactNode => {
    const { entity, T, buses, columns, selectedRow } = this.props;

    return (
      <Table
        entity={entity}
        pageSize={10}
        totalSize={buses.length}
        stats={{ count: buses.length }}
        currentData={buses}
        csvUrl=""
        columns={columns}
        T={T}
        orderField="name"
        orderDir="asc"
        currentPage={1}
        loaded={true}
        fetching={false}
        multiSelect={false}
        onRowSelect={this.handleRowSelect}
        selectedRows={selectedRow ? [selectedRow] : []}
      />
    );
  };

  handleRowSelect = (row: Bus, isSelected: boolean): void => {
    this.props.handleRowSelect(isSelected ? row : undefined);
  };

  render(): ReactNode {
    return (
      <React.Fragment>
        <Row className="reactTable-buttonBar">{this.renderButtonBar()}</Row>
        <Row>
          <Col id={`table-${this.props.entity}` as string}>{this.renderTable()}</Col>
        </Row>
      </React.Fragment>
    );
  }
}

type Props = {
  buses: Bus[];
  buttons: BusTableButton[];
  columns: BusTableColumn<any>[];
  refreshBuses(): void;
  selectedRow?: Bus;
  handleRowSelect(bus?: Bus): void;
  entity: string;
  T: Polyglot;
};

type State = {
  popover?: string;
};

export type BusTableButton = {
  id: string;
  icon: string;
  disabled: (bus?: Bus) => boolean;
  onClick: (bus: Bus) => void;
};

export type BusTableColumn<A> = {
  dataField: string;
  isKey?: boolean;
  dataFormat?(a: A): string | A | ReactNode;
  width?: string;
};

export default BusTable;
