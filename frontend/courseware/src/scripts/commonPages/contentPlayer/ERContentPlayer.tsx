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

import ERContentIndex from '../../commonPages/contentPlayer/views/ERContentIndex';
import RubricGrid from '../../components/rubric/RubricGrid';
import ERContentContainer from '../../landmarks/ERContentContainer';
import { useContentsResource } from '../../resources/ContentsResource';
import { useCourseSelector } from '../../loRedux';
import ContentInstructions from '../../contentPlayerComponents/parts/ContentInstructions';
import {
  isCourseWithRelationships,
  isLessonWithRelationships,
  isModuleWithRelationships,
  isUnitWithRelationships,
  selectOnContentPlayerPath,
  selectPageContent,
} from '../../courseContentModule/selectors/contentEntrySelectors';
import { selectActualUser, selectCurrentUser } from '../../utilities/rootSelectors';
import { getRoleSegment } from '../../utils/linkUtils';
import React from 'react';
import { useCollapse } from 'react-collapsed';
import { Navigate } from 'react-router-dom';
import { Alert } from 'reactstrap';
import { ErrorBoundary } from 'react-error-boundary';

const ERContentPlayer: React.FC<{ search: string; state: any }> = ({ search, state }) => {
  const viewingAs = useCourseSelector(selectCurrentUser);
  // use Content Resource purely for resource loading into redux purposes
  useContentsResource(viewingAs.id);

  const content = useCourseSelector(selectPageContent);
  const actualUser = useCourseSelector(selectActualUser);
  const onContentPath = useCourseSelector(selectOnContentPlayerPath);

  // Bail out while a route change away from content is mid-flight under v7's deferred transition, so
  // the still-mounted player can't fire its module→first-child redirect and bounce that navigation
  // (e.g. print). See selectOnContentPlayerPath.
  if (!onContentPath) {
    return null;
  }

  if (isCourseWithRelationships(content) || !content.typeId) {
    /**
     * NOTE: we should never get here. The redirect here caused navigation issues.
     * TODO: push any potential redirects further up the hierarchy.
     * */
    return null;
  } else if (
    isUnitWithRelationships(content) ||
    isModuleWithRelationships(content) ||
    isLessonWithRelationships(content)
  ) {
    const child = content.elements?.[0];
    if (!child) return null;
    // v6: navigation state is a separate prop, and a relative pathname would resolve differently
    // than v5's URL-relative behavior, so build the sibling content path absolutely.
    return (
      <Navigate
        replace
        to={{ pathname: `${getRoleSegment()}/content/${child.id}`, search }}
        state={state}
      />
    );
  } else
    return (
      <ERContentContainer title={content.name}>
        <ErrorBoundary
          fallback={
            <div className="p-4">
              <Alert color="danger">Something went wrong.</Alert>
            </div>
          }
        >
          <FloatingInstructions />
          <ERContentIndex
            content={content}
            actualUser={actualUser}
            viewingAs={viewingAs}
          />
        </ErrorBoundary>
      </ERContentContainer>
    );
};

const FloatingInstructions: React.FC = () => {
  const instructing = useCourseSelector(state => state.ui.instructingState.status);
  const instructions = useCourseSelector(state => state.ui.instructionsState.value);

  const { getCollapseProps } = useCollapse({
    defaultExpanded: false,
    id: 'instructions',
    isExpanded: !!instructing,
  });

  return (
    <div
      className="floating-instructions content-width"
      {...getCollapseProps()}
    >
      <div className="scroller">
        <div className="container">
          {instructions?.instructions && (
            <ContentInstructions instructions={instructions.instructions} />
          )}
          {instructions?.rubric && (
            <div className="mx-3 pb-4">
              <RubricGrid rubric={instructions.rubric} />
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default ERContentPlayer;
