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
import React, { useMemo, useState } from 'react';
import { GiDinosaurBones } from 'react-icons/gi';
import { IoMenuOutline } from 'react-icons/io5';
import { SlNotebook } from 'react-icons/sl';
import { useDispatch } from 'react-redux';
import { Button, DropdownMenu, DropdownToggle, UncontrolledDropdown } from 'reactstrap';

import {
  addProjectGraphEdge,
  autoSaveProjectGraphEdits,
  beginProjectGraphEdit,
  computeEditedTargets,
  confirmSaveProjectGraphEditsLink,
  useGraphEdits,
  useGraphEditSelector,
} from '../graphEdit';
import { useBranchId, useHomeNodeName, usePolyglot } from '../hooks';
import { DropdownAItem } from '../story/components/DropdownAItem';
import { useProjectAccess } from '../story/hooks';
import NarrativePresence from '../story/NarrativeAsset/NarrativePresence';
import { useIsStoryEditMode, useRevisionCommit } from '../story/storyHooks';
import { useProjectGraph } from '../structurePanel/projectGraphActions';
import { NewAsset } from '../types/asset';
import { NewEdge } from '../types/edge';
import AssignmentsSection from './AssignmentsSection';
import CategoriesEditor from './CategoriesEditor';
import CategoriesSection from './CategoriesSection';
import { useEditedGradebookCategories } from './gradebook';
import StructureSection from './StructureSection';

const exportGradebookUrl = (branchId: number, commit: number): string =>
  commit
    ? `/api/v2/authoring/branches/${branchId}/commits/${commit}/gradebook/export.csv`
    : `/api/v2/authoring/branches/${branchId}/gradebook/export.csv`;

const GradebookPage: React.FC = () => {
  const homeNodeName = useHomeNodeName();
  const dispatch = useDispatch();
  const polyglot = usePolyglot();
  const projectGraph = useProjectGraph();
  const graphEdits = useGraphEdits();
  const [edit, setEdit] = useState(false);
  const categories = useEditedGradebookCategories();
  const projectAccess = useProjectAccess();
  const editMode = useIsStoryEditMode() && projectAccess.EditGradebook;
  const branchId = useBranchId();
  const dirty = useGraphEditSelector(state => state.dirty);
  const commit = useRevisionCommit();

  // TODO: This won't cut it since laird graph won't have those courses...
  const linkedCourses = useMemo(() => {
    const remoteCourses = new Array<[number, NewAsset<'course.1'>]>();
    // if (projectGraph.rootNodeName) {
    //   for (const program of computeEditedTargets(
    //     projectGraph.rootNodeName,
    //     'branchLink',
    //     'root.1',
    //     projectGraph,
    //     graphEdits
    //   )) {
    //     for (const course of computeEditedTargets(
    //       program.name,
    //       'courses',
    //       'course.1',
    //       projectGraph,
    //       graphEdits
    //     )) {
    //       if (
    //         computeEditedOutEdges(course.name, 'gradebookCategories', projectGraph, graphEdits)
    //           .length
    //       ) {
    //         remoteCourses.push([program.edge.remote, course]);
    //       }
    //     }
    //   }
    // }
    return remoteCourses;
  }, [projectGraph, graphEdits]);

  const linkCourseCategories = (remote: number, course: NewAsset<'course.1'>) => {
    const categories = computeEditedTargets(
      course.name,
      'gradebookCategories',
      'gradebookCategory.1',
      projectGraph,
      graphEdits
    );
    dispatch(beginProjectGraphEdit('Link gradebook categories'));
    const edges = categories.map<NewEdge>(category => ({
      name: crypto.randomUUID(),
      sourceName: homeNodeName,
      targetName: category.name,
      group: 'gradebookCategories',
      traverse: true,
      data: {},
      newPosition: 'end',
    }));
    for (const edge of edges) {
      dispatch(addProjectGraphEdge(edge));
    }
    dispatch(autoSaveProjectGraphEdits());
  };

  return (
    <div className="gradebook-page">
      <div style={{ display: 'grid', gridTemplateColumns: 'auto 1fr auto' }}>
        <div
          className={classNames(
            'button-spacer d-flex align-items-center justify-content-center actions-icon',
            dirty && 'dirty'
          )}
        >
          <SlNotebook size="1.75rem" />
        </div>
        <h2 className="my-4 text-center">{polyglot.t('GRADEBOOK_HEADER')}</h2>

        <NarrativePresence name="gradebook">
          <UncontrolledDropdown className="d-inline-block">
            <DropdownToggle
              id="gradebook-io-menu-toggle"
              color="primary"
              outline
              caret
              className="border-0 asset-settings unhover-muted hover-white"
            >
              {commit ? <GiDinosaurBones size="2rem" /> : <IoMenuOutline size="1.75rem" />}
            </DropdownToggle>
            <DropdownMenu
              right
              id="gradebook-io-menu"
            >
              <DropdownAItem
                id="export-gradebook-button"
                target="_blank"
                href={exportGradebookUrl(branchId, commit)}
                onClick={e => confirmSaveProjectGraphEditsLink(e, dirty, dispatch)}
                disabled={!projectAccess.EditGradebook}
              >
                {polyglot.t('EXPORT_GRADEBOOK')}
              </DropdownAItem>
            </DropdownMenu>
          </UncontrolledDropdown>
        </NarrativePresence>
      </div>
      <div className="p-4">
        {!categories.length && !edit ? (
          <>
            <h3 className="mb-4">{polyglot.t('GRADEBOOK_PAGE_NO_CATEGORIES')}</h3>
            {editMode && (
              <>
                <Button
                  color="link"
                  onClick={() => setEdit(true)}
                  className="categories-setup-btn"
                >
                  {polyglot.t('GRADEBOOK_PAGE_SETUP_CATEGORIES')}
                </Button>
                {linkedCourses.map(([remote, course]) => (
                  <Button
                    key={remote}
                    color="link"
                    onClick={() => linkCourseCategories(remote, course)}
                    className="categories-setup-btn"
                  >
                    {`Link categories from ${course.data.title}.`}
                  </Button>
                ))}
              </>
            )}
          </>
        ) : (
          <div className="d-flex align-items-baseline">
            <h3 className="mb-4">{polyglot.t('GRADEBOOK_CATEGORIES_TITLE')}</h3>
            {editMode && (
              <>
                <span className="ms-2">(</span>
                <Button
                  color="link"
                  onClick={() => setEdit(!edit)}
                  className="px-0 categories-edit-btn"
                >
                  {polyglot.t(edit ? 'DONE' : 'EDIT')}
                </Button>
                <span>)</span>
              </>
            )}
          </div>
        )}
        {edit ? <CategoriesEditor /> : categories.length ? <CategoriesSection /> : null}
        <AssignmentsSection userCanEdit={editMode} />
        {!!categories.length && <StructureSection />}
      </div>
    </div>
  );
};
export default GradebookPage;
