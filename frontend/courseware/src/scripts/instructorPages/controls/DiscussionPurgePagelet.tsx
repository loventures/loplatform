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

import axios from 'axios';
import DateEditor from '../../components/dateTime/DateEditor';
import { useCourseSelector } from '../../loRedux';
import dayjs from 'dayjs';
import advanced from 'dayjs/plugin/advancedFormat';
import timezone from 'dayjs/plugin/timezone';
import LoadingSpinner from '../../directives/loadingSpinner';
import React, { useState } from 'react';
import { Button } from 'reactstrap';

dayjs.extend(advanced);
dayjs.extend(timezone);

type Summary = {
  [key: string]: {
    title: string;
    numberOfPosts: number;
    delGuid?: string;
  };
};

// We must send the user's timezone so that "today" is correct from the user's perspective.
const timezoneOffset = dayjs().format('Z');

const DiscussionPurgePagelet: React.FC = () => {
  const section = useCourseSelector(s => s.course.id);

  const [working, setWorking] = useState<boolean>(false);
  const [success, setSuccess] = useState<string | undefined>(undefined);
  const [error, setError] = useState<any | undefined>(undefined);
  const [warning, setWarning] = useState<Summary | undefined>(undefined);

  const sixMonthsAgo = dayjs().subtract(6, 'months').format('YYYY-MM-DD');
  const [purgeDate, setPurgeDate] = useState<string>(sixMonthsAgo);

  const openWarning = () => {
    setError(undefined);
    axios
      .post<Summary>(`/api/v2/discussion/purge/${section}/dry`, { date: purgeDate, timezoneOffset })
      .then(resp => {
        if (resp.status === 200) {
          const summ = resp.data;
          setWarning(summ);
        } else {
          setError(resp.data);
        }
      })
      .catch(e => {
        setError(e);
        console.error(e);
      });
  };

  const doDelete = () => {
    setWorking(true);
    axios
      .post<Summary>(`/api/v2/discussion/purge/${section}`, { date: purgeDate, timezoneOffset })
      .then(resp => {
        if (resp.status === 200) {
          const dels = Object.values(resp.data).map(r => r.delGuid);
          setSuccess(dels[0]);
          setWorking(false);
        } else {
          setError(resp.data);
        }
      })
      .catch(e => {
        setError(e);
        console.error(e);
      })
      .finally(() => setWorking(false));
  };

  const formattedDate = dayjs(purgeDate || sixMonthsAgo).format('MMMM Do YYYY');
  const totalPurge = warning
    ? Object.values(warning).reduce((sum, c) => sum + c.numberOfPosts, 0)
    : 0;

  return (
    <div className="purge-pagelet d-flex flex-column align-items-center justify-content-center">
      <p className="my-3 w-100">
        For long running courses, it may be desirable to purge all discussion boards of old posts to
        maintain active course engagement. Discussion Purge allows you to remove all un-pinned
        discussion posts on or before a specified date. Purged discussion posts can be recovered but
        will require a ticket to platform support so use discretion before applying this tool.
      </p>
      <div className="d-flex flex-column justify-content-center align-items-center">
        <DateEditor
          onChange={e => {
            setError(undefined);
            setSuccess(undefined);
            setWarning(undefined);
            setWorking(false);
            setPurgeDate(e.target.value);
          }}
          onBlur={e => setPurgeDate(e.target.value)}
          value={purgeDate || sixMonthsAgo}
          name="purgeDate"
        />
        <Button
          color="primary"
          className="mt-3"
          disabled={Boolean(warning)}
          onClick={() => openWarning()}
        >
          Confirm Purge of all posts on or before {formattedDate}
        </Button>
        {warning ? (
          <div className="mt-3">
            {totalPurge > 0 ? (
              <>
                <p>{totalPurge} discussion posts will be deleted. Are you sure?</p>
                <ul>
                  {Object.values(warning).map(c => (
                    <li key={c.title}>
                      <span className="text-truncate">{c.title}</span>
                      <span> ({c.numberOfPosts})</span>
                    </li>
                  ))}
                </ul>
                <Button
                  color="danger"
                  onClick={doDelete}
                >
                  Delete {totalPurge} posts occurring on or before {formattedDate}
                </Button>
              </>
            ) : (
              <p>No un-pinned discussion posts were found on or before {formattedDate}.</p>
            )}
          </div>
        ) : null}
        {working ? <LoadingSpinner /> : null}
        {success ? (
          <div className="alert alert-success m-4">
            <h5>Discussion Purge Complete</h5>
            <p>
              All posts on or before {formattedDate} have now been purged from the discussion
              boards. Please visit the discussion areas for each lesson to confirm that all
              appropriate posts have been purged.
            </p>
            <p>
              If you need posts restored, you can submit a support request. Please make sure your
              email includes course information, the date you performed the purge, and this unique
              deletion code: "{success ?? 'delGuid'}".
            </p>
          </div>
        ) : null}
        {error ? (
          <div className="alert alert-danger m-5">Error encountered: {JSON.stringify(error)} </div>
        ) : null}
      </div>
    </div>
  );
};

export default DiscussionPurgePagelet;
