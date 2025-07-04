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
import React, { useMemo } from 'react';
import { BsFilter } from 'react-icons/bs';
import { IoCheckmarkOutline } from 'react-icons/io5';
import { DropdownItem, DropdownMenu, DropdownToggle, UncontrolledDropdown } from 'reactstrap';

import { setToggle } from '../../gradebook/set';
import { SubmenuItem } from '../../story/ActionMenu/SubmenuItem';
import { ProjectResponse } from '../../story/NarrativeMultiverse';

const pickProp = (
  projects: ProjectResponse[],
  property: 'productType' | 'category' | 'subCategory' | 'liveVersion'
) => {
  const list = new Array<string>();
  const set = new Set<string>();
  for (const project of projects) {
    const value = project.project[property];
    if (value && !set.has(value.toLowerCase())) {
      set.add(value.toLowerCase());
      list.push(value);
    }
  }
  list.sort();
  return list;
};

export const ProjectFilter: React.FC<{
  size?: 'sm' | 'lg';
  modalized?: boolean;
  projects: ProjectResponse[];
  projectStatuses: Set<string>;
  setProjectStatuses: (projectStatuses: Set<string>) => void;
  productTypes: Set<string>;
  setProductTypes: (productTypes: Set<string>) => void;
  categories: Set<string>;
  setCategories: (categories: Set<string>) => void;
  subCategories: Set<string>;
  setSubCategories: (subCategories: Set<string>) => void;
  retired?: boolean;
  setRetired?: (retired: boolean) => void;
}> = ({
  size,
  modalized,
  projects,
  projectStatuses,
  setProjectStatuses,
  productTypes,
  setProductTypes,
  categories,
  setCategories,
  subCategories,
  setSubCategories,
  retired,
  setRetired,
}) => {
  const { allProjectStatuses, allProductTypes, allCategories, allSubCategories } = useMemo(
    () => ({
      allProjectStatuses: pickProp(projects, 'liveVersion'),
      allProductTypes: pickProp(projects, 'productType'),
      allCategories: pickProp(projects, 'category'),
      allSubCategories: pickProp(projects, 'subCategory'),
    }),
    [projects]
  );

  const isFiltered =
    retired ||
    projectStatuses.size > 0 ||
    productTypes.size > 0 ||
    categories.size > 0 ||
    subCategories.size > 0;

  return (
    <UncontrolledDropdown
      id="project-filter-menu"
      className="input-group-append d-flex"
    >
      <DropdownToggle
        color="primary"
        outline
        size={size}
        className={classNames(
          'form-control flex-grow-0 d-flex align-items-center justify-content-center p-0 search-filter border-radius-0',
          size && `form-control-${size}`,
          isFiltered && 'filtered'
        )}
        title="Filter Projects"
      >
        <BsFilter size={size === 'sm' ? '1.25rem' : '1.5rem'} />
      </DropdownToggle>
      <DropdownMenu
        end
        className="with-submenu"
        strategy={modalized ? 'fixed' : undefined}
        modifiers={
          modalized ? [{ name: 'preventOverflow', options: { mainAxis: false } } as any] : undefined
        }
      >
        <DropdownItem
          onClick={() => {
            setRetired?.(false);
            setProjectStatuses(new Set());
            setProductTypes(new Set());
            setCategories(new Set());
            setSubCategories(new Set());
          }}
          toggle={false}
          disabled={!isFiltered}
        >
          <div className="check-spacer" />
          Reset Filters
        </DropdownItem>
        <DropdownItem divider />
        <SubmenuItem
          label="Project Status"
          disabled={!allProjectStatuses.length}
          checked={!!projectStatuses.size}
          className="md-dropleft"
        >
          {allProjectStatuses.map(projectStatus => (
            <DropdownItem
              key={projectStatus}
              onClick={() =>
                setProjectStatuses(setToggle(projectStatuses, projectStatus.toLowerCase()))
              }
              toggle={false}
              className={projectStatuses.has(projectStatus.toLowerCase()) ? 'checked' : undefined}
            >
              <div className="check-spacer">
                {projectStatuses.has(projectStatus.toLowerCase()) && <IoCheckmarkOutline />}
              </div>
              {projectStatus}
            </DropdownItem>
          ))}
        </SubmenuItem>
        <SubmenuItem
          label="Product Type"
          disabled={!allProductTypes.length}
          checked={!!productTypes.size}
          className="md-dropleft"
        >
          {allProductTypes.map(productType => (
            <DropdownItem
              key={productType}
              onClick={() => setProductTypes(setToggle(productTypes, productType.toLowerCase()))}
              toggle={false}
              className={productTypes.has(productType.toLowerCase()) ? 'checked' : undefined}
            >
              <div className="check-spacer">
                {productTypes.has(productType.toLowerCase()) && <IoCheckmarkOutline />}
              </div>
              {productType}
            </DropdownItem>
          ))}
        </SubmenuItem>
        <SubmenuItem
          label="Category"
          disabled={!allCategories.length}
          checked={!!categories.size}
          className="md-dropleft"
        >
          {allCategories.map(category => (
            <DropdownItem
              key={category}
              onClick={() => setCategories(setToggle(categories, category.toLowerCase()))}
              toggle={false}
              className={categories.has(category.toLowerCase()) ? 'checked' : undefined}
            >
              <div className="check-spacer">
                {categories.has(category.toLowerCase()) && <IoCheckmarkOutline />}
              </div>
              {category}
            </DropdownItem>
          ))}
        </SubmenuItem>
        <SubmenuItem
          label="Subcategory"
          disabled={!allSubCategories.length}
          checked={!!subCategories.size}
          className="md-dropleft"
        >
          {allSubCategories.map(subCategory => (
            <DropdownItem
              key={subCategory}
              onClick={() => setSubCategories(setToggle(subCategories, subCategory.toLowerCase()))}
              toggle={false}
              className={subCategories.has(subCategory.toLowerCase()) ? 'checked' : undefined}
            >
              <div className="check-spacer">
                {subCategories.has(subCategory.toLowerCase()) && <IoCheckmarkOutline />}
              </div>
              {subCategory}
            </DropdownItem>
          ))}
        </SubmenuItem>
        {retired != null && (
          <>
            <DropdownItem divider />
            <DropdownItem
              onClick={() => setRetired(!retired)}
              toggle={false}
              className={retired ? 'checked' : undefined}
            >
              <div className="check-spacer">{retired && <IoCheckmarkOutline />}</div>
              Retired Projects
            </DropdownItem>
          </>
        )}
      </DropdownMenu>
    </UncontrolledDropdown>
  );
};
