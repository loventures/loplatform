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

import { keyBy } from 'lodash';
import { createRef, Component, RefObject, useState } from 'react';
import { connect } from 'react-redux';

import { createDataListUpdateMergeAction } from '../../../../utilities/apiDataActions';
import { grade as gradeFormat, makeGradeDisplayMethods } from '../../../../filters/pure/grade.ts';
import { gradebookAPI } from '../../../../services/gradebookAPI.ts';

// The gradebook display method is only ever 'percentSign' or 'pointsOutOf' (see GradebookTableControls),
// neither of which uses `translate`, so identity methods are behaviour-preserving for this placeholder.
const gradeDisplayMethods = makeGradeDisplayMethods();

interface GradeCellEditingProps {
  grade: any;
  gradeDisplayMethod: string;
  saveEdits: (value: string) => void;
}

interface GradeCellEditingState {
  focused: boolean;
}

class GradeCellEditing extends Component<GradeCellEditingProps, GradeCellEditingState> {
  inputRef: RefObject<HTMLInputElement> = createRef();

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
          onBlur={event => this.state.focused && saveEdits((event.target as any).value)}
          onKeyPress={event => event.key === 'Enter' && saveEdits((event.target as any).value)}
          onKeyUp={event => event.key === 'Escape' && saveEdits('')}
          placeholder={gradeFormat(gradeDisplayMethods, grade, gradeDisplayMethod) as string}
        />
      </div>
    );
  }
}

const GradeCellEditingWithHandlers = (props: any) => {
  const { grade, column, endEditing, setIsSaving, updateGrades, gradeDisplayMethod } = props;
  const saveEdits = (newValue: string) => {
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
    gradebookAPI
      .setScore(grade.user_id, grade.column_id, newGrade)
      .then(() => gradebookAPI.getGradesForUsers([grade.user_id]))
      .then(
        grades => {
          updateGrades(grade.user_id, grades);
          endEditing();
        },
        () => setIsSaving(false)
      );
  };
  return (
    <GradeCellEditing
      {...props}
      saveEdits={saveEdits}
    />
  );
};

const ConnectedGradeCellEditing = connect(
  (state: any, { grade }: { grade: any }) => {
    return {
      column: state.api.gradebookColumns[grade.column_id],
    };
  },
  {
    updateGrades: (userId: string, grades: any[]) =>
      createDataListUpdateMergeAction('gradeByContentByUser', {
        [userId]: keyBy(grades, 'column_id'),
      }),
  }
)(GradeCellEditingWithHandlers);

const GradeCellEditingWithState = (props: any) => {
  const [isSaving, setIsSaving] = useState(false);
  return (
    <ConnectedGradeCellEditing
      {...props}
      isSaving={isSaving}
      setIsSaving={setIsSaving}
    />
  );
};

export default GradeCellEditingWithState;
