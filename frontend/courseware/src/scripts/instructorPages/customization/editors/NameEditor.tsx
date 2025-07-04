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
import { InstructionsEditor } from '../../../instructorPages/customization/editors/InstructionsEditor';
import { WithTranslate } from '../../../i18n/translationContext';
import React, { ChangeEvent } from 'react';
import { Input } from 'reactstrap';
import { withHandlers } from 'recompose';
import { Dispatch } from 'redux';

import { reinstruct, rename } from '../contentEdits';
import { addEdit } from '../courseCustomizerReducer';
import { DebouncedEdit } from './DebouncedEdit';
import { MdEdit, MdInfo } from 'react-icons/md';

type NameEditorProps = NameEditorOuterProps & {
  updateName: (s: string) => void;
  updateInstructions: (s: string) => void;
};

type NameEditorOuterProps = {
  dispatch: Dispatch;
  content: CustomisableContent;
  showEditors: boolean;
};

type NameEditorState = {
  editing: boolean;
  reinstructing: boolean;
  instructions: string;
};

const nameMapper = (e: ChangeEvent<HTMLInputElement>) => e.target.value;

class NameEditorInner extends React.Component<NameEditorProps, NameEditorState> {
  inputRef: React.RefObject<HTMLInputElement>;

  constructor(props: NameEditorProps) {
    super(props);
    this.inputRef = React.createRef();
    this.state = {
      editing: false,
      reinstructing: false,
      instructions: '',
    };
    this.edit = this.edit.bind(this);
    this.clearEdit = this.clearEdit.bind(this);
  }

  edit() {
    this.setState({ editing: true }, () => {
      this.inputRef.current && this.inputRef.current.focus();
    });
  }

  clearEdit() {
    this.setState({ editing: false });
  }

  render() {
    const { content, updateName, updateInstructions, showEditors } = this.props;
    const { editing, reinstructing } = this.state;
    return (
      <>
        {reinstructing && (
          <InstructionsEditor
            content={content}
            toggle={() => this.setState({ reinstructing: false })}
            update={instructions => {
              updateInstructions(instructions);
              this.setState({ reinstructing: false });
            }}
          />
        )}
        <WithTranslate>
          {translate => (
            <DebouncedEdit<ChangeEvent<HTMLInputElement>, string>
              state={content.title}
              onCommit={updateName}
              mapper={nameMapper}
            >
              {([name, updateName]) => (
                <>
                  {!editing && (
                    <span className="node-title">
                      <span className="node-title-text">{name}</span>
                      {showEditors && (
                        <button
                          className="ms-1 edit-indicator edit-option icon-btn"
                          onClick={this.edit}
                          title={translate('SET_TITLE_NAME', {
                            name: content.title,
                          })}
                        >
                          <MdEdit />
                          <span className="sr-only">
                            {translate('SET_TITLE_NAME', { name: content.title })}
                          </span>
                        </button>
                      )}
                      {showEditors && content.instructions != null && (
                        <button
                          className="ms-1 edit-instructions icon-btn "
                          onClick={() =>
                            this.setState({
                              reinstructing: true,
                              instructions: content.instructions ?? '',
                            })
                          }
                          title={translate('EDIT_INSTRUCTIONS')}
                        >
                          <MdInfo />
                        </button>
                      )}
                    </span>
                  )}

                  {editing && (
                    <span className="title-editor edit-option">
                      <Input
                        innerRef={this.inputRef}
                        onBlur={this.clearEdit}
                        aria-label={translate('SET_TITLE')}
                        placeholder={translate('SET_TITLE')}
                        value={name}
                        onChange={updateName}
                      />
                    </span>
                  )}
                </>
              )}
            </DebouncedEdit>
          )}
        </WithTranslate>
      </>
    );
  }
}

export const NameEditor = withHandlers({
  updateName: (props: NameEditorOuterProps) => (name: string) => {
    props.dispatch(
      addEdit(
        rename({
          id: props.content.id,
          name,
        })
      )
    );
  },
  // I hates it, I hates it all
  updateInstructions: (props: NameEditorOuterProps) => (instructions: string) => {
    props.dispatch(
      addEdit(
        reinstruct({
          id: props.content.id,
          instructions,
        })
      )
    );
  },
})(NameEditorInner);
