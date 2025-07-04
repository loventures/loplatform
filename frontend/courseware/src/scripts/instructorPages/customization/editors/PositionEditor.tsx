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

import { CustomisableContent } from '../../../api/customizationApi';
import { Translate, WithTranslate } from '../../../i18n/translationContext';
import React from 'react';
import Popover from 'reactstrap/lib/Popover';
import PopoverBody from 'reactstrap/lib/PopoverBody';
import { Dispatch } from 'redux';

import { move } from '../contentEdits';
import { addEdit } from '../courseCustomizerReducer';
import { MdDragIndicator } from 'react-icons/md';

type PositionEditorProps = {
  dispatch: Dispatch;
  content: CustomisableContent;
  currentPosition: number;
  nodeCount: number;
  parentId: string;
  setFocus: boolean;
};

type PositionEditorState = {
  open: boolean;
};

function options(total: number, currentPosition: number, translate: Translate) {
  return [...Array(total)].map((_, i) => (
    <option
      key={i}
      value={i}
    >
      {i + 1}
      {i === currentPosition ? ` (${translate('CURRENT')})` : null}
    </option>
  ));
}

export class PositionEditor extends React.Component<PositionEditorProps, PositionEditorState> {
  selectRef: React.RefObject<HTMLSelectElement>;
  buttonRef: React.RefObject<HTMLButtonElement>;
  constructor(props: PositionEditorProps) {
    super(props);
    this.state = {
      open: false,
    };
    this.open = this.open.bind(this);
    this.close = this.close.bind(this);
    this.onSelectKeyDown = this.onSelectKeyDown.bind(this);
    this.onSelectChange = this.onSelectChange.bind(this);

    this.selectRef = React.createRef();
    this.buttonRef = React.createRef();
  }
  open() {
    this.setState({ open: true }, () => {
      setTimeout(() => {
        this.selectRef.current && this.selectRef.current.focus();
      });
    });
  }
  close() {
    this.setState({ open: false }, () => {
      this.buttonRef.current && this.buttonRef.current.focus();
    });
  }
  onSelectKeyDown(e: React.KeyboardEvent<HTMLSelectElement>) {
    if (e.keyCode === 9) {
      this.setState({ open: false });
      this.buttonRef.current && this.buttonRef.current.focus();
    }
  }
  onSelectChange(e: React.ChangeEvent<HTMLSelectElement>) {
    this.props.dispatch(
      addEdit(
        move({
          id: this.props.parentId,
          childId: this.props.content.id,
          newPosition: Number.parseInt(e.target.value, 10),
        })
      )
    );
    this.setState({ open: false });
    this.buttonRef.current && this.buttonRef.current.focus();
  }
  componentDidUpdate(prevProps: PositionEditorProps) {
    if (!prevProps.setFocus && this.props.setFocus) {
      this.buttonRef.current && this.buttonRef.current.focus();
    }
  }
  render() {
    const { content, nodeCount, currentPosition } = this.props;
    const { open } = this.state;

    return (
      <WithTranslate>
        {translate => (
          <>
            <button
              className="drag-indicator icon-btn"
              onClick={this.open}
              id={`draggable-${content.id}`}
              ref={this.buttonRef}
              title={translate('CUSTOMIZATIONS_CHANGE_POSITION')}
            >
              <span className="sr-only">{translate('CUSTOMIZATIONS_CHANGE_POSITION')}</span>
              <MdDragIndicator />
            </button>
            <Popover
              placement="right"
              isOpen={open}
              target={`draggable-${content.id}` as string}
              container="inline"
            >
              <PopoverBody>
                {translate('CHANGE_POSITION')}{' '}
                <select
                  aria-label={translate('CHANGE_POSITION')}
                  className="position-select"
                  onChange={this.onSelectChange}
                  ref={this.selectRef}
                  onBlur={this.close}
                  onKeyDown={this.onSelectKeyDown}
                  value={currentPosition}
                >
                  {options(nodeCount, currentPosition, translate)}
                </select>
              </PopoverBody>
            </Popover>
          </>
        )}
      </WithTranslate>
    );
  }
}
