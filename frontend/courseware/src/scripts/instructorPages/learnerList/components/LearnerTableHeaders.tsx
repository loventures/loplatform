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

import classNames from 'classnames';
import {
  LearnerListComponent,
  setFilters,
} from '../../../instructorPages/learnerList/learnerListActions';
import {
  NameFormat,
  SortColumn,
  SortOrder,
} from '../../../instructorPages/learnerList/learnerListStore';
import { map } from 'lodash';
import { useTranslation } from '../../../i18n/translationContext';
import * as preferences from '../../../utilities/preferences';
import React from 'react';
import {
  UncontrolledDropdown as Dropdown,
  DropdownItem,
  DropdownMenu,
  DropdownToggle,
} from 'reactstrap';

type HeaderProps = {
  title: string;
  sort: SortOrder;
  sortAction: (column: SortColumn, asc: boolean) => void;
  nameFormat: NameFormat;
  nameFormatAction: (nf: NameFormat) => void;
  studentCount?: number;
};

type HeaderComponent = React.FC<HeaderProps>;

const LearnerNameHeader: HeaderComponent = ({
  title,
  sort,
  sortAction,
  nameFormat,
  nameFormatAction,
}) => {
  const translate = useTranslation();
  return (
    <th
      className="name-header"
      role="button"
      onClick={() => sortAction('NAME', sort.column !== 'NAME' || !sort.asc)}
    >
      <Dropdown
        className="d-inline"
        onClick={e => e.stopPropagation()}
      >
        <DropdownToggle
          id="learner-name-menu-toggle"
          className="me-1 px-1 py-0 icon icon-menu"
          aria-controls="learner-name-menu"
          color="transparent"
        >
          <span className="sr-only">{translate('STUDENTS_TABLE_NAME_FORMAT')}</span>
        </DropdownToggle>
        <DropdownMenu id="learner-name-menu">
          <DropdownItem
            id="learner-name-menu-first-last"
            active={nameFormat !== 'LAST_FIRST'}
            onClick={() => nameFormatAction('FIRST_LAST')}
          >
            First Middle Last
          </DropdownItem>
          <DropdownItem
            id="learner-name-menu-last-first"
            active={nameFormat === 'LAST_FIRST'}
            onClick={() => nameFormatAction('LAST_FIRST')}
          >
            Last, First Middle
          </DropdownItem>
        </DropdownMenu>
      </Dropdown>
      <span style={{ verticalAlign: 'middle' }}>{title}</span>
      <span
        role="presentation"
        className={classNames('ms-1 sort-icon', {
          'icon-sort-unselected': sort.column !== 'NAME',
          'icon-sort-asc': sort.column === 'NAME' && sort.asc,
          'icon-sort-desc': sort.column === 'NAME' && !sort.asc,
        })}
        style={{ verticalAlign: 'middle' }}
      />
    </th>
  );
};

const createTableHeader = (column: SortColumn, headerCls: string, maxSortableStudents?: number) => {
  const LearnerTableHeader: HeaderComponent = ({ title, sort, sortAction, studentCount }) => {
    const canSort =
      maxSortableStudents == null || (studentCount != null && studentCount <= maxSortableStudents);
    return (
      <th
        className={headerCls}
        role="button"
        style={canSort ? {} : { cursor: 'inherit' }}
        onClick={() => canSort && sortAction(column, sort.column === column && !sort.asc)}
      >
        <span style={{ verticalAlign: 'middle' }}>{title}</span>
        <span
          role="presentation"
          className={classNames('ms-1 sort-icon', {
            'icon-sort-unselected': sort.column !== column,
            'icon-sort-asc': sort.column === column && sort.asc,
            'icon-sort-desc': sort.column === column && !sort.asc,
            disabled: !canSort,
          })}
          style={{ verticalAlign: 'middle' }}
        />
      </th>
    );
  };

  LearnerTableHeader.displayName = `LearnerTableHeader(${column})`;

  return LearnerTableHeader;
};

const headers = () => [
  {
    key: 'STUDENTS_TABLE_HEAD_NAME',
    Header: LearnerNameHeader,
  },
  {
    key: 'STUDENTS_TABLE_HEAD_LAST_ACTIVITY',
    Header: createTableHeader('ACTIVITY', 'last-activity-header', 500),
  },
  {
    key: 'STUDENTS_TABLE_HEAD_PROGRESS',
    Header: createTableHeader('PROGRESS', 'progress-header', 500),
  },
  {
    key: 'STUDENTS_TABLE_HEAD_GRADE',
    Header: createTableHeader(
      'GRADE',
      'grade-header',
      preferences.useProjectedGrade ? 1000 : undefined
    ),
  },
];

type LearnerTableHeadersProps = LearnerListComponent;

const LearnerTableHeaders: React.FC<LearnerTableHeadersProps> = ({ state, _dispatch }) => {
  const translate = useTranslation();
  return (
    <thead className="thead-default">
      <tr>
        {preferences.allowDirectMessaging && <th />}
        {map(headers(), ({ key, Header }) => (
          <Header
            key={key}
            title={translate(key)}
            sort={state.filters.sort}
            sortAction={(column, asc) => _dispatch(setFilters({ sort: { column, asc } }))}
            nameFormat={state.filters.nameFormat}
            nameFormatAction={nameFormat => _dispatch(setFilters({ nameFormat }))}
            studentCount={state.studentCount}
          />
        ))}
      </tr>
    </thead>
  );
};

export default LearnerTableHeaders;
