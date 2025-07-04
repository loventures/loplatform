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

import { UserInfo } from '../../../../loPlatform';
import { Content } from '../../../api/contentsApi';
import Course from '../../../bootstrap/course';
import ERActivity from '../../../commonPages/contentPlayer/views/ERActivity';
import { ERLandmark } from '../../../landmarks/ERLandmarkProvider';
import { useContentGatingInfoResource } from '../../../resources/GatingInformationResource';
import { useLearningPathResource } from '../../../resources/LearningPathResource';
import { useCourseSelector } from '../../../loRedux';
import {
  ContentWithRelationships,
  assembleActivityView,
} from '../../../courseContentModule/selectors/assembleContentView';
import { ViewingAs } from '../../../courseContentModule/selectors/contentEntry';
import { selectContentItemRelations } from '../../../courseContentModule/selectors/contentEntrySelectors';
import { useTranslation } from '../../../i18n/translationContext';
import { selectContentItemsWithNebulousDetails } from '../../../selectors/selectContentItemsWithNebulousDetails';
import { CONTENT_TYPE_LESSON, CONTENT_TYPE_MODULE } from '../../../utilities/contentTypes';
import { timeoutEffect } from '../../../utilities/effectUtils';
import { PrintService } from '../../../utilities/print';
import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { FiPrinter, FiX } from 'react-icons/fi';
import { useHistory } from 'react-router';
import { Button, Spinner } from 'reactstrap';
import { lojector } from '../../../loject';

const ERPrintOverlay: React.FC<{ loaded: boolean; printable: boolean; onClose: () => void }> = ({
  loaded,
  printable,
  onClose,
}) => {
  const translate = useTranslation();

  return (
    <div
      id="print-overlay"
      className="position-fixed vw-100 vh-100 flex-column-center justify-content-center d-print-none-view"
      style={{
        zIndex: 9999,
        left: 0,
        top: 0,
        background: 'rgba(0,0,0,.25)',
      }}
    >
      <div
        className="flex-column-center"
        style={{
          backgroundColor: 'rgba(255,255,255,.85)',
          borderRadius: '1rem',
          padding: '2rem 2rem 1rem',
        }}
      >
        <FiPrinter
          size="16rem"
          strokeWidth={1}
          style={{
            opacity: printable ? 0.85 : 0.5,
            transition: 'opacity ease-in .3s',
          }}
        />
        <div
          id="preparing-print-view"
          style={{ visibility: printable ? 'hidden' : 'visible' }}
        >
          {loaded ? 'Rendering print view...' : 'Preparing print view...'}
          <Spinner
            className="ms-2"
            bsSize="sm"
          />
        </div>
      </div>
      <Button
        id="print-close"
        color="light"
        style={{ position: 'absolute', top: '1rem', right: '1rem' }}
        className="px-2"
        onClick={onClose}
        aria-label={translate('CLOSE_PRINT_VIEW')}
        title={translate('CLOSE_PRINT_VIEW')}
      >
        <FiX
          size="2rem"
          strokeWidth={1.25}
        />
      </Button>
    </div>
  );
};

const ERContentPrint: React.FC<{
  content: ContentWithRelationships;
  actualUser: UserInfo;
  viewingAs: ViewingAs;
}> = ({ content, viewingAs, actualUser }) => {
  const [loaded, setLoaded] = useState(0); // how many content items have loaded
  const [bodyHeight, setBodyHeight] = useState(0); // has the body height stabilised
  const [printable, setPrintable] = useState(false); // are we ready to print
  const history = useHistory();
  const nebulousContents = useCourseSelector(selectContentItemsWithNebulousDetails);
  const contentRelations = useCourseSelector(selectContentItemRelations);
  const learningPath = useLearningPathResource(viewingAs.id);
  const gatingInfo = useContentGatingInfoResource(content.id, viewingAs.id);

  const assembleContentItem = useCallback(
    ({ id, lesson }: { id: string; lesson?: Content }) => ({
      ...assembleActivityView(nebulousContents[id], contentRelations, nebulousContents),
      lesson,
    }),
    [nebulousContents, contentRelations]
  );

  const module = useMemo(
    () =>
      content.typeId === CONTENT_TYPE_MODULE
        ? content
        : content.ancestors.find(c => c.typeId === CONTENT_TYPE_MODULE),
    [content]
  );

  // this is only defined if you are printing a single piece of content.
  const lesson = useMemo(
    () =>
      content.typeId === CONTENT_TYPE_LESSON
        ? content
        : content.ancestors.find(c => c.typeId === CONTENT_TYPE_LESSON),
    [content]
  );

  const elements = useMemo(
    () =>
      content.typeId === CONTENT_TYPE_MODULE && !gatingInfo.isLocked
        ? learningPath.modules
            .find(m => m.content.id === content.id)!
            .elements.map(assembleContentItem)
        : [assembleContentItem(content)],
    [content, learningPath, assembleContentItem, gatingInfo]
  );

  const onLoaded = useCallback(
    timeoutEffect(() => setLoaded(l => 1 + l), 100),
    [setLoaded]
  );

  /* This adds the elements to the DOM one at a time, waiting for each to call back that it
   * is loaded before waiting 100ms and adding the next. Once the last has loaded then we
   * loop every 500ms waiting for the document height to settle. Only then do we open print.
   * It's not clear that all this is necessary, but the delays reduce the burden on both the
   * browser and the server, and hopefully gives any content scripts plenty of time to run.
   */

  const fullyLoaded = !!loaded && loaded >= elements.length;
  useEffect(() => {
    if (fullyLoaded) {
      const documentHeight = document.body.offsetHeight;
      if (bodyHeight === documentHeight) {
        setPrintable(true);
      } else {
        return timeoutEffect(() => setBodyHeight(documentHeight), 500)();
      }
    }
  }, [fullyLoaded, bodyHeight]);

  const closePrintView = () => history.goBack();

  useEffect(() => {
    if (printable) {
      const printService: PrintService = lojector.get('Print');
      return printService.printViewPrint(undefined, content, closePrintView);
    }
  }, [printable]);

  return (
    <ERLandmark
      landmark="content"
      className="container p-0"
    >
      <ERPrintOverlay
        loaded={fullyLoaded}
        printable={printable}
        onClose={closePrintView}
      />
      <div className="content-print-index m-0 m-md-3 m-lg-4">
        <div className="text-muted section-id">Section: {Course.groupId}</div>
        <h1 className="course-name">{Course.name}</h1>
        {module && <h2 className="module-title mb-4">{module.name}</h2>}
        {lesson && <h3 className="lesson-title mb-4">{lesson.name}</h3>}
        {elements.slice(0, 1 + loaded).map((element, index) => {
          const thisLesson = element.lesson;
          const priorLesson = elements[index - 1]?.lesson;
          const showLesson =
            thisLesson &&
            thisLesson.id !== priorLesson?.id &&
            (index > 0 || thisLesson.name !== module?.name);

          return (
            <div
              className="mt-4 print-activity"
              key={element.id}
            >
              {showLesson && <h3 className="lesson-title mb-4">{thisLesson?.name}</h3>}
              <ERActivity
                content={element}
                viewingAs={viewingAs}
                actualUser={actualUser}
                onLoaded={onLoaded}
                printView
              />
              <hr />
            </div>
          );
        })}
      </div>
    </ERLandmark>
  );
};

export default ERContentPrint;
