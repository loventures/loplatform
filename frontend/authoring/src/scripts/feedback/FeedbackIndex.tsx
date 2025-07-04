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

import React, { useEffect, useMemo, useState } from 'react';
import { BsChatLeftText } from 'react-icons/bs';
import { useDispatch } from 'react-redux';
import { useHistory } from 'react-router';
import { Link } from 'react-router-dom';
import { Button, Table } from 'reactstrap';

import {
  trackFeedbackIndexDetails,
  trackFeedbackIndexDownload,
  trackFeedbackIndexPage,
  trackFeedbackIndexRefresh,
} from '../analytics/AnalyticsEvents';
import { Loadable } from '../authoringUi';
import { formatFullDate, formatMD } from '../dateUtil';
import { useBranchId, useDcmSelector, usePolyglot } from '../hooks';
import { ConfirmationTypes } from '../modals/ConfirmModal';
import { openModal } from '../modals/modalActions';
import { ModalIds } from '../modals/modalIds';
import { ApiQueryResults } from '../srs/apiQuery';
import { useProjectGraph } from '../structurePanel/projectGraphActions';
import { openToast } from '../toast/actions';
import { feedbackLoaded, refreshFeedback, setFeedbackOffset } from './feedbackActions';
import { FeedbackDto, archiveFeedback, downloadFeedbackUrl, loadFeedbacks } from './FeedbackApi';
import FeedbackAssigneeDropdown from './FeedbackAssigneeDropdown';
import { useFeedbackFilters, useFeedbackOffset } from './feedbackHooks';
import FeedbackStatusDropdown from './FeedbackStatusDropdown';

const FeedbackTable: React.FC<ApiQueryResults<FeedbackDto>> = ({
  objects,
  count,
  offset,
  limit,
  filterCount,
  totalCount,
}) => {
  const dispatch = useDispatch();
  const polyglot = usePolyglot();
  const branchId = useBranchId();
  const { branch, status, assignee } = useFeedbackFilters();
  const [rotate, setRotate] = useState(0);
  const feedbackBranch = branch ?? branchId;

  const { userCanEditSettings } = useDcmSelector(s => s.layout);

  const onArchiveAll = () => {
    dispatch(
      openModal(ModalIds.Confirm, {
        confirmationType: ConfirmationTypes.ArchiveFeedback,
        color: 'danger',
        words: {
          header: polyglot.t('CONFIRM_ARCHIVE_FEEDBACK.HEADER'),
          body: polyglot.t('CONFIRM_ARCHIVE_FEEDBACK.BODY'),
          confirm: polyglot.t('ARCHIVE'),
        },
        confirmCallback: () => {
          archiveFeedback(feedbackBranch).then(() => {
            dispatch(openToast(polyglot.t('FEEDBACK_ARCHIVED'), 'success'));
            dispatch(refreshFeedback());
          });
        },
      })
    );
  };
  return (
    <div>
      <div className="d-flex flex-row align-items-center p-2 feedback-controls">
        <Button
          size="sm"
          color="primary"
          outline
          className="material-icons md-18 p-1 mini-button"
          title={polyglot.t('FEEDBACK_DOWNLOAD')}
          tag="a"
          href={downloadFeedbackUrl(feedbackBranch, { status, assignee })}
          onClick={trackFeedbackIndexDownload}
          target="_blank"
          rel="noreferrer noopener"
        >
          <span className="material-icons md-18">download</span>
        </Button>
        {userCanEditSettings && (
          <Button
            size="sm"
            color="danger"
            outline
            className="material-icons md-18 p-1 mini-button ms-1"
            title={polyglot.t('FEEDBACK_ARCHIVE_ALL')}
            onClick={onArchiveAll}
          >
            clear_all
          </Button>
        )}

        <div className="flex-grow-1"></div>
        <Button
          size="sm"
          color="transparent"
          className="material-icons md-18 p-1 me-3"
          title="Refresh"
          onClick={() => {
            setRotate(r => 360 + r);
            dispatch(refreshFeedback());
            trackFeedbackIndexRefresh();
          }}
        >
          <span
            className="material-icons md-18"
            style={{ transition: 'transform linear .5s', transform: `rotate(${rotate}deg)` }}
          >
            refresh
          </span>
        </Button>
        <div>
          {count ? `${offset + 1} – ${offset + count} of ${filterCount}` : `0 of ${filterCount}`}
          {filterCount < totalCount ? ` (out of ${totalCount})` : ''}
        </div>
        <Button
          disabled={offset === 0}
          size="sm"
          color="transparent"
          className="material-icons md-18 p-1 ms-3 border-0"
          title="Previous page"
          onClick={() => {
            dispatch(setFeedbackOffset(offset - limit));
            trackFeedbackIndexPage('Previous');
          }}
        >
          chevron_left
        </Button>
        <Button
          disabled={offset + count >= filterCount}
          size="sm"
          color="transparent"
          className="material-icons md-18 p-1 ms-2 border-0"
          title="Next page"
          onClick={() => {
            dispatch(setFeedbackOffset(offset + limit));
            trackFeedbackIndexPage('Next');
          }}
        >
          chevron_right
        </Button>
      </div>
      <div style={{ marginLeft: '-1rem', marginRight: '-1rem' }}>
        <Table className="feedback-table">
          <tbody>
            {!objects.length ? (
              <tr className="inactive">
                <td>{totalCount ? 'No results match your filter.' : 'No results.'}</td>
              </tr>
            ) : (
              objects.map(feedback => (
                <FeedbackRow
                  key={feedback.id}
                  feedback={feedback}
                />
              ))
            )}
          </tbody>
        </Table>
      </div>
    </div>
  );
};

const FeedbackRow: React.FC<{ feedback: FeedbackDto }> = ({ feedback }) => {
  const branchId = useBranchId();
  const history = useHistory();
  const text = useMemo(() => {
    const temp = document.createElement('span');
    temp.innerHTML = feedback.feedback;
    return temp.textContent;
  }, [feedback.feedback]);
  const { nodes } = useProjectGraph();
  const disabled = !nodes[feedback.assetName];

  return (
    <tr
      onClick={
        disabled
          ? undefined
          : () => {
              history.push(`/branch/${branchId}/feedback/${feedback.id}`);
              trackFeedbackIndexDetails();
            }
      }
      className={disabled ? 'inactive disabled' : undefined}
    >
      <td
        className="title-block"
        style={{ width: '15rem', paddingLeft: '1.75rem' }}
      >
        <div className="text-truncate fw-bold feedback-sender">{feedback.creator.fullName}</div>
        <div className="text-truncate feedback-time">{formatFullDate(feedback.created)}</div>
      </td>
      <td className="feedback-content-block">
        <div className="d-flex align-items-start">
          <Link
            to={`/branch/${branchId}/feedback/${feedback.id}`}
            className="flex-grow-1 text-dark feedback-text"
            style={{
              overflow: 'hidden',
              textOverflow: 'ellipsis',
              display: '-webkit-box',
              WebkitLineClamp: 2,
              WebkitBoxOrient: 'vertical',
            }}
            onClick={e => {
              e.stopPropagation();
              trackFeedbackIndexDetails();
            }}
          >
            {text}
          </Link>
          <div
            className="d-flex flex-column align-items-end ms-1 feedback-dropdowns"
            onClick={e => e.stopPropagation()}
          >
            <FeedbackStatusDropdown
              feedback={feedback}
              disabled={disabled}
            />
            <FeedbackAssigneeDropdown
              feedback={feedback}
              disabled={disabled}
            />
          </div>
        </div>
      </td>
      <td
        style={{ width: '6rem', paddingRight: '1.75rem' }}
        className="text-right text-nowrap date-block"
      >
        <div style={{ paddingTop: '1px' }}>{formatMD(feedback.modified)}</div>
        {feedback.replies ? (
          <div>
            {feedback.replies}
            <BsChatLeftText
              className="ms-1"
              size=".85rem"
              style={{ verticalAlign: '-2px' }}
            />
          </div>
        ) : null}
      </td>
    </tr>
  );
};

const FeedbackPage: React.FC = () => {
  const branchId = useBranchId();
  const [fetching, setFetching] = useState(true);
  const [feedbacks, setFeedbacks] = useState<ApiQueryResults<FeedbackDto>>();
  const { branch, status, assignee, unit, module, refresh } = useFeedbackFilters();
  const offset = useFeedbackOffset();
  const dispatch = useDispatch();
  const feedbackBranch = branch ?? branchId;

  useEffect(() => {
    loadFeedbacks(feedbackBranch, {
      status,
      assignee,
      offset,
      unit,
      module,
      remotes: branch ? branchId : undefined,
      limit: 10,
    }).then(feedbacks => {
      if (offset && !feedbacks.count) {
        dispatch(setFeedbackOffset(0));
      } else {
        dispatch(feedbackLoaded(feedbacks.objects));
        setFeedbacks(feedbacks);
        setFetching(false);
      }
    });
    return undefined;
  }, [feedbackBranch, offset, status, assignee, unit, module, refresh]);

  return (
    <div className="px-3 pb-5 mx-auto feedback-app">
      <Loadable loading={fetching}>{() => <FeedbackTable {...feedbacks} />}</Loadable>
    </div>
  );
};

export default FeedbackPage;
