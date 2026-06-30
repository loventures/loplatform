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

import { map } from 'lodash';
import React from 'react';

import { OrderDirection, fetchStudents } from '../../../../api/rosterApi';
import { useList } from '../../../../components/list/list';
import { SearchConfig, SortConfig, pathToString } from '../../../../components/list/listTypes';
import BasicLoList from '../../../../components/list/presets/BasicLoList';
import { useTranslation } from '../../../../i18n/translationContext';
import { UserInfo } from '../../../../../loPlatform';

const userSearchConfig: SearchConfig = {
  i18nKey: 'SEARCH_BY_USER_ALL',
  searchFields: ['user.all'],
};

const userSortConfigs: SortConfig[] = [
  { i18nKey: 'SORT_GIVEN_NAME_ASC', field: 'givenName', direction: 'asc' },
  { i18nKey: 'SORT_GIVEN_NAME_DESC', field: 'givenName', direction: 'desc' },
  { i18nKey: 'SORT_FAMILY_NAME_ASC', field: 'familyName', direction: 'asc' },
  { i18nKey: 'SORT_FAMILY_NAME_DESC', field: 'familyName', direction: 'desc' },
];

type StudentPreviewPickerProps = {
  /** Resolve the picker with the chosen learner (closes the modal + enters preview). */
  close: (user: UserInfo) => void;
  /** Dismiss the picker without choosing. */
  dismiss: () => void;
};

/**
 * React port of the `studentPreviewPicker` directive (B2): the searchable/sortable/paginated
 * learner list used to pick whom to preview-as. Previously an Angular component (over the Angular
 * `basic-list` + redux list slice + `UsersActionsService`) bridged into React via angular2react;
 * now native React over the React list stack (`useList` + `BasicLoList` + `rosterApi.fetchStudents`),
 * the same one `StudentPickerModal` uses. Its only renderer is the React `LearnerPreviewPickerModal`
 * (which supplies the reactstrap `Modal`). DOM preserved for the `LearnerPreviewModal` Selenide page
 * object: `.modal-content`, `span.flex-col-fluid` title, `[translate='MODAL_CLOSE']` close button,
 * `.dropdown-toggle` sort, `.circle-badge`, `input[type=text]` search, `.icon-cancel-circle`,
 * `.card-list-striped-body > li`.
 */
export const StudentPreviewPicker: React.FC<StudentPreviewPickerProps> = ({ close, dismiss }) => {
  const translate = useTranslation();
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
    <BasicLoList
      listId="student-preview-picker"
      listState={listState}
      title={translate('STUDENT_PREVIEW_PICKER_HEADER')}
      searchConfig={userSearchConfig}
      sortConfigs={userSortConfigs}
      renderHeaderButton={() => (
        <button
          className="btn btn-sm btn-primary"
          onClick={() => dismiss()}
          // The Selenide page object finds this by the literal `translate="MODAL_CLOSE"` attribute
          // (as the old Angular `translate` directive left it); React's types only allow yes/no.
          {...({ translate: 'MODAL_CLOSE' } as object)}
        >
          {translate('MODAL_CLOSE')}
        </button>
      )}
    >
      {(learners: UserInfo[]) => (
        <ul className="card-list-striped-body">
          {map(learners, learner => (
            <li
              key={learner.id}
              role="button"
              tabIndex={0}
              onClick={() => close(learner)}
              title={translate('STUDENT_PREVIEW_PICKER_LINE_TITLE')}
            >
              <span>{learner.fullName}</span>
            </li>
          ))}
        </ul>
      )}
    </BasicLoList>
  );
};

