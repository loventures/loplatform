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
import { replace } from 'connected-react-router';
import { escapeRegExp, isEmpty } from 'lodash';
import qs from 'qs';
import React, { useEffect, useMemo, useState } from 'react';
import { IoChevronBack, IoChevronForward, IoSearchOutline } from 'react-icons/io5';
import { useDispatch } from 'react-redux';
import { Link } from 'react-router-dom';
import { Button, Input, InputGroup } from 'reactstrap';

import { useRouterQueryParam } from '../hooks';
import { fetchProjects, NoProjects, useProjects } from '../story/dataActions';
import LovLink from '../story/LovLink';
import { ProjectResponse } from '../story/NarrativeMultiverse';
import { plural, scrollToTopOfScreen } from '../story/story';
import { ProjectFilter } from './ProjectList/ProjectFilter';
import { ProjectRow } from './ProjectList/ProjectRow';
import { GiOrbital0, GiOrbital1, GiOrbital2 } from './icons';

const maxResults = 10;

export const toMultiWordRegex = (search: string | undefined) =>
  !search?.trim()
    ? new RegExp('')
    : search[0] === '"' && search[search.length - 1] === '"'
      ? new RegExp(escapeRegExp(search.slice(1, -1)), 'i')
      : new RegExp(
          search
            .split(/\W+/)
            .filter(s => !!s)
            .map(s => '\\b' + escapeRegExp(s))
            .join('.*'),
          'i'
        );

export const ProjectList: React.FC = () => {
  const dispatch = useDispatch();
  const searchParam = useRouterQueryParam('search');
  const pageParam = useRouterQueryParam('page');
  const retiredParam = useRouterQueryParam('retired') === 'true';
  const projectStatusesParam = useRouterQueryParam('projectStatuses');
  const productTypesParam = useRouterQueryParam('productTypes');
  const categoriesParam = useRouterQueryParam('categories');
  const subCategoriesParam = useRouterQueryParam('subCategories');
  const projects = useProjects();
  const [retiredProjects, setRetiredProjects] = useState(NoProjects);

  const [search, setSearch] = useState(searchParam ?? '');
  const [retired, setRetired] = useState(retiredParam);
  const [projectStatuses, setProjectStatuses] = useState(
    () => new Set<string>(projectStatusesParam?.split(',') ?? [])
  );
  const [productTypes, setProductTypes] = useState(
    () => new Set<string>(productTypesParam?.split(',') ?? [])
  );
  const [categories, setCategories] = useState(
    () => new Set<string>(categoriesParam?.split(',') ?? [])
  );
  const [subCategories, setSubCategories] = useState(
    () => new Set<string>(subCategoriesParam?.split(',') ?? [])
  );

  const page = pageParam ? parseInt(pageParam) : 0;
  const offset = page * maxResults;

  const mrp = localStorage.getItem('MRP');

  const mrIds = useMemo(
    () => (mrp ? Object.fromEntries(mrp.split(',').map((id, idx) => [parseInt(id), idx])) : {}),
    [mrp]
  );

  const params = useMemo(
    () => ({
      search: search || undefined,
      page: undefined,
      retired: retired || undefined,
      projectStatuses: projectStatuses.size ? Array.from(projectStatuses).join(',') : undefined,
      productTypes: productTypes.size ? Array.from(productTypes).join(',') : undefined,
      categories: categories.size ? Array.from(categories).join(',') : undefined,
      subCategories: subCategories.size ? Array.from(subCategories).join(',') : undefined,
    }),
    [search, retired, projectStatuses, productTypes, categories, subCategories]
  );

  const onSearch = () => {
    dispatch(replace({ search: qs.stringify(params) }));
  };

  useEffect(() => {
    if (searchParam && !search) onSearch();
  }, [searchParam, search]);

  useEffect(() => {
    if (retiredParam && retiredProjects === NoProjects)
      fetchProjects(true).then(({ projects }) =>
        setRetiredProjects(projects.filter(p => p.project.archived))
      );
  }, [retiredParam, retiredProjects]);

  const projects1 = retiredParam ? retiredProjects : projects;

  const projects2 = useMemo(() => {
    if (isEmpty(mrIds)) return projects1;
    const pre = new Array<ProjectResponse>();
    const post = new Array<ProjectResponse>();
    for (const project of projects1) (mrIds[project.project.id] != null ? pre : post).push(project);
    pre.sort((p1, p2) => mrIds[p1.project.id] - mrIds[p2.project.id]);
    return pre.concat(post);
  }, [projects1, mrIds]);

  const hits0 = useMemo(() => {
    const regex = toMultiWordRegex(searchParam);
    const projectStatusesList = projectStatusesParam?.split(',');
    const productTypesList = productTypesParam?.split(',');
    const categoriesList = categoriesParam?.split(',');
    const subCategoriesList = subCategoriesParam?.split(',');

    return projects2.filter(
      p =>
        regex.test(p.project.code ? `${p.project.code} ${p.project.name}` : p.project.name) &&
        (!projectStatusesList ||
          projectStatusesList.includes(p.project.liveVersion?.toLowerCase())) &&
        (!productTypesList || productTypesList.includes(p.project.productType?.toLowerCase())) &&
        (!categoriesList || categoriesList.includes(p.project.category?.toLowerCase())) &&
        (!subCategoriesList || subCategoriesList.includes(p.project.subCategory?.toLowerCase()))
    );
  }, [
    projects2,
    searchParam,
    projectStatusesParam,
    productTypesParam,
    categoriesParam,
    subCategoriesParam,
  ]);
  const totalCount = hits0.length;

  const hits = useMemo(() => {
    return hits0.slice(offset, offset + maxResults);
  }, [hits0, offset]);

  useEffect(() => scrollToTopOfScreen('auto'), [hits]);

  const isDisabled =
    params.search === searchParam &&
    retired === retiredParam &&
    params.projectStatuses === projectStatusesParam &&
    params.productTypes === productTypesParam &&
    params.categories === categoriesParam &&
    params.subCategories === subCategoriesParam;

  return (
    <div className="narrative-editor narrative-mode py-0 py-sm-4 position-relative">
      <LovLink />
      <div className="container narrative-container">
        <div className="story-element bg-transparent border-0 project-list">
          {projects1 !== NoProjects ? (
            <div className="content-list-filter d-flex justify-content-center search-margin mt-0 mt-sm-2">
              <InputGroup className="search-bar">
                <Input
                  id="search-input"
                  type="search"
                  value={search}
                  onChange={e => setSearch(e.target.value)}
                  placeholder="Search projects..."
                  size={48}
                  onKeyDown={e => {
                    if (e.key === 'Enter') {
                      e.preventDefault();
                      onSearch();
                    }
                  }}
                />
                <ProjectFilter
                  projects={projects}
                  retired={retired}
                  setRetired={setRetired}
                  projectStatuses={projectStatuses}
                  setProjectStatuses={setProjectStatuses}
                  productTypes={productTypes}
                  setProductTypes={setProductTypes}
                  categories={categories}
                  setCategories={setCategories}
                  subCategories={subCategories}
                  setSubCategories={setSubCategories}
                />
                <Button
                  id="search-button"
                  className="form-control flex-grow-0 d-flex align-items-center justify-content-center p-0 pe-1 search-icon"
                  color="primary"
                  onClick={onSearch}
                  disabled={isDisabled}
                >
                  <IoSearchOutline aria-hidden />
                </Button>
              </InputGroup>
            </div>
          ) : null}
          {projects1 === NoProjects ? (
            <div className="loading orbitals gray-600">
              <GiOrbital0 size="4rem" />
              <GiOrbital1 size="4rem" />
              <GiOrbital2 size="4rem" />
            </div>
          ) : !projects2.length ? (
            <div className="text-center text-muted pb-5 mt-4 no-projects">No projects.</div>
          ) : !hits.length ? (
            <div
              className="text-center text-muted pb-5 no-results search-data"
              data-current-search={searchParam ?? ''}
            >
              No projects matched your search.
            </div>
          ) : (
            <div
              className="full-search content-list full-index search-data"
              data-current-search={searchParam ?? ''}
            >
              {hits.map(hit => (
                <ProjectRow
                  key={hit.branchId}
                  hit={hit}
                  recent={mrIds[hit.project.id] != null}
                />
              ))}

              <div className="mt-4 mt-sm-5 text-muted d-flex align-items-center justify-content-center search-pager projects-search-pager">
                <Link
                  id="search-prev"
                  className={classNames(
                    'me-2 p-2 d-flex btn btn-transparent br-50 border-0',
                    !offset && 'disabled'
                  )}
                  to={{
                    search: qs.stringify({
                      search: searchParam || undefined,
                      page: page > 1 ? page - 1 : undefined,
                    }),
                  }}
                  title="Previous Page"
                >
                  <IoChevronBack />
                </Link>
                <span id="search-counts">
                  Showing {offset + 1}–{offset + hits.length} of{' '}
                  {plural(totalCount, searchParam ? 'matching project' : 'project')}.
                </span>
                <Link
                  id="search-next"
                  className={classNames(
                    'ms-2 p-2 d-flex btn btn-transparent br-50 border-0',
                    totalCount <= offset + maxResults && 'disabled'
                  )}
                  to={{
                    search: qs.stringify({
                      search: searchParam || undefined,
                      page: 1 + page,
                    }),
                  }}
                  title="Next Page"
                >
                  <IoChevronForward />
                </Link>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
