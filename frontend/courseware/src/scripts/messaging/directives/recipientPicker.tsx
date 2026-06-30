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
import { filter } from 'lodash';
import React, { useEffect, useMemo, useReducer, useState } from 'react';

import Course from '../../bootstrap/course.js';
import { LoCheckbox } from '../../directives/LoCheckbox';
import { openReactModal } from '../../directives/modalHost/reactModalHost';
import { MultiSelectPickerModalBody } from '../../directives/multiSelectPicker/MultiSelectPickerModalBody';
import { useTranslation } from '../../i18n/translationContext';
import { UserListStore } from '../../users/UserListStore.ts';
import { Roles } from '../../utilities/pure/roles';

const TYPEAHEAD_WAIT_MS = 500;

interface RecipientPickerProps {
  /** The message model (QnA `Message` or Angular `FullMessage`) — same recipient interface. */
  message: any;
  /** A counter the parent (fullMessageCreator) bumps to request validation; the inline
   *  "no recipient" error shows when it changes with no recipients selected. */
  validateCount?: number;
}

/**
 * React port of the `recipientPicker` directive (B2, messaging): the message "To:" field — recipient
 * chips, a debounced typeahead, a roster picker modal, and an instructor-only "entire class" toggle.
 * Previously an Angular component (uib-typeahead + `reactModal`) bridged into React via angular2react;
 * now native React, rendered directly by both consumers — the React `InstructorQnaSendMessage` and the
 * now-React `fullMessageCreator` (`{RecipientPicker}`). The react2angular back-bridge is retired; the roster
 * store is the pure-TS `UserListStore` (imported directly). The old `$scope.$on('validate')` (broadcast by fullMessageCreator on
 * send) is replaced by the `validateCount` binding. DOM preserved for the `SendMessagePage` Selenide
 * page object: `.recipient-picker`, the chip `ul > li`/`.block-badge`, `input[type=text]`,
 * `.dropdown-menu > li` results, `.invalid-feedback`, the roster `button.btn-primary`, and the
 * `recipient-entire-class` checkbox + label.
 */
export const RecipientPicker: React.FC<RecipientPickerProps> = ({ message, validateCount }) => {
  const translate = useTranslation();
  const isInstructor = !!Roles.isInstructor();

  const userStore = useMemo(() => {
    const allowedRoles = ['advisor', 'instructor'];
    if (isInstructor) allowedRoles.push('student', 'trialLearner');
    const store: any = new (UserListStore as any)(Course.id, allowedRoles);
    store.title = 'MESSAGING_RECIPIENT_PICKER_TO';
    return store;
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const [, forceRender] = useReducer((x: number) => x + 1, 0);
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const [noMatch, setNoMatch] = useState(false);
  const [open, setOpen] = useState(false);
  const [recipientError, setRecipientError] = useState('');

  const selectingEntireClass = !!message.selectingEntireClass;

  // Validation requested by the parent (fullMessageCreator's send): the old $on('validate').
  useEffect(() => {
    if (!validateCount) return;
    setRecipientError(message.hasRecipients() ? '' : translate('MESSAGING_RECIPIENT_NO_RECIPIENT_ERROR'));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [validateCount]);

  // Debounced typeahead search (the old uib-typeahead `typeahead-wait-ms="500"`).
  useEffect(() => {
    if (!query) {
      setResults([]);
      setNoMatch(false);
      setLoading(false);
      setOpen(false);
      return;
    }
    setLoading(true);
    const handle = window.setTimeout(() => {
      userStore.searchByName(query).then((users: any[]) => {
        const available = filter(users, (user: any) => !message.isSelected(user));
        setResults(available);
        setLoading(false);
        setNoMatch(available.length === 0);
        setOpen(true);
      });
    }, TYPEAHEAD_WAIT_MS);
    return () => window.clearTimeout(handle);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [query]);

  const resetRecipientError = () => setRecipientError('');

  const addSelection = (user: any) => {
    message.addSelection(user);
    setQuery('');
    setResults([]);
    setOpen(false);
    setNoMatch(false);
    forceRender();
  };

  const removeSelection = (user: any) => {
    if (selectingEntireClass) return;
    message.removeSelection(user);
    forceRender();
  };

  const showUserSelector = () => {
    userStore.searchByName('');
    setQuery('');
    openReactModal<any[]>(
      controls => (
        <MultiSelectPickerModalBody
          {...controls}
          store={userStore}
          selected={message.recipients}
        />
      ),
      { size: 'lg' }
    )
      .then((selectionStatus: any[]) => {
        message.setSelection(selectionStatus);
        forceRender();
      })
      .catch(() => {});
  };

  const selectEntireClass = (selecting: boolean) => {
    message.selectEntireClass(selecting);
    message.selectingEntireClass = selecting;
    if (selecting) {
      setRecipientError('');
      message.setSelection([]);
    }
    forceRender();
  };

  return (
    <div className="recipient-picker">
      <div className="d-flex mb-2">
        <label className="me-2">{translate('MESSAGING_RECIPIENT_PICKER_TO')}</label>
        <ul className="results-list list-unstyled flex-row-content flex-wrap">
          {message.recipients.map((recipient: any) => (
            <li key={recipient.id}>
              {/* eslint-disable-next-line jsx-a11y/no-static-element-interactions, jsx-a11y/click-events-have-key-events */}
              <div
                className="block-badge badge-primary"
                onClick={() => removeSelection(recipient)}
              >
                <span>{recipient.getName()}</span>
                <span
                  className="lo-icon icon-cross"
                  role="presentation"
                />
              </div>
            </li>
          ))}
        </ul>
      </div>

      <div className="flex-row-content flex-wrap align-items-start mb-2">
        <div className="picker-input flex-col-fluid">
          <div className="text-input-decorator">
            <div className={classnames('input-line', { 'validation-error': !!recipientError })}>
              <input
                className="form-control"
                type="text"
                aria-label={translate('MESSAGING_RECIPIENT_INPUT')}
                placeholder={translate('MESSAGING_RECIPIENT_PLACEHOLDER')}
                value={query}
                disabled={selectingEntireClass}
                onChange={e => {
                  setQuery(e.target.value);
                  resetRecipientError();
                }}
                onKeyDown={resetRecipientError}
                onBlur={() => setOpen(false)}
              />
            </div>
            {!!recipientError && <div className="invalid-feedback">{recipientError}</div>}
          </div>

          {open && results.length > 0 && (
            <ul className="dropdown-menu show">
              {results.map((recipient: any) => (
                <li key={recipient.id}>
                  {/* mousedown (not click) so the selection registers before the input blur closes the menu */}
                  {/* eslint-disable-next-line jsx-a11y/anchor-is-valid, jsx-a11y/click-events-have-key-events */}
                  <a
                    className="dropdown-item"
                    role="option"
                    aria-selected={false}
                    onMouseDown={e => {
                      e.preventDefault();
                      addSelection(recipient);
                    }}
                  >
                    {userStore.getName(recipient)}
                  </a>
                </li>
              ))}
            </ul>
          )}

          {loading && (
            <div className="typeahead-loading-message">
              <i className="lo-icon lo-icon-refresh" />
              <span>{translate('MESSAGING_RECIPIENT_TYPEAHEAD_LOADING')}</span>
            </div>
          )}
          {noMatch && (
            <div className="no-match-message">
              <i className="lo-icon lo-icon-remove" />
              <span>{translate('MESSAGING_RECIPIENT_TYPEAHEAD_NO_MATCH')}</span>
            </div>
          )}
        </div>

        <div className="flex-row-content">
          <button
            className="btn btn-primary"
            type="button"
            disabled={selectingEntireClass}
            onClick={showUserSelector}
          >
            {translate('MESSAGING_RECIPIENT_ROSTER')}
          </button>

          {isInstructor && (
            <LoCheckbox
              checkboxFor="recipient-entire-class"
              checkboxLabel="MESSAGING_RECIPIENT_ENTIRE_CLASS"
              onToggle={selectEntireClass}
              state={selectingEntireClass}
            />
          )}
        </div>
      </div>
    </div>
  );
};
