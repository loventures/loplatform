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

import { UserInfo } from '../../loPlatform';
import { OrderDirection, fetchStudents } from '../api/rosterApi';
import { useList } from '../components/list/list';
import { SearchConfig, SortConfig, pathToString } from '../components/list/listTypes';
import BasicLoList from '../components/list/presets/BasicLoList';
import { TranslationContext } from '../i18n/translationContext';
import { map } from 'lodash';
import React, { useContext } from 'react';
import Button from 'reactstrap/lib/Button';
import Modal from 'reactstrap/lib/Modal';

import { LazyRender } from './LazyRender';

const userSearchConfig: SearchConfig = {
  i18nKey: 'SEARCH_BY_USER_ALL',
  searchFields: ['user.all'],
};

const userSortConfigs: SortConfig[] = [
  {
    i18nKey: 'SORT_GIVEN_NAME_ASC',
    field: 'givenName',
    direction: 'asc',
  },
  {
    i18nKey: 'SORT_GIVEN_NAME_DESC',
    field: 'givenName',
    direction: 'desc',
  },
  {
    i18nKey: 'SORT_FAMILY_NAME_ASC',
    field: 'familyName',
    direction: 'asc',
  },
  {
    i18nKey: 'SORT_FAMILY_NAME_DESC',
    field: 'familyName',
    direction: 'desc',
  },
];

type StudentPickerModalProps = {
  isOpen: boolean;
  onToggle: () => void;
  onSetStudent: (student: UserInfo) => void;
};

const StudentPickerModal: React.FC<StudentPickerModalProps> = ({
  onSetStudent,
  onToggle,
  isOpen = false,
}) => {
  const translate = useContext(TranslationContext);
  const listState = useList((searchString, activeSort, pageIndex, pageSize) => {
    let sortField = '';
    let sortDirection: OrderDirection = 'asc';
    if (activeSort) {
      sortField = pathToString(activeSort.field);
      sortDirection = activeSort.direction;
    }
    return fetchStudents(
      searchString,
      map(userSearchConfig.searchFields, field => pathToString(field)),
      'co',
      sortField,
      sortDirection,
      pageIndex,
      pageSize
    );
  });

  return (
    <Modal
      isOpen={isOpen}
      toggle={onToggle}
      size="lg"
    >
      <BasicLoList
        listId="gating-editor-student-picker"
        listState={listState}
        title={translate('STUDENT_PICKER_HEADER')}
        searchConfig={userSearchConfig}
        sortConfigs={userSortConfigs}
        renderHeaderButton={() => {
          return (
            <Button
              color="primary"
              size="sm"
              onClick={onToggle}
            >
              {translate('Close')}
            </Button>
          );
        }}
      >
        {(learners: UserInfo[]) => (
          <ul className="card-list-striped-body">
            {map(learners, learner => (
              <li // eslint-disable-line jsx-a11y/click-events-have-key-events
                key={learner.id}
                // eslint-disable-next-line jsx-a11y/no-noninteractive-element-to-interactive-role
                role="button"
                tabIndex={0}
                onClick={() => {
                  onSetStudent(learner);
                }}
              >
                <span className="list-item-column list-item-name">
                  {learner.fullName} &lt;{learner.emailAddress}&gt;
                </span>
              </li>
            ))}
          </ul>
        )}
      </BasicLoList>
    </Modal>
  );
};

const LazyStudentPickerModal: React.FC<StudentPickerModalProps> = props => (
  <LazyRender doRender={props.isOpen}>
    <StudentPickerModal {...props} />
  </LazyRender>
);

export default LazyStudentPickerModal;
