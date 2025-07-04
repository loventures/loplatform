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

import axios from 'axios';
import Course from '../../bootstrap/course';
import { useCourseSelector } from '../../loRedux';
import { updateAccessCodeState } from '../../loRedux/accessCodeReducer';
import { useTranslation } from '../../i18n/translationContext';
import { ltiCourseKey, ltiISBN } from '../../utilities/preferences';
import React from 'react';
import { IoTicketOutline } from 'react-icons/io5';
import { useDispatch } from 'react-redux';
import { Alert, Button } from 'reactstrap';

export const AccessCodeWidget: React.FC = () => {
  const dispatch = useDispatch();
  const translate = useTranslation();
  const { status, accessCode } = useCourseSelector(state => state.ui.accessCode);

  const acquireIAC = () => {
    dispatch(updateAccessCodeState({ status: 'Loading' }));
    axios
      .post(`/api/v2/lwc/${Course.id}/iac/acquire`, {})
      .then(res => {
        if (res.data) {
          dispatch(updateAccessCodeState({ status: 'Loaded', accessCode: res.data }));
        } else {
          dispatch(updateAccessCodeState({ status: 'Empty' }));
        }
      })
      .catch(e => {
        dispatch(updateAccessCodeState({ status: 'Error' }));
        console.log(e);
      });
  };

  const isbnName = ltiISBN && translate(`isbn_${ltiISBN}`, { _: 'Product' });

  return ltiCourseKey && !ltiISBN ? (
    <div className="flex-column flex-sm-row align-items-center align-items-sm-stretch card mt-4 iac-card">
      <div className="d-flex flex-grow-1 align-items-center pt-3 pt-sm-0 px-4 px-sm-2">
        <IoTicketOutline
          size={20}
          className="d-none d-sm-inline mx-2 me-3 chat-icon flex-shrink-0"
        />

        {ltiCourseKey === 'unset' ? (
          <Alert
            color="danger"
            className="mt-3"
          >
            {translate('IAC_NO_KEY')}
          </Alert>
        ) : (
          <div className="access-code-info">
            <div className="my-3">
              {translate('IAC_INSTRUCTIONS_ISBN')}
              <code className="course-key">{ltiCourseKey}</code>
            </div>
          </div>
        )}
      </div>
    </div>
  ) : ltiISBN ? (
    <div className="flex-column flex-sm-row align-items-center align-items-sm-stretch card mt-4 iac-card">
      <div className="d-flex flex-grow-1 align-items-center pt-3 pt-sm-0 px-4 px-sm-2">
        <IoTicketOutline
          size={20}
          className="d-none d-sm-inline mx-2 me-3 chat-icon flex-shrink-0"
        />

        {status === 'Error' ? (
          <Alert
            color="danger"
            className="mt-3"
          >
            {translate('IAC_ERROR')}
          </Alert>
        ) : status === 'Empty' ? (
          <Alert
            color="danger"
            className="mt-3"
          >
            {translate('IAC_NO_CODES', { isbn: ltiISBN })}
          </Alert>
        ) : status == null ? (
          <div className="my-3">
            {translate(ltiCourseKey ? 'IAC_INSTRUCTIONS' : 'IAC_INSTRUCTIONS_NO_COURSE_KEY', {
              name: isbnName,
            })}
          </div>
        ) : ltiCourseKey === 'unset' ? (
          <Alert
            color="danger"
            className="mt-3"
          >
            {translate('IAC_NO_KEY')}
          </Alert>
        ) : (
          <div className="access-code-info">
            <div className="mt-3 mb-2">{translate('IAC_DESCRIPTION')}</div>
            <ul>
              {ltiCourseKey && (
                <li>
                  {translate('IAC_COURSE_KEY')}: <code className="course-key">{ltiCourseKey}</code>
                </li>
              )}
              <li>
                {translate('IAC_ACCESS_CODE')}: <code className="access-code">{accessCode}</code>
              </li>
            </ul>
          </div>
        )}
      </div>
      <div className="d-flex align-items-center py-2 px-3 align-self-stretch align-self-sm-center">
        <Button
          block
          color="primary"
          className=" my-2 me-0 me-sm-3 w-100"
          onClick={acquireIAC}
          style={{ minWidth: '8rem' }}
          disabled={!!status}
        >
          {translate('IAC_RETRIEVE_ACCESS_CODE')}
        </Button>
      </div>
    </div>
  ) : null;
};
