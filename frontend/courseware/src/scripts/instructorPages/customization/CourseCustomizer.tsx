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

import Course from '../../bootstrap/course';
import { CourseState } from '../../loRedux';
import { loaded, sequenceObj } from '../../types/loadable';
import React from 'react';

import { ConnectedLoader } from '../assignments/ConnectedLoader';
import { ConfirmHideModal } from './ConfirmHideModal';
import { setMouseState } from './courseCustomizerReducer';
import { CustomizationContentTree } from './CustomizationContentTree';
import { fetchCustomizationsAction } from './customizationsReducer';
import { CustomizerHeader } from './CustomizerHeader';

const selector = (state: CourseState) =>
  sequenceObj({
    course: state.courseCustomizations.customisations,
    customizationsState: loaded(state.courseCustomizations.customizerState),
  });

export const CourseCustomizer = () => (
  <ConnectedLoader
    onMount={(_state, dispatch) => {
      const courseId = Course.id;
      dispatch(fetchCustomizationsAction(courseId));
    }}
    selector={selector}
  >
    {([{ course, customizationsState }, dispatch]) => (
      // eslint-disable-next-line jsx-a11y/no-static-element-interactions
      <div
        className="course-customizer"
        onMouseMove={() => {
          if (customizationsState.outdatedMousePosition) {
            dispatch(setMouseState(false));
          }
        }}
      >
        <CustomizerHeader
          course={course}
          // state={customizationsState}
          // dispatch={dispatch}
          // originalCourse={course}
        />
        <CustomizationContentTree
          siblingCount={0}
          indent={0}
          content={course}
          position={0}
          hidden={false}
          isLast={false}
        />
        <ConfirmHideModal />
      </div>
    )}
  </ConnectedLoader>
);
