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

import classnames from 'classnames';
import { ContentLite } from '../../api/contentsApi';
import Course from '../../bootstrap/course';
import ERBackButton from '../../commonPages/contentPlayer/ERBackButton';
import ERBookmarkButton from '../../commonPages/contentPlayer/ERBookmarkButton';
import ERFullScreenButton from '../../commonPages/contentPlayer/ERFullScreenButton';
import ERHomeButton from '../../commonPages/contentPlayer/ERHomeButton';
import ERInstructionsButton from '../../commonPages/contentPlayer/ERInstructionsButton';
import ERPrintButton from '../../commonPages/contentPlayer/ERPrintButton';
import ERFeedbackButton from '../../feedback/ERFeedbackButton';
import { useFeedbackEnabled } from '../../feedback/FeedbackStateService';
import { ERLandmark } from '../../landmarks/ERLandmarkProvider';
import { useCourseSelector } from '../../loRedux';
import QnaButton from '../../qna/QnaButton';
import { ContentWithRelationships } from '../../courseContentModule/selectors/assembleContentView';
import {
  selectContent,
  selectNavToPageContent,
} from '../../courseContentModule/selectors/contentEntrySelectors';
import { selectFullscreenState } from '../../courseContentModule/selectors/contentLandmarkSelectors';
import {
  CONTENT_TYPE_LESSON,
  CONTENT_TYPE_MODULE,
  CONTENT_TYPE_UNIT,
} from '../../utilities/contentTypes';
import { qnaEnabled, smeFeedbackEnabled } from '../../utilities/preferences';
import { selectCurrentUser } from '../../utilities/rootSelectors';
import React, { useEffect, useRef, useState } from 'react';
import { useParams } from 'react-router';
import { DropdownMenu, DropdownToggle, UncontrolledDropdown } from 'reactstrap';
import { IoEllipsisVerticalOutline } from 'react-icons/io5';
import { useTranslation } from '../../i18n/translationContext.tsx';
import { useMedia } from 'react-use';
import ERBookmarkModal from './ERBookmarkModal.tsx';
import ERSidebarButton from '../sideNav/ERSidebarButton.tsx';

// Heuristics so that we can responsively collapse breadcrumbs
// . Lesson 4: Set up your Life / Lesson 4 Introduction / Road Trip
//   -> Lesson 4 / Introduction / Road Trip
// . Lesson 4: Set up your Life / Chapter 1: Hitting the Road / Choose an Attorney
//   -> Lesson 4 / Chapter 1 / Choose an Attorney

// Splits segments into primary and ancillary components for responsive collapse.

export const ancillarise = (
  seg: string,
  prior?: string
): Array<{ label: string; className: string }> => {
  const match0 = seg.match(/^((?:Chapter|Module|Lesson|Unit)\s+\d+)(:.+)/i);
  if (match0) {
    // "Chapter 1: Foo Bar" -> primary: "Chapter 1", ancillary: ": Foo Bar"
    return [
      { className: 'primary', label: match0[1] },
      { className: 'ancillary', label: match0[2] },
    ];
  }
  const match1 = seg.match(/^((?:Chapter|Module|Lesson|Unit)\s+\d+\s+)(.+)/i);
  if (match1 && prior?.toLowerCase().startsWith(match1[1].trimEnd().toLowerCase())) {
    // "Lesson 1: X / Lesson 1 Introduction" -> ancillary: "Lesson 1 ", primary: "Introduction"
    return [
      { className: 'ancillary', label: match1[1] },
      { className: 'primary', label: match1[2] },
    ];
  }

  return [{ className: 'primary', label: seg }];
};

// shrinks a module and lesson name to a more compact form like the content header
export const minimalise = (module?: string, lesson?: string): string[] => {
  if (module == null) {
    return [];
  } else if (lesson == null || lesson.toLowerCase() === module.toLowerCase()) {
    return [module];
  }
  const match0 = module.match(/^((?:Chapter|Module|Lesson|Unit)\s+\d+)(:.+)/i);
  const shortModule = match0 ? match0[1] : module;
  const match1 = lesson.match(/^((?:Chapter|Module|Lesson|Unit)\s+\d+:?\s+)(.+)/i);
  const shortLesson =
    match1 && module?.toLowerCase().startsWith(match1[1].trimEnd().toLowerCase())
      ? match1[2]
      : lesson;
  return [shortModule, shortLesson];
};

// This is ridiculous but we can't rely on the passed-in content being a
// ContentWithRelationships because sometimes after the back button it is
// not that anymore. So instead we just rely on [useCourseSelector] which
// does a magical lookup for a [content.id] property on the component it
// is running in. Somehow.
// Update: we can't rely on useCourseSelector because that selects from the url
// which may not have the content id in it (Qna). Instead we check the
// incoming content for ancestors and use those if we have them.
type ERContentTitleProps = {
  content: ContentLite;
  printView?: boolean;
};

export const useMagicalStuckness = (ref: React.MutableRefObject<HTMLDivElement>) => {
  const [stuck, setStuck] = useState(false);
  const [minHeight, setMinHeight] = useState<number | undefined>(undefined);
  const mediumScreen = useMedia('(min-width: 48em)');

  // Switch to stuck mode when the remaining visible title area is the same size as
  // the stuck header so the transition is smooth.
  // There's no way to reliably use an intersection observer to achieve this, and
  // there's no way to use position sticky without having certain scroll positions
  // where the browser just oscillates between stuck and unstuck.
  useEffect(() => {
    const el = ref.current;
    if (el) {
      let offscreen = false;
      const listener = () => {
        const rect = el.getBoundingClientRect();
        const height = mediumScreen ? 80 : 64; // 5rem desktop.. but 4rem mobile
        const nowOffscreen = rect.top + rect.height < height;
        if (nowOffscreen !== offscreen && rect.height > 0) {
          offscreen = nowOffscreen;
          setStuck(offscreen);
          // We need to preserve the height of the content title when we switch
          // to stuck mode so the content doesn't jank upwards.
          setMinHeight(mh => Math.max(rect.height, mh ?? 0));
          document.body.classList.toggle('header-stuck', offscreen);
        }
      };
      window.addEventListener('scroll', listener);
      const resizer = new ResizeObserver(listener);
      resizer.observe(el);
      listener();
      return () => {
        document.body.classList.remove('header-stuck');
        resizer.disconnect();
        window.removeEventListener('scroll', listener);
      };
    }
  }, [mediumScreen]);
  return { stuck, minHeight, mediumScreen };
};

const ERFancyContentTitle: React.FC<ERContentTitleProps> = ({ content: contentProp }) => {
  // these should all be memoized, but *everything* changes on every render
  const ref = useRef<HTMLDivElement>();
  const viewingAs = useCourseSelector(selectCurrentUser);
  const translate = useTranslation();
  const [addBookmark, setAddBookmark] = useState(false);

  // Big yikes
  let content = useCourseSelector(selectContent) as ContentWithRelationships;
  if (contentProp.hasOwnProperty('ancestors')) {
    content = contentProp as ContentWithRelationships;
  }
  const unit = content.ancestors?.find(c => c.typeId === CONTENT_TYPE_UNIT);
  const module = content.ancestors?.find(c => c.typeId === CONTENT_TYPE_MODULE);
  const lesson = content.ancestors?.find(c => c.typeId === CONTENT_TYPE_LESSON);
  const fullScreen = !!useCourseSelector(selectFullscreenState);
  const doNav = useCourseSelector(selectNavToPageContent);
  const [instructorFeedbackEnabled] = useFeedbackEnabled();
  const feedbackEnabled = smeFeedbackEnabled || instructorFeedbackEnabled;
  const { questionId: qnaRoute } = useParams<{ questionId: string }>(); // hack to check if we are using the qna Route

  const degenerate = !unit && module && !lesson && module.name === content.name;

  const rawCrumbSegments =
    (!unit && !module) || degenerate ? [] : [unit?.name, module?.name, lesson?.name];
  const crumbSegments = rawCrumbSegments.filter(
    (name, index): name is string =>
      name != null && name?.toLowerCase() !== rawCrumbSegments[1 + index]?.toLowerCase()
  );

  const { stuck, minHeight, mediumScreen } = useMagicalStuckness(ref);

  // This layout is a bit of a horror. In non-stuck mode we float the buttons right so the title
  // elements will flow around them; this allows the main title to take up the full width of the
  // screen, igoring buttons. For balance we have to add float left button spacers. But float
  // elements have to come first in the DOM. Then in stuck mode we have to switch to a flex
  // layout which means we need to shenanigan the left floater into the left. But then in small
  // devices in stuck mode we drop those left spacers. But those spacers are now the previous
  // button.

  const showQna = qnaEnabled && !qnaRoute && !viewingAs.isInstructor;
  return (
    <div
      className={classnames('er-content-title mb-4', stuck && !fullScreen && 'stuck')}
      style={minHeight ? { minHeight: `${minHeight}px` } : undefined}
      ref={ref}
    >
      <div className="content-title">
        {mediumScreen ? (
          <>
            <div className="d-flex d-print-none content-spacer flex-grow-0 me-2">
              {doNav ? <ERBackButton /> : <ERHomeButton />}
              <ERBookmarkButton
                content={content}
                setOpen={setAddBookmark}
              />
              <ERInstructionsButton stuck={stuck} />
            </div>
            <div className="d-flex d-print-none content-actions flex-grow-0 ms-2">
              {showQna ? <QnaButton /> : feedbackEnabled && !qnaRoute ? <ERFeedbackButton /> : null}
              <ERPrintButton
                viewingAs={viewingAs}
                content={content}
                module={module}
              />
              <ERFullScreenButton />
            </div>
          </>
        ) : (
          <>
            <div className="d-flex d-print-none content-spacer flex-grow-0 me-1">
              <ERSidebarButton header />
            </div>
            <div className="d-flex d-print-none content-actions flex-grow-0 ms-1">
              <UncontrolledDropdown>
                <DropdownToggle
                  id="kebab-button"
                  color="primary"
                  outline
                  className="border-white px-2"
                  aria-label={translate('PRINT_OPTIONS')}
                  title={translate('PRINT_OPTIONS')}
                >
                  <IoEllipsisVerticalOutline
                    size="1.75rem"
                    aria-hidden={true}
                    style={{ margin: '.125rem' }}
                  />
                </DropdownToggle>
                <DropdownMenu end>
                  {doNav ? <ERBackButton dropdown /> : <ERHomeButton dropdown />}
                  <ERBookmarkButton
                    content={content}
                    setOpen={setAddBookmark}
                    dropdown
                  />
                  <ERInstructionsButton
                    stuck={stuck}
                    dropdown
                  />
                  {showQna ? (
                    <QnaButton dropdown />
                  ) : feedbackEnabled && !qnaRoute ? (
                    <ERFeedbackButton dropdown />
                  ) : null}
                  {/* No mobile print or full screen for now */}
                </DropdownMenu>
              </UncontrolledDropdown>
            </div>
          </>
        )}

        <div className="title-bits">
          <div className="activity-context">
            <div
              className={classnames(
                'er-breadcrumb-segment text-truncate course-crumb',
                `nindex-${crumbSegments.length}`
              )}
            >
              <span>{Course.name}</span>
              <br style={{ display: 'none' }} />
            </div>
            {crumbSegments.map((seg, idx, segs) => (
              <div
                key={idx}
                className={classnames(
                  'er-breadcrumb-segment text-truncate',
                  `nindex-${segs.length - 1 - idx}`
                )}
              >
                {ancillarise(seg, segs[idx - 1]).map(({ className, label }, idx) => (
                  <span
                    key={idx}
                    className={className}
                  >
                    {label}
                  </span>
                ))}
              </div>
            ))}
          </div>
          <ERLandmark
            landmark="mainHeader"
            tag="h1"
            className="h3 activity-title pb-1 mb-0 feedback-context"
            tabIndex={-1}
            data-id="title"
          >
            {content.name}
          </ERLandmark>
        </div>
      </div>
      <ERBookmarkModal
        content={content}
        isOpen={addBookmark}
        setOpen={setAddBookmark}
      />
    </div>
  );
};

const ERPrintContentTitle: React.FC<ERContentTitleProps> = ({ content }) => (
  <h1 className="h3 print-content-title mb-3">{content.name}</h1>
);

const ERContentTitle: React.FC<ERContentTitleProps> = props =>
  props.printView ? <ERPrintContentTitle {...props} /> : <ERFancyContentTitle {...props} />;

export default ERContentTitle;
