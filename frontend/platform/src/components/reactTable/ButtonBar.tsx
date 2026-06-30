/*
 * LO Platform copyright (C) 2007–2026 LO Ventures LLC.
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
import React, { useState } from 'react';
import {
  Button,
  ButtonDropdown,
  ButtonGroup,
  Col,
  DropdownItem,
  DropdownMenu,
  DropdownToggle,
  Popover,
  PopoverBody,
} from 'reactstrap';

import { LoPlatform } from '../../types/loPlatform';
import JefreshDotPng from '../../imgs/jefresh.png';

interface Row {
  id: number;
  [key: string]: unknown;
}

interface TableButton {
  key?: string;
  name: string;
  onClick?: (rows: any) => Promise<unknown>;
  multiSelect?: boolean;
  solo?: boolean;
  href?: string;
  target?: string;
  download?: boolean;
  className?: string;
  lastButton?: boolean;
  disabled?: boolean;
  alwaysEnabled?: boolean;
  rel?: string;
  color?: string;
  iconName?: string;
}

interface DropdownItemDef {
  key: string;
  name: React.ReactNode;
  onClick: () => void;
}

type TogglePopover = (type: string | null, bool: boolean, forcePopover?: boolean) => void;

interface ButtonBarProps {
  xs: number;
  md: number;
  lg: number;
  baseName: string;
  createButton: boolean;
  T: Polyglot;
  createDropdown: boolean;
  dropdownItems: DropdownItemDef[];
  updateButton: boolean;
  selectedRows: Row[];
  getButtons: (rows: any, togglePopover: TogglePopover) => Array<TableButton | React.ReactElement<any>>;
  multiSelect: boolean;
  canDeleteRow: (row: Row) => boolean;
  canUpdateRow: (row: Row) => boolean;
  deleteButton: boolean;
  multiDelete: boolean;
  onJefreshClicked: () => void;
  fetching: boolean;
  loaded: boolean;
  lo_platform: LoPlatform;
  refresh: () => void;
  showModal: (style: string) => void;
  onCreate?: () => void;
  renderForm?: (...args: any[]) => React.ReactNode;
  filterColsLength: number;
}

const ButtonBar: React.FC<ButtonBarProps> = props => {
  const {
    xs,
    md,
    lg,
    baseName,
    createButton,
    T,
    createDropdown,
    dropdownItems,
    updateButton,
    selectedRows,
    getButtons,
    multiSelect,
    canDeleteRow,
    canUpdateRow,
    deleteButton,
    multiDelete,
    onJefreshClicked,
    fetching,
    loaded,
    lo_platform,
    refresh,
    showModal,
    onCreate,
    renderForm,
    filterColsLength,
  } = props;

  const [popover, setPopover] = useState<string | null>(null);
  const [createDropdownOpen, setCreateDropdownOpen] = useState(false);
  const [forcePopover, setForcePopover] = useState<boolean | undefined>(undefined);

  const propButtonClick = (btn: TableButton) => {
    const rows =
      multiSelect && btn.multiSelect ? selectedRows : selectedRows.length && selectedRows[0];
    btn.onClick && btn.onClick(rows).then(res => res && refresh());
  };

  const togglePopover: TogglePopover = (type, bool, force) => {
    setPopover(bool ? type : null);
    setForcePopover(force);
  };

  const onDropdownClick = (item: DropdownItemDef) => {
    item.onClick();
    showModal('create');
  };

  const onCreateClickHandler = () => {
    if (onCreate) {
      onCreate();
    } else if (renderForm) {
      showModal('create');
    }
  };

  const renderCreate = () => {
    const id = 'react-table-create-button';
    const className = classNames({ 'col-lg-4': filterColsLength <= 2 });
    const label = filterColsLength > 2 ? T.t(`${baseName}.createButton`) : undefined;
    return (
      createButton && (
        <Button
          id={id}
          className={className}
          onClick={onCreateClickHandler}
          color="success"
          aria-label={label}
        >
          {filterColsLength > 2 ? (
            <i
              className="material-icons md-18"
              aria-hidden="true"
            >
              add
            </i>
          ) : (
            <React.Fragment>
              <i
                className="material-icons md-18 d-none d-lg-inline-block d-xl-none"
                aria-hidden="true"
              >
                add
              </i>
              <span className="d-lg-none d-xl-inline text-truncate">
                {T.t(`${baseName}.createButton`)}
              </span>
            </React.Fragment>
          )}
        </Button>
      )
    );
  };

  const toDropdown = (item: DropdownItemDef) => {
    const onClick = () => onDropdownClick(item);
    return (
      <DropdownItem
        role="menuitem"
        key={item.key}
        id={`create-type-${item.key}`}
        onClick={onClick}
      >
        {item.name}
      </DropdownItem>
    );
  };

  const renderCreateDropdown = () => {
    const toggle = () => setCreateDropdownOpen(open => !open);
    return (
      createDropdown && (
        <ButtonDropdown
          isOpen={createDropdownOpen}
          toggle={toggle}
        >
          <DropdownToggle
            caret
            color="success"
            id="react-table-create-dropdown-button"
            aria-controls="react-table-create-dropdown-menu"
          >
            {T.t(`${baseName}.createButton`)}
          </DropdownToggle>
          <DropdownMenu id="react-table-create-dropdown-menu">
            {dropdownItems.map(toDropdown)}
          </DropdownMenu>
        </ButtonDropdown>
      )
    );
  };

  const renderUpdate = () => {
    const oneSelected = selectedRows.length === 1;
    const isDisabled = !oneSelected || !canUpdateRow(selectedRows[0]);
    return (
      updateButton && (
        <Button
          id="react-table-update-button"
          onMouseOver={() => togglePopover('update', true)}
          onMouseOut={() => togglePopover('update', false)}
          onClick={() => showModal('update')}
          className="glyphButton"
          disabled={isDisabled}
          aria-label={T.t(`${baseName}.updateButton`)}
        >
          <i
            className="material-icons md-18"
            aria-hidden="true"
          >
            edit
          </i>
        </Button>
      )
    );
  };

  const renderCustomButtons = (
    btns: Array<TableButton | React.ReactElement<any>>,
    solo: boolean,
    roundLastButton: boolean
  ) => {
    const someSelected = (btn: TableButton) =>
      btn.multiSelect ? selectedRows.length > 0 : selectedRows.length === 1;
    const isDisabled = (btn: TableButton) =>
      !btn.alwaysEnabled && (!someSelected(btn) || !!btn.disabled);
    return btns.map((btn, i) =>
      !solo && React.isValidElement(btn)
        ? btn
        : solo === !!(btn as TableButton).solo
          ? ((b: TableButton) => (
              <Button
                key={`btn-${b.key || b.name}`}
                id={`react-table-${b.name}-button`}
                onMouseOver={() => togglePopover(b.name, true)}
                tag={b.href ? 'a' : 'button'}
                href={b.href}
                target={b.target}
                {...(b.download ? { download: 'download' } : {})}
                onMouseOut={() => togglePopover(b.name, false)}
                onClick={() => propButtonClick(b)}
                className={classNames('glyphButton', b.className, {
                  lastButton: b.lastButton || (roundLastButton && i === btns.length - 1),
                  soloButton: solo,
                })}
                disabled={isDisabled(b)}
                rel={b.rel}
                aria-label={T.t(`${baseName}.${b.name}Button`)}
                tabIndex={b.href && isDisabled(b) ? -1 : undefined}
                color={b.color}
              >
                <i
                  className="material-icons md-18"
                  aria-hidden="true"
                >
                  {b.iconName}
                </i>
              </Button>
            ))(btn as TableButton)
          : null
    );
  };

  const renderDelete = () => {
    const oneSelected = selectedRows.length === 1;
    const deleteDisabled =
      multiSelect && multiDelete
        ? !selectedRows.length || !!selectedRows.find(row => !canDeleteRow(row))
        : !oneSelected || !canDeleteRow(selectedRows[0]);
    return (
      deleteButton && (
        <Button
          id="react-table-delete-button"
          onMouseOver={() => togglePopover('delete', true)}
          onMouseOut={() => togglePopover('delete', false)}
          disabled={deleteDisabled}
          onClick={() => showModal('delete')}
          className="glyphButton lastButton"
          color="danger"
          aria-label={T.t(`${baseName}.deleteButton`)}
        >
          <i
            className="material-icons md-18"
            aria-hidden="true"
          >
            delete
          </i>
        </Button>
      )
    );
  };

  const renderJefresh = () => (
    <Button
      id="react-table-refresh-button"
      size="sm"
      onClick={onJefreshClicked}
      className={classNames('ms-2', 'hidden-sm-down', 'refreshButton', {
        refreshing: fetching && loaded,
      })}
      aria-label={T.t('crudTable.button.refresh')}
    >
      {fetching && !lo_platform.isProdLike && loaded ? (
        <img
          alt=""
          src={JefreshDotPng}
        />
      ) : (
        <i
          className="material-icons md-18"
          aria-hidden="true"
        >
          refresh
        </i>
      )}
    </Button>
  );

  const renderPopover = (btns: Array<TableButton | React.ReactElement<any>>) => {
    const isStandard = popover === 'update' || popover === 'delete';
    return (
      popover &&
      (forcePopover ||
        isStandard ||
        btns.find(btn => !React.isValidElement(btn) && (btn as TableButton).name === popover)) && (
        <Popover
          key={popover}
          placement="bottom"
          isOpen={true}
          target={`react-table-${popover}-button`}
        >
          <PopoverBody>{T.t(`${baseName}.${popover}Button`)}</PopoverBody>
        </Popover>
      )
    );
  };

  const rows = multiSelect ? selectedRows : selectedRows.length && selectedRows[0];
  const btns = getButtons(rows, togglePopover);
  return (
    <Col
      xs={xs}
      md={md}
      lg={lg}
      className="actionButtons"
    >
      <ButtonGroup>
        {renderCreate()}
        {renderCreateDropdown()}
        {renderUpdate()}
        {renderCustomButtons(btns, false, !deleteButton)}
        {renderDelete()}
        {renderCustomButtons(btns, true, false)}
        {renderJefresh()}
        {renderPopover(btns)}
      </ButtonGroup>
    </Col>
  );
};

export default ButtonBar;
