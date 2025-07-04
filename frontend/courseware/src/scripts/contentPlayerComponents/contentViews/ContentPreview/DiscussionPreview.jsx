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
import dayjs from 'dayjs';
import localized from 'dayjs/plugin/localizedFormat';
import { withTranslation } from '../../../i18n/translationContext.js';

import ContentIcon from '../../parts/ContentIcon.jsx';
import ContentLink from '../../parts/ContentLink.js';
import ContentLockableContainer from '../../parts/ContentLockableContainer.jsx';
import { TranslatedContentLeafType } from '../propTypes.js';

dayjs.extend(localized);

const SummaryCount = (label, count, classNames) => (
  <div className={classNames}>
    {count} {label}
  </div>
);

const SpecificSummaryRow = ({ summary = {}, viewingAs, translate }) => (
  <div className="discussion-preview-special mt-0 flex-row-content flex-wrap align-self-start">
    {summary.newPostCount > 0 &&
      viewingAs.isStudent &&
      SummaryCount(
        translate('DISCUSSION_PREVIEW_POST_COUNT_NEW'),
        summary.newPostCount,
        'block-badge badge-danger'
      )}

    {summary.unreadPostCount > 0 &&
      viewingAs.isInstructor &&
      SummaryCount(
        translate('DISCUSSION_PREVIEW_POST_COUNT_UNREAD'),
        summary.unreadPostCount,
        'block-badge badge-danger'
      )}

    {summary.unrespondedThreadCount > 0 &&
      viewingAs.isInstructor &&
      SummaryCount(
        translate('DISCUSSION_PREVIEW_POST_COUNT_UNRESPONDED'),
        summary.unrespondedThreadCount,
        'block-badge badge-warning'
      )}
  </div>
);

const OverallSummaryRow = ({ summary = {}, translate }) => (
  <div className="discussion-preview-overall mt-0 flex-row-content flex-wrap align-self-start">
    {SummaryCount(translate('DISCUSSION_PREVIEW_POST_COUNT'), summary.postCount)}

    {SummaryCount(translate('DISCUSSION_PREVIEW_PARTICIPANT_COUNT'), summary.participantCount)}
  </div>
);

const StatsBox = ({ summary = {}, translate }) => (
  <div className="discussion-stats-box">
    <span className="stats-box-label">{translate('DISCUSSION_PREVIEW_LAST_UPDATED')}</span>
    <div className="stats-box-stats">
      <span>
        {summary.postCount > 0
          ? dayjs(summary.lastPostCreationDate).format('l')
          : translate('DISCUSSION_PREVIEW_LAST_UPDATED_NEVER')}
      </span>
    </div>
  </div>
);

const DiscussionPreview = ({
  translate,
  content,
  typeId = 'type-' + content.typeId.replace(/\..*/, ''), //ex. 'diagnostic.1' -> 'type-diagnostic'
  viewingAs,
  showSummary = true,
}) => (
  <ContentLockableContainer
    content={content}
    viewingAs={viewingAs}
  >
    <div
      className={classnames([
        'card discussion-preview',
        typeId,
        content.availability.isLocked && 'locked',
      ])}
    >
      <div className="card-body">
        <div className="flex-row-content flex-wrap align-items-start justify-content-around content-view-top-row">
          <ContentIcon
            className="h3"
            content={content}
          />
          <div className="d-flex flex-column flex-col-fluid align-self-stretch">
            <div className="discussion-preview-title-row flex-col-fluid align-self-stretch">
              <ContentLink
                content={content}
                viewingAs={viewingAs}
                disabled={content.availability.isLocked}
                nav={viewingAs.isStudent ? undefined : 'none'}
              >
                <h2 className="h3">{content.name}</h2>
              </ContentLink>
            </div>
            {!content.availability.isLocked &&
              showSummary &&
              SpecificSummaryRow({
                translate,
                viewingAs,
                summary: content.activity.discussionSummary,
              })}
            {!content.availability.isLocked &&
              showSummary &&
              OverallSummaryRow({
                translate,
                summary: content.activity.discussionSummary,
              })}
          </div>
          {!content.availability.isLocked &&
            showSummary &&
            StatsBox({
              translate,
              summary: content.activity.discussionSummary,
            })}
        </div>
      </div>
    </div>
  </ContentLockableContainer>
);

DiscussionPreview.propTypes = TranslatedContentLeafType;

export default withTranslation(DiscussionPreview);
