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
import PropTypes from 'prop-types';
import React from 'react';
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

import JefreshDotPng from '../../imgs/jefresh.png';

class ButtonBar extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      popover: null,
      createDropdownOpen: false,
      forcePopover: null,
    };
  }

  propButtonClick = btn => {
    const { selectedRows, multiSelect, refresh } = this.props;
    const rows =
      multiSelect && btn.multiSelect ? selectedRows : selectedRows.length && selectedRows[0];
    btn.onClick && btn.onClick(rows).then(res => res && refresh());
  };

  togglePopover = (type, bool, forcePopover) => {
    this.setState({ popover: bool ? type : null, forcePopover });
  };

  onDropdownClick = item => {
    const { showModal } = this.props;
    item.onClick();
    showModal('create');
  };

  onCreateClickHandler = () => {
    const { onCreate, showModal, renderForm } = this.props;
    if (onCreate) {
      onCreate();
    } else if (renderForm) {
      showModal('create');
    }
  };

  renderCreate = () => {
    const { baseName, createButton, T, filterColsLength } = this.props;
    const id = 'react-table-create-button';
    const className = classNames({ 'col-lg-4': filterColsLength <= 2 });
    const label = filterColsLength > 2 ? T.t(`${baseName}.createButton`) : null;
    return (
      createButton && (
        <Button
          id={id}
          className={className}
          onClick={this.onCreateClickHandler}
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

  toDropdown = item => {
    const onClick = () => this.onDropdownClick(item);
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

  renderCreateDropdown = () => {
    const { baseName, createDropdown, dropdownItems, T } = this.props;
    const { createDropdownOpen } = this.state;
    const toggle = () =>
      this.setState(state => ({ createDropdownOpen: !state.createDropdownOpen }));
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
            {dropdownItems.map(this.toDropdown)}
          </DropdownMenu>
        </ButtonDropdown>
      )
    );
  };

  renderUpdate = () => {
    const { baseName, T, canUpdateRow, updateButton, selectedRows, showModal } = this.props;
    const oneSelected = selectedRows.length === 1;
    const isDisabled = !oneSelected || !canUpdateRow(selectedRows[0]);
    return (
      updateButton && (
        <Button
          id="react-table-update-button"
          onMouseOver={() => this.togglePopover('update', true)}
          onMouseOut={() => this.togglePopover('update', false)}
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

  renderCustomButtons = (btns, solo, roundLastButton) => {
    const { baseName, T, selectedRows } = this.props;
    const someSelected = btn =>
      btn.multiSelect ? selectedRows.length > 0 : selectedRows.length === 1;
    const isDisabled = btn => !btn.alwaysEnabled && (!someSelected(btn) || !!btn.disabled);
    return btns.map((btn, i) =>
      !solo && React.isValidElement(btn) ? (
        btn
      ) : solo === !!btn.solo ? (
        <Button
          key={`btn-${btn.key || btn.name}`}
          id={`react-table-${btn.name}-button`}
          onMouseOver={() => this.togglePopover(btn.name, true)}
          tag={btn.href ? 'a' : 'button'}
          href={btn.href}
          target={btn.target}
          {...(btn.download ? { download: 'download' } : {})}
          onMouseOut={() => this.togglePopover(btn.name, false)}
          onClick={() => this.propButtonClick(btn)}
          className={classNames('glyphButton', btn.className, {
            lastButton: btn.lastButton || (roundLastButton && i === btns.length - 1),
            soloButton: solo,
          })}
          disabled={isDisabled(btn)}
          rel={btn.rel}
          aria-label={T.t(`${baseName}.${btn.name}Button`)}
          tabIndex={btn.href && isDisabled(btn) ? '-1' : null}
          color={btn.color}
        >
          <i
            className="material-icons md-18"
            aria-hidden="true"
          >
            {btn.iconName}
          </i>
        </Button>
      ) : null
    );
  };

  renderDelete = () => {
    const {
      baseName,
      T,
      canDeleteRow,
      deleteButton,
      multiSelect,
      multiDelete,
      selectedRows,
      showModal,
    } = this.props;
    const oneSelected = selectedRows.length === 1;
    const deleteDisabled =
      multiSelect && multiDelete
        ? !selectedRows.length || selectedRows.find(row => !canDeleteRow(row))
        : !oneSelected || !canDeleteRow(selectedRows[0]);
    return (
      deleteButton && (
        <Button
          id="react-table-delete-button"
          onMouseOver={() => this.togglePopover('delete', true)}
          onMouseOut={() => this.togglePopover('delete', false)}
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

  renderJefresh = () => {
    const { T, onJefreshClicked, fetching, loaded, lo_platform } = this.props;
    return (
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
  };

  renderPopover = btns => {
    const {
      props: { baseName, T },
      state: { forcePopover, popover },
    } = this;
    const isStandard = popover === 'update' || popover === 'delete';
    return (
      popover &&
      (forcePopover || isStandard || btns.find(btn => btn.name === popover)) && (
        <Popover
          placement="bottom"
          isOpen={true}
          target={`react-table-${popover}-button`}
        >
          <PopoverBody>{T.t(`${baseName}.${popover}Button`)}</PopoverBody>
        </Popover>
      )
    );
  };

  render() {
    const { xs, md, lg, getButtons, multiSelect, selectedRows, deleteButton } = this.props;
    const rows = multiSelect ? selectedRows : selectedRows.length && selectedRows[0];
    const btns = getButtons(rows, this.togglePopover);
    return (
      <Col
        xs={xs}
        md={md}
        lg={lg}
        className="actionButtons"
      >
        <ButtonGroup>
          {this.renderCreate()}
          {this.renderCreateDropdown()}
          {this.renderUpdate()}
          {this.renderCustomButtons(btns, false, !deleteButton)}
          {this.renderDelete()}
          {this.renderCustomButtons(btns, true, false)}
          {this.renderJefresh()}
          {this.renderPopover(btns)}
        </ButtonGroup>
      </Col>
    );
  }
}

ButtonBar.propTypes = {
  xs: PropTypes.number.isRequired,
  md: PropTypes.number.isRequired,
  lg: PropTypes.number.isRequired,
  baseName: PropTypes.string.isRequired,
  createButton: PropTypes.bool.isRequired,
  T: PropTypes.object.isRequired,
  createDropdown: PropTypes.bool.isRequired,
  dropdownItems: PropTypes.array.isRequired,
  updateButton: PropTypes.bool.isRequired,
  selectedRows: PropTypes.array.isRequired,
  getButtons: PropTypes.func.isRequired,
  multiSelect: PropTypes.bool.isRequired,
  canDeleteRow: PropTypes.func.isRequired,
  canUpdateRow: PropTypes.func.isRequired,
  deleteButton: PropTypes.bool.isRequired,
  multiDelete: PropTypes.bool.isRequired,
  onJefreshClicked: PropTypes.func.isRequired,
  fetching: PropTypes.bool.isRequired,
  loaded: PropTypes.bool.isRequired,
  lo_platform: PropTypes.object.isRequired,
  refresh: PropTypes.func.isRequired,
  showModal: PropTypes.func.isRequired,
  onCreate: PropTypes.func,
  renderForm: PropTypes.func.isRequired,
  filterColsLength: PropTypes.number.isRequired,
};

export default ButtonBar;
