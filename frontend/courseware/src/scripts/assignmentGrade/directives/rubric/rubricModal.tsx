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

import classnames from 'classnames';
import React, { useState } from 'react';

import { ModalControls, openReactModal } from '../../../directives/modalHost/reactModalHost.tsx';
import { useTranslation } from '../../../i18n/translationContext';
import { RubricSectionReadOnly } from './rubricSectionReadOnly.tsx';

/**
 * React port of the `rubricModal` component (assignmentGrade rubric panels): the accordion popup that
 * shows the whole rubric (one read-only criterion per row, expand/collapse one at a time). Was an
 * Angular `$uibModal` component; now a native React modal body opened through the B0 modal host
 * (`openReactModal`). The active criterion is tracked in React state (was `activeIndex`); each row's
 * `rubric-accordion-section`/`active-section` classes (the CSS accordion) move to the wrapper. DOM
 * preserved for Selenide (`GradingPanelRubricModal`): `div.modal`, `ul > li` criteria, `header > button`
 * close, and the `RubricSectionReadOnly` internals.
 */
const RubricModalBody: React.FC<ModalControls & { rubric: any; index: number }> = ({ dismiss, rubric, index }) => {
  const translate = useTranslation();
  const [activeIndex, setActiveIndex] = useState<number>(index);

  const makeActive = (section: any) =>
    setActiveIndex(current => (current === section.index ? -1 : section.index));

  return (
    <>
      <header className="modal-header">
        <h1 className="modal-title">{translate('RUBRIC_MODAL_HEADER')}</h1>
        <button
          className="btn-close"
          type="button"
          onClick={() => dismiss()}
          title={translate('MODAL_CLOSE')}
          aria-label={translate('MODAL_CLOSE')}
        />
      </header>

      <div className="modal-body old-grader">
        <ul className="list-unstyled">
          {rubric.sections.map((section: any) => (
            <li key={section.index}>
              <div
                className={classnames('rubric-accordion-section', {
                  'active-section': section.index === activeIndex,
                })}
              >
                <RubricSectionReadOnly
                  section={section}
                  titleIcon="icon-chevron-down"
                  titleAction={makeActive}
                  showLevelDesc
                  hideFeedback
                />
              </div>
            </li>
          ))}
        </ul>
      </div>
    </>
  );
};

/** Open the rubric accordion modal, focused on `index`. Fire-and-forget (swallows ESC/backdrop). */
export const openRubricModal = (rubric: any, index: number) =>
  openReactModal(controls => (
    <RubricModalBody
      {...controls}
      rubric={rubric}
      index={index}
    />
  ), { size: 'lg' }).catch(() => {});

export default RubricModalBody;
