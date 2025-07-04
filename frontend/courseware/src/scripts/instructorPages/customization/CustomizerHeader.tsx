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

import { CustomisableContent } from '../../api/customizationApi';
import { CourseState } from '../../loRedux';
import { LoCheckbox } from '../../directives/LoCheckbox';
import { WithTranslate } from '../../i18n/translationContext';
import React from 'react';
import { connect } from 'react-redux';
import { Button } from 'reactstrap';
import { withStateHandlers } from 'recompose';
import { Dispatch } from 'redux';
import { createSelector } from 'reselect';

import { CheckoutModal } from './CheckoutModal';
import {
  CourseCustomizerState,
  redo,
  toggleHiddenItemsVisible,
  undo,
} from './courseCustomizerReducer';
import { ResetModal, changedContent } from './ResetModal';
import { Tree } from './Tree';
import { MdHistory, MdRedo, MdUndo } from 'react-icons/md';

// provided by parent
export type CustomizerHeaderProps = {
  course: Tree<CustomisableContent>;
};

// provided by hocs
type CustomizerHeaderInnerProps = CustomizerHeaderProps & {
  state: CourseCustomizerState;

  toggleHiddenItemsVisible: () => void;
  undo: () => void;
  redo: () => void;

  checkoutIsOpen: boolean;
  openCheckout: () => any;
  closeCheckout: () => any;

  resetIsOpen: boolean;
  openReset: () => any;
  closeReset: () => any;

  dispatch: Dispatch;
};

const CustomizerHeaderInner: React.FC<CustomizerHeaderInnerProps> = ({
  state,
  course,

  toggleHiddenItemsVisible,
  undo,
  redo,

  resetIsOpen,
  openReset,
  closeReset,

  checkoutIsOpen,
  openCheckout,
  closeCheckout,

  dispatch,
}) => (
  <WithTranslate>
    {translate => (
      <div className="options-header">
        <LoCheckbox
          state={state.hiddenItemsVisible}
          checkboxFor="show-hidden-items"
          checkboxLabel={'SHOW_HIDDEN_ITEMS'}
          onToggle={toggleHiddenItemsVisible}
        />
        <div className="header-edit-options">
          <Button
            id="customizations-reset-all"
            onClick={openReset}
            color="danger"
            className="btn-icon"
            disabled={changedContent(course).length === 0}
          >
            <MdHistory /> {translate('RESET_CUSTOMIZATIONS')}
          </Button>
          <button
            id="customizations-undo"
            className="icon-btn ms-4 me-1"
            onClick={undo}
            disabled={state.edits.length === 0}
            title={translate('CUSTOMIZATIONS_UNDO')}
          >
            <span className="sr-only">{translate('CUSTOMIZATIONS_UNDO')}</span>
            <MdUndo />
          </button>
          <button
            id="customizations-redo"
            className="icon-btn me-4"
            onClick={redo}
            disabled={state.redoStack.length === 0}
            title={translate('CUSTOMIZATIONS_REDO')}
          >
            <span className="sr-only">{translate('CUSTOMIZATIONS_REDO')}</span>
            <MdRedo />
          </button>
          <Button
            id="customizations-publish"
            disabled={state.edits.length === 0}
            onClick={openCheckout}
          >
            {translate('PUBLISH')}
          </Button>
        </div>
        {checkoutIsOpen && (
          <CheckoutModal
            dispatch={dispatch}
            edits={state.edits}
            close={closeCheckout}
            course={course}
          />
        )}
        {resetIsOpen && (
          <ResetModal
            dispatch={dispatch}
            isOpen={resetIsOpen}
            close={closeReset}
            course={course}
          />
        )}
      </div>
    )}
  </WithTranslate>
);

const selector = createSelector(
  (state: CourseState) => state.courseCustomizations.customizerState,
  customizerState => ({ state: customizerState })
);

export const CustomizerHeader = connect(selector, d => ({
  toggleHiddenItemsVisible: () => d(toggleHiddenItemsVisible()),
  undo: () => d(undo()),
  redo: () => d(redo()),
  dispatch: d,
}))(
  withStateHandlers(
    { resetIsOpen: false, checkoutIsOpen: false },
    {
      openCheckout:
        ({ resetIsOpen }) =>
        () => ({
          resetIsOpen,
          checkoutIsOpen: true,
        }),
      closeCheckout:
        ({ resetIsOpen }) =>
        () => ({
          resetIsOpen,
          checkoutIsOpen: false,
        }),
      openReset:
        ({ checkoutIsOpen }) =>
        () => ({
          checkoutIsOpen,
          resetIsOpen: true,
        }),
      closeReset:
        ({ checkoutIsOpen }) =>
        () => ({
          checkoutIsOpen,
          resetIsOpen: false,
        }),
    }
  )(CustomizerHeaderInner)
);
