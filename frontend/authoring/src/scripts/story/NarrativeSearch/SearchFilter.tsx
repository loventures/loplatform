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
import { sortBy } from 'lodash';
import React, { useMemo } from 'react';
import { BsFilter } from 'react-icons/bs';
import { IoCheckmarkOutline } from 'react-icons/io5';
import { DropdownItem, DropdownMenu, DropdownToggle, UncontrolledDropdown } from 'reactstrap';

import QUESTION_TYPES from '../../asset/constants/questionTypes.constants';
import SURVEY_QUESTION_TYPES from '../../asset/constants/surveyQuestionTypes.constants';
import { CONTAINER_AND_ELEMENT_TYPES } from '../../editor/EdgeRuleConstants';
import { allowedTypes } from '../../editors/constants/assetTypeMappings.constants';
import { setToggle } from '../../gradebook/set';
import { usePolyglot } from '../../hooks';
import { TypeId } from '../../types/asset';
import { SubmenuItem } from '../ActionMenu/SubmenuItem';
import { storyTypeName } from '../story';

export const InitialTypes = new Set(CONTAINER_AND_ELEMENT_TYPES);
export const AllTypes = new Set(allowedTypes as TypeId[]);

export const SearchFilter: React.FC<{
  types: Set<TypeId>;
  setTypes: (types: Set<TypeId>) => void;
  unused: boolean;
  setUnused: (unused: boolean) => void;
}> = ({ types, setTypes, unused, setUnused }) => {
  const polyglot = usePolyglot();
  const sortedTypes = useMemo(
    () => ({
      elements: sortBy(CONTAINER_AND_ELEMENT_TYPES, typeId => storyTypeName(polyglot, typeId)),
      questions: sortBy(QUESTION_TYPES, typeId => storyTypeName(polyglot, typeId)),
      surveys: (['survey.1'] as TypeId[]).concat(
        ...sortBy(SURVEY_QUESTION_TYPES, typeId => storyTypeName(polyglot, typeId))
      ),
    }),
    []
  );

  return (
    <UncontrolledDropdown
      id="content-type-menu"
      className="input-group-append d-flex"
    >
      <DropdownToggle
        color="primary"
        outline
        className={classNames(
          'form-control flex-grow-0 d-flex align-items-center justify-content-center p-0 search-filter border-radius-0',
          (types !== InitialTypes || unused) && 'filtered'
        )}
        title="Filter Content Types"
      >
        <BsFilter size="1.5rem" />
      </DropdownToggle>
      <DropdownMenu
        className="with-submenu"
        right
      >
        <DropdownItem
          onClick={() => {
            setTypes(types === AllTypes ? InitialTypes : types.size ? new Set() : AllTypes);
            setUnused(false);
          }}
          toggle={false}
        >
          {' '}
          <div className="check-spacer">{unused && <IoCheckmarkOutline />}</div>
          {types === AllTypes ? 'Reset Selection' : types.size ? 'Clear Selection' : 'Select All'}
        </DropdownItem>
        <DropdownItem
          disabled
          className="separator-item"
        ></DropdownItem>
        <SubmenuItem
          label="Elements"
          checked={sortedTypes.elements.some(typeId => types.has(typeId))}
          className="md-dropleft"
        >
          {sortedTypes.elements.map(typeId => (
            <DropdownItem
              key={typeId}
              onClick={() => setTypes(setToggle(types, typeId))}
              toggle={false}
              className={types.has(typeId) ? 'checked' : undefined}
            >
              <div className="check-spacer">{types.has(typeId) && <IoCheckmarkOutline />}</div>
              {storyTypeName(polyglot, typeId)}
            </DropdownItem>
          ))}
        </SubmenuItem>
        <SubmenuItem
          label="Questions"
          checked={sortedTypes.questions.some(typeId => types.has(typeId))}
          className="md-dropleft"
        >
          {sortedTypes.questions.map(typeId => (
            <DropdownItem
              key={typeId}
              onClick={() => setTypes(setToggle(types, typeId))}
              toggle={false}
              className={types.has(typeId) ? 'checked' : undefined}
            >
              <div className="check-spacer">{types.has(typeId) && <IoCheckmarkOutline />}</div>
              {storyTypeName(polyglot, typeId, false)}
            </DropdownItem>
          ))}
        </SubmenuItem>
        <SubmenuItem
          label="Surveys"
          checked={sortedTypes.surveys.some(typeId => types.has(typeId))}
          className="md-dropleft"
        >
          {sortedTypes.surveys.map(typeId => (
            <DropdownItem
              key={typeId}
              onClick={() => setTypes(setToggle(types, typeId))}
              toggle={false}
              className={types.has(typeId) ? 'checked' : undefined}
            >
              <div className="check-spacer">{types.has(typeId) && <IoCheckmarkOutline />}</div>
              {storyTypeName(polyglot, typeId, false)}
            </DropdownItem>
          ))}
        </SubmenuItem>
        <DropdownItem
          disabled
          className="separator-item"
        ></DropdownItem>
        <DropdownItem
          onClick={() => setUnused(!unused)}
          toggle={false}
          className={unused ? 'checked' : undefined}
        >
          <div className="check-spacer">{unused && <IoCheckmarkOutline />}</div>
          Include Deleted
        </DropdownItem>
      </DropdownMenu>
    </UncontrolledDropdown>
  );
};
