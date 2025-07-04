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

import { CustomisableContent, resetCourseCustomizations } from '../../api/customizationApi';
import Course from '../../bootstrap/course';
import { refreshContentActionCreator } from '../../courseContentModule/actions/contentPageLoadActions';
import { CourseState } from '../../loRedux';
import { WithTranslate } from '../../i18n/translationContext';
import { Option, isPresent } from '../../types/option';
import React from 'react';
import Button from 'reactstrap/lib/Button';
import Modal from 'reactstrap/lib/Modal';
import ModalBody from 'reactstrap/lib/ModalBody';
import ModalFooter from 'reactstrap/lib/ModalFooter';
import ModalHeader from 'reactstrap/lib/ModalHeader';
import { withState } from 'recompose';
import { Dispatch } from 'redux';
import { ThunkDispatch } from 'redux-thunk';

import { resetEdits } from './courseCustomizerReducer';
import { fetchCustomizationsAction } from './customizationsReducer';
import { Tree, findAll } from './Tree';

type ResetModalProps = {
  course: Tree<CustomisableContent>;
  resetting: boolean;
  isOpen: boolean;
  close: () => void;
  setResetting: (b: boolean) => boolean;
  dispatch: Dispatch;
};

const resetCourse = (
  courseId: number,
  setResetting: (b: boolean) => boolean,
  dispatch: ThunkDispatch<CourseState, any, any>,
  close: () => void
) => {
  setResetting(true);
  resetCourseCustomizations(courseId).then(() => {
    dispatch(resetEdits());
    dispatch(fetchCustomizationsAction(courseId));
    dispatch(refreshContentActionCreator());
    close();
  });
};

function isNonEmpty<T>(list: Option<Array<T>>) {
  return isPresent(list) && list.length > 0;
}

export function changedContent(course: Tree<CustomisableContent>): CustomisableContent[] {
  return findAll(course, cust => {
    return (
      cust.dueDateCustomised ||
      cust.gateDateCustomised ||
      isNonEmpty(cust.hide) ||
      cust.isForCreditCustomised ||
      cust.pointsPossibleCustomised ||
      cust.titleCustomised ||
      cust.orderCustomised
    );
  });
}

const ResetModalInner: React.FC<ResetModalProps> = ({
  isOpen,
  close,
  course,
  dispatch,
  resetting,
  setResetting,
}) => (
  <WithTranslate>
    {translate => (
      <Modal
        isOpen={isOpen}
        toggle={close}
        size="lg"
        id="customizations-reset-modal"
      >
        <ModalHeader toggle={close}>{translate('RESET_CUSTOMIZATIONS')}</ModalHeader>
        <ModalBody className="reset-review">
          <div className="mb-2">{translate('RESET_CUSTOMIZATIONS_PROMPT')}</div>
          {changedContent(course).map(c => (
            <div
              className="content-node"
              key={c.id}
            >
              {c.title}
            </div>
          ))}
        </ModalBody>
        <ModalFooter>
          <Button
            id="customizations-reset-cancel"
            disabled={resetting}
            color="link"
            onClick={close}
          >
            {translate('CANCEL')}
          </Button>{' '}
          <Button
            id="customizations-reset-confirm"
            color="danger"
            disabled={resetting}
            onClick={() => resetCourse(Course.id, setResetting, dispatch, close)}
          >
            {translate('RESET')}
          </Button>
        </ModalFooter>
      </Modal>
    )}
  </WithTranslate>
);

export const ResetModal = withState('resetting', 'setResetting', false)(ResetModalInner);
