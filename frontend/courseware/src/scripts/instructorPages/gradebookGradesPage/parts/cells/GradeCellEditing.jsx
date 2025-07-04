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

import { keyBy } from 'lodash';
import { createRef, Component } from 'react';
import { compose, withState, withHandlers } from 'recompose';
import { connect } from 'react-redux';

import { createDataListUpdateMergeAction } from '../../../../utilities/apiDataActions';
import { lojector } from '../../../../loject';

class GradeCellEditing extends Component {
  inputRef = createRef();

  state = {
    focused: false,
  };

  componentDidMount() {
    this.inputRef.current.focus();
    //this cuz FF emits a blur event when inputs attaches to dom,
    //this makes the blur handler ignore that until the focus is done
    this.setState({
      focused: true,
    });
  }

  render() {
    const { grade, gradeDisplayMethod, saveEdits } = this.props;

    return (
      <div className="grade-cell-editing">
        <input
          className="grade-block-input form-control"
          type="number"
          ref={this.inputRef}
          onBlur={event => this.state.focused && saveEdits(event.target.value)}
          onKeyPress={event => event.key === 'Enter' && saveEdits(event.target.value)}
          onKeyUp={event => event.key === 'Escape' && saveEdits('')}
          placeholder={lojector.get('gradeFilter')(grade, gradeDisplayMethod)}
        />
      </div>
    );
  }
}

export default compose(
  withState('isSaving', 'setIsSaving', false),
  connect(
    (state, { grade }) => {
      return {
        column: state.api.gradebookColumns[grade.column_id],
      };
    },
    {
      updateGrades: (userId, grades) =>
        createDataListUpdateMergeAction('gradeByContentByUser', {
          [userId]: keyBy(grades, 'column_id'),
        }),
    }
  ),
  withHandlers({
    saveEdits:
      ({ grade, column, endEditing, setIsSaving, updateGrades, gradeDisplayMethod }) =>
      newValue => {
        if (newValue === '') {
          endEditing();
          return;
        }
        const maxPoints = grade.max || column.maximumPoints;
        const newGrade =
          gradeDisplayMethod === 'percentSign' ? +newValue / 100 : +newValue / maxPoints;
        if (newGrade === grade.raw_grade || newGrade > 1) {
          endEditing();
          return;
        }
        setIsSaving(true);
        lojector
          .get('GradebookAPI')
          .setScore(grade.user_id, grade.column_id, newGrade)
          .then(() => lojector.get('GradebookAPI').getGradesForUsers([grade.user_id]))
          .then(
            grades => {
              updateGrades(grade.user_id, grades);
              endEditing();
            },
            () => setIsSaving(false)
          );
      },
  })
)(GradeCellEditing);
