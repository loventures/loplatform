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

import Course from '../../bootstrap/course';
import ERContentPrint from '../../commonPages/contentPlayer/views/ERContentPrint';
import ERContentContainer from '../../landmarks/ERContentContainer';
import { useContentsResource } from '../../resources/ContentsResource';
import { useCourseSelector } from '../../loRedux';
import { uniq } from 'lodash';
import {
  isCourseWithRelationships,
  selectPageContent,
} from '../../courseContentModule/selectors/contentEntrySelectors';
import { allowPrintingEntireLesson } from '../../utilities/preferences';
import { selectActualUser, selectCurrentUser } from '../../utilities/rootSelectors';
import React, { useEffect, useMemo } from 'react';

const ERContentPrinter: React.FC = () => {
  const viewingAs = useCourseSelector(selectCurrentUser);
  // use Content Resource purely for resource loading into redux purposes
  useContentsResource(viewingAs.id);

  // This dire horror forces angular assessments to render in print mod
  useEffect(() => {
    (window as any).inPrintMode = true;
    return () => {
      delete (window as any).inPrintMode;
    };
  }, []);

  const content = useCourseSelector(selectPageContent);
  const actualUser = useCourseSelector(selectActualUser);

  const title = useMemo(
    () =>
      isCourseWithRelationships(content)
        ? content.name
        : uniq([Course, ...content.ancestors, content].map(a => a.name)).join(' – '),
    [content, Course]
  );

  const printAllowed =
    !isCourseWithRelationships(content) && (content.ancestors.length || allowPrintingEntireLesson);

  return (
    <ERContentContainer
      title={title}
      className="print-view"
    >
      {printAllowed && (
        <ERContentPrint
          content={content}
          actualUser={actualUser}
          viewingAs={viewingAs}
        />
      )}
    </ERContentContainer>
  );
};

export default ERContentPrinter;
