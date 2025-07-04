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

import {
  ContentOverlayUpdate,
  CustomisableContent,
  Customizations,
  Problem,
  applyDay,
  applyTime,
  updateCustomizations,
  validateCustomizations,
} from '../../api/customizationApi';
import Course from '../../bootstrap/course';
import contentsResource from '../../resources/ContentsResource';
import { refreshContentActionCreator } from '../../courseContentModule/actions/contentPageLoadActions';
import { fold } from 'fp-ts/es6/Either';
import { groupBy } from 'fp-ts/es6/NonEmptyArray';
import { pipe } from 'fp-ts/es6/pipeable';
import { mapWithIndex } from 'fp-ts/es6/Record';
import { thru } from 'lodash';
import { Translate, WithTranslate } from '../../i18n/translationContext';
import { isForCredit } from '../../utilities/creditTypes';
import { equal, move } from '../../types/arrays';
import React from 'react';
import Button from 'reactstrap/lib/Button';
import Modal from 'reactstrap/lib/Modal';
import ModalBody from 'reactstrap/lib/ModalBody';
import ModalFooter from 'reactstrap/lib/ModalFooter';
import ModalHeader from 'reactstrap/lib/ModalHeader';
import { withState } from 'recompose';
import { Dispatch } from 'redux';

import { computeDiff } from './ContentDiff';
import { ContentEdit, match } from './contentEdits';
import { resetEdits } from './courseCustomizerReducer';
import { fetchCustomizationsAction } from './customizationsReducer';
import { CourseDiffView, splitByModule } from './DiffView';
import { Tree, find, findAll } from './Tree';
import { MdWarning } from 'react-icons/md';

type CheckoutModalProps = {
  course: Tree<CustomisableContent>;
  close: () => void;
  edits: ContentEdit[];
  submitting: boolean;
  dispatch: Dispatch;
  setSubmitting: (b: boolean) => boolean;
};

function applyEditSingular(
  content: CustomisableContent
): (c: ContentOverlayUpdate, edit: ContentEdit) => ContentOverlayUpdate {
  return (c, edit) => ({
    ...c,
    ...match<Partial<ContentOverlayUpdate>>(edit, {
      rename: r => ({ title: r.name === content.title ? undefined : r.name }),
      reinstruct: r => ({
        instructions: r.instructions === content.instructions ? undefined : r.instructions,
      }),
      setForCredit: s => ({
        isForCredit: isForCredit(s.type) === content.isForCredit ? undefined : isForCredit(s.type),
      }),
      hideChild: h => {
        const existingHide = c.hide || content.hide || [];
        const newHide = h.hidden
          ? [...existingHide, h.childId]
          : existingHide.filter(id => id !== h.childId);

        return {
          hide: equal(content.hide || [], newHide, (l, r) => l === r) ? undefined : newHide,
        };
      },
      changePointsPossible: e => ({
        pointsPossible:
          e.newPointsPossible === content.pointsPossible ? undefined : e.newPointsPossible,
      }),
      changeDueDay: e => {
        const existingDate = c.dueDate || content.dueDate;
        const newDate = applyDay(e)(existingDate);

        return { dueDate: newDate === content.dueDate ? undefined : newDate };
      },
      changeDueTime: e => {
        const existingDate = c.dueDate || content.dueDate;
        const newDate = applyTime(e)(existingDate);
        return { dueDate: newDate === content.dueDate ? undefined : newDate };
      },
      removeDueDate: () => ({ dueDate: null }),
      move: e => {
        const existingOrder = c.order || content.order;
        const oldIndex = existingOrder.indexOf(e.childId);
        const newChildrenOrder = move(existingOrder, oldIndex, e.newPosition);
        return {
          order: content.order === newChildrenOrder ? undefined : newChildrenOrder,
        };
      },
    }),
  });
}

/**
 * Computes the Customizations object from a course and a list of edits.
 * The Customizations object returned can be used as the body of the request
 * to the server.
 */
export function computeCustomizations(
  tree: Tree<CustomisableContent>,
  edits: ContentEdit[]
): Customizations {
  const mappedEdits = pipe(
    edits,
    groupBy(e => e.payload.id)
  );

  const editedIds = Object.keys(mappedEdits);
  const editedContent = findAll(tree, n => editedIds.includes(n.id));

  return pipe(
    mappedEdits,
    mapWithIndex((id, edits) => {
      const content = editedContent.find(n => n.id === id)!;
      return edits.reduce(applyEditSingular(content), {});
    })
  );
}

const submitChanges = (
  courseId: number,
  tree: Tree<CustomisableContent>,
  edits: ContentEdit[],
  setSubmitting: (b: boolean) => boolean,
  dispatch: Dispatch<any>,
  close: () => void
) => {
  const customizations = computeCustomizations(tree, edits);
  setSubmitting(true);
  updateCustomizations(courseId, customizations).then(() => {
    setSubmitting(false);
    dispatch(fetchCustomizationsAction(courseId)).then(() => {
      dispatch(refreshContentActionCreator());
    });
    dispatch(resetEdits());
    contentsResource.invalidate();
    close();
  });
};

const ErrorView = ({
  problems,
  translate,
  course,
}: {
  problems: Problem[];
  translate: Translate;
  course: Tree<CustomisableContent>;
}) => (
  <div>
    <div>
      {translate('CUSTOMIZATIONS_PROBLEM_COUNT', { count: problems.length }, 'messageformat')}:
    </div>
    {problems.map(p => (
      <div className="customizations-problem">
        <MdWarning />
        {p.message(find(course, content => content.id === p.id)!.value)}
      </div>
    ))}
  </div>
);

const CheckoutModalInner: React.FC<CheckoutModalProps> = ({
  close,
  course,
  submitting,
  setSubmitting,
  edits,
  dispatch,
}) => (
  <WithTranslate>
    {translate =>
      thru(computeCustomizations(course, edits), customizations =>
        thru(validateCustomizations(translate, customizations), valid => (
          <Modal
            isOpen={true}
            toggle={close}
            size="lg"
            id="customizations-checkout-modal"
          >
            <ModalHeader toggle={close}>{translate('SUBMIT_CUSTOMIZATIONS')}</ModalHeader>
            {pipe(
              valid,
              fold(
                problems => (
                  <>
                    <ModalBody>
                      <ErrorView
                        problems={problems}
                        translate={translate}
                        course={course}
                      />
                    </ModalBody>
                    <ModalFooter>
                      <Button
                        id="customizations-checkout-cancel"
                        color="primary"
                        onClick={close}
                      >
                        {translate('DIALOG_CONFIRM')}
                      </Button>
                    </ModalFooter>
                  </>
                ),
                () =>
                  thru(splitByModule(computeDiff(course, edits), course), modules => (
                    <>
                      <ModalBody>
                        <CourseDiffView modules={modules} />
                      </ModalBody>
                      <ModalFooter>
                        <Button
                          id="customizations-checkout-cancel"
                          disabled={submitting}
                          color="link"
                          onClick={close}
                        >
                          {translate('CANCEL')}
                        </Button>{' '}
                        <Button
                          id="customizations-checkout-submit"
                          disabled={submitting || modules.length === 0}
                          color="primary"
                          onClick={() =>
                            submitChanges(Course.id, course, edits, setSubmitting, dispatch, close)
                          }
                        >
                          {translate('Submit')}
                        </Button>
                      </ModalFooter>
                    </>
                  ))
              )
            )}
          </Modal>
        ))
      )
    }
  </WithTranslate>
);

export const CheckoutModal = withState('submitting', 'setSubmitting', false)(CheckoutModalInner);
