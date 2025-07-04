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
import useGatingTooltip from '../../commonPages/sideNav/useGatingTooltip';
import LoLink from '../../components/links/LoLink';
import {
  ContentWithAncestors,
  useFlatLearningPathResource,
} from '../../resources/LearningPathResource';
import { ContentPlayerPageLink } from '../../utils/pageLinks';
import { useTranslation } from '../../i18n/translationContext';
import { getContentDisplayInfo } from '../../utilities/contentDisplayInfo';
import React from 'react';
import { FiLock } from 'react-icons/fi';

type ERNextUpProps = {
  content: ContentLite;
  nextUp: ContentWithAncestors;
};

const ERNextUp: React.FC<ERNextUpProps> = ({ content, nextUp }) => {
  const translate = useTranslation();
  const { linkProps, locked, GatingTooltip, showLockIcon } = useGatingTooltip(
    nextUp.id,
    'next-up-button'
  );
  // horrid
  const current = useFlatLearningPathResource().find(c => c.id === content.id);
  // if the lesson has the same name as the module then it's the funny old
  // whole-module lesson (excluding the discussion board) so pretend it doesn't end
  const finishedLesson =
    nextUp.lesson?.id !== current?.lesson?.id &&
    (nextUp.lesson != null || current?.lesson?.name !== current?.module?.name)
      ? current?.lesson
      : null;
  const finishedUnit = nextUp.unit?.id !== current?.unit?.id ? current?.unit : null;
  const finishedModule = nextUp.module?.id !== current?.module?.id ? current?.module : null;
  const finished = finishedUnit ?? finishedModule ?? finishedLesson;

  const nextContext =
    (nextUp.unit?.id !== current?.unit?.id ? nextUp.unit : null) ??
    (nextUp.module?.id !== current?.module?.id ? nextUp.module : null) ??
    (finishedLesson ? nextUp.lesson : null);

  const displayKey = getContentDisplayInfo(nextUp).displayKey;
  const contentType = translate(`CONTENT_NAME_${displayKey}`);
  const subtext = nextUp.duration
    ? translate('ASSET_TIME_MINUTE', {
        durationMinutes: nextUp.duration,
        contentType,
      })
    : contentType;
  // the key below is to avoid the link remaining focused after navigation
  return (
    <>
      {finished && (
        <div className="finished-up d-flex align-items-center justify-content-center mb-4 mb-lg-5">
          <div className="dash flex-grow-1" />
          <div className="finished flex-grow-0 mx-2">
            <span>End of </span>
            {finished.name}
          </div>
          <div className="dash flex-grow-1" />
        </div>
      )}
      <div className="next-up px-3 p-md-0 flex-column-center d-print-none">
        <LoLink
          key={nextUp.id}
          to={ContentPlayerPageLink.toLink({ content: nextUp })}
          aria-label={`${translate('FOOTER_NEXT')}: ${nextUp.name}, ${subtext}`}
          className={classnames('btn-group d-flex flex-row btn-parent', { locked })}
          style={{ textDecoration: 'none' }}
          {...linkProps}
        >
          <div
            className={classnames(
              'btn btn-primary lighten-when-dark d-flex text-nowrap align-items-center',
              {
                disabled: locked,
              }
            )}
            style={{ flexGrow: 0 }}
          >
            {translate('FOOTER_NEXT')}
          </div>
          <div
            className={classnames(
              'btn btn-link btn-light px-3 d-flex flex-row align-items-center justify-content-between',
              {
                disabled: locked,
              }
            )}
            style={{ textDecoration: 'none', textAlign: 'left', minWidth: '10rem' }}
          >
            <div
              className="d-flex flex-column"
              style={{ minWidth: 0 }}
            >
              {nextContext && <div className="small text-muted">{nextContext.name}</div>}
              <div
                className="next-up-activity-name"
                style={{ textDecoration: 'underline' }}
                title={nextUp.name}
              >
                {nextUp.name}
              </div>
              <div className="next-up-activity-type small text-color">{subtext}</div>
            </div>
            {showLockIcon ? (
              <FiLock
                className="ms-2"
                style={{ flexShrink: 0, marginRight: '-.25rem' }}
                size="1rem"
                strokeWidth={2}
                aria-hidden={true}
                title={translate('ER_ACTIVITY_LOCKED')}
              />
            ) : null}
            {showLockIcon ? <GatingTooltip /> : null}
          </div>
        </LoLink>
      </div>
    </>
  );
};

export default ERNextUp;
