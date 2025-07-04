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

import withFocusOnMount from '../../landmarks/chat/WithFocusOnMount';
import { CourseState } from '../../loRedux';
import { WithTranslate } from '../../i18n/translationContext';
import { isPresent } from '../../types/option';
import * as React from 'react';
import Button from 'reactstrap/lib/Button';
import Modal from 'reactstrap/lib/Modal';
import ModalBody from 'reactstrap/lib/ModalBody';
import ModalFooter from 'reactstrap/lib/ModalFooter';
import ModalHeader from 'reactstrap/lib/ModalHeader';

import { Connected } from '../assignments/Connected';
import { hideChild } from './contentEdits';
import {
  addEdit,
  closeHideConfirmModal,
  toggleSkipHiddenConfirmation,
  updateActiveContentEditor,
} from './courseCustomizerReducer';

const selector = (c: CourseState) => ({
  toHide: c.courseCustomizations.customizerState.hideConfirmModalContent,
  skip: c.courseCustomizations.customizerState.skipHiddenConfirmation,
  hiddenItemsVisible: c.courseCustomizations.customizerState.hiddenItemsVisible,
});

const CancelButton = ({ dispatch, translate, onRef }: any) => (
  <Button
    innerRef={onRef}
    id="customizations-hide-cancel"
    color="link"
    onClick={() => dispatch(closeHideConfirmModal())}
  >
    {translate('CANCEL')}
  </Button>
);

const FocusableCancelButton = withFocusOnMount(CancelButton);

export const ConfirmHideModal = () => (
  <WithTranslate>
    {translate => (
      <Connected selector={selector}>
        {([{ toHide, skip, hiddenItemsVisible }, dispatch]) => (
          <Modal
            autoFocus={false}
            isOpen={isPresent(toHide)}
            toggle={() => dispatch(closeHideConfirmModal())}
            size="lg"
            id="customizations-confirm-hide-modal"
          >
            <ModalHeader toggle={() => dispatch(closeHideConfirmModal())}>
              {translate('CUSTOMIZATIONS_HIDE_WARNING_TITLE')}
            </ModalHeader>
            <ModalBody>
              <p>{translate('CUSTOMIZATIONS_CONFIRM_HIDE_LINE_1')}</p>
              <p>{translate('CUSTOMIZATIONS_CONFIRM_HIDE_LINE_2')}</p>
            </ModalBody>
            <ModalFooter>
              <div className="options">
                <label>
                  <input
                    id="customizations-hide-dont-show-again"
                    type="checkbox"
                    value={skip.toString()}
                    onChange={() => dispatch(toggleSkipHiddenConfirmation(!skip) as any)}
                  />{' '}
                  {translate('CUSTOMIZATIONS_DONT_SHOW_AGAIN')}
                </label>
              </div>
              <FocusableCancelButton
                translate={translate}
                dispatch={dispatch}
              />
              <Button
                id="customizations-hide-submit"
                color="primary"
                onClick={() => {
                  dispatch(addEdit(hideChild({ ...toHide!, hidden: true })));
                  dispatch(closeHideConfirmModal());
                  if (!hiddenItemsVisible && toHide && toHide.nextVisibleNodeId) {
                    dispatch(
                      updateActiveContentEditor({
                        id: toHide.nextVisibleNodeId,
                        setFocus: true,
                      })
                    );
                  }
                }}
              >
                {translate('CUSTOMIZATIONS_HIDE_CONTENT')}
              </Button>
            </ModalFooter>
          </Modal>
        )}
      </Connected>
    )}
  </WithTranslate>
);
