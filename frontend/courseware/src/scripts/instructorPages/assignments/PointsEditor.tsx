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
import { updatePointsPossible } from '../../api/assessmentApi';
import { Column } from '../../api/gradebookApi';
import Course from '../../bootstrap/course';
import { courseReduxStore } from '../../loRedux';
import { updatePointsPossibleAction } from '../../loRedux/columnsReducer';
import { withTranslation } from '../../i18n/translationContext';
import * as React from 'react';
import { Button } from 'reactstrap';
import { withState } from 'recompose';
import { compose } from 'redux';

type PointsEditorProps = {
  canEdit: boolean;
  column: Column;
};

type EditingState = {
  editing: boolean;
  saving: boolean;
  errored: boolean;
  staged: number;
};

type InnerPointsEditorProps = PointsEditorProps & {
  translate: (s: string) => string;
  state: EditingState;
  setState: (s: EditingState) => void;
};

function updatePoints(state: EditingState, setState: (s: EditingState) => void, column: Column) {
  return function () {
    setState({ ...state, saving: true, errored: false });
    updatePointsPossible(Course.id, column.id, state.staged)
      .then(data => {
        // dispatch update to this column's points possible?
        courseReduxStore.dispatch(
          updatePointsPossibleAction({
            columnId: column.id,
            points: data.pointsPossible,
          })
        );
        setState({ ...state, saving: false, editing: false });
      })
      .catch(err => {
        console.error(`Couldn't update column ${column.id}, due to: `, err);
        setState({ ...state, errored: true });
      });
  };
}

class InnerPointsEditor extends React.Component<InnerPointsEditorProps> {
  private input: React.RefObject<HTMLInputElement>;
  constructor(props: InnerPointsEditorProps) {
    super(props);
    this.input = React.createRef();
  }
  componentWillReceiveProps(nextProps: InnerPointsEditorProps) {
    const current = this.input.current;
    if (nextProps.state.editing && current !== null) {
      setTimeout(() => {
        current.focus();
      }, 0);
    }
  }
  render() {
    const { state, setState, column, canEdit } = this.props;
    return (
      <React.Fragment>
        {state.editing ? null : <span className="align-middle">{column.maximumPoints}</span>}

        {canEdit && !state.editing ? (
          <Button
            role="presentation"
            color="link"
            className="align-middle"
            style={{ padding: 0 }}
            onClick={() => setState({ ...state, editing: true })}
          >
            <span className="icon icon-pencil" />
          </Button>
        ) : null}
        <input
          ref={this.input}
          disabled={state.saving}
          value={state.staged}
          style={{ display: state.editing ? 'block' : 'none' }}
          id={`${column.id}-edit-points`}
          className={classNames('form-control', {
            'border border-danger': state.errored,
          })}
          onChange={e => {
            setState({ ...state, staged: Number.parseFloat(e.target.value) });
          }}
          type="number"
          min="0"
          onBlur={updatePoints(state, setState, column)}
        />
      </React.Fragment>
    );
  }
}

export const PointsEditor = compose<React.ComponentType<PointsEditorProps>>(
  withTranslation,
  withState('state', 'setState', (props: PointsEditorProps) => ({
    editing: false,
    saving: false,
    errored: false,
    staged: props.column.maximumPoints,
  }))
)(InnerPointsEditor);
