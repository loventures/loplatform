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

import axios from 'axios';
import React, { useState } from 'react';
import { Alert, Button, Container, Input, Progress } from 'reactstrap';

import { asjax, ContentTypeMultipart } from '../services';
import { useTranslations } from '../redux/state';

const State_Init = 0;
const State_Counting = 1;
const State_Counted = 2;
const State_Purging = 3;
const State_Purged = 4;

interface UploadInfo {
  guid: string;
  fileName: string;
  size: number;
}

const Gdpr: React.FC = () => {
  const T = useTranslations();
  const [state, setState] = useState(State_Init);
  const [done, setDone] = useState(0);
  const [todo, setTodo] = useState(0);
  const [description, setDescription] = useState('');
  const [error, setError] = useState(false);
  const [file, setFile] = useState<UploadInfo | null>(null);
  const [uploading, setUploading] = useState(false);

  const suffix = () => (file ? `?minors=${file.guid}` : '');

  const countPurge = () => {
    setState(State_Counting);
    axios
      .get<number>(`/api/v2/gdpr/inactiveUserCount${suffix()}`)
      .then(res => {
        setState(State_Counted);
        setTodo(res.data);
      })
      .catch(() => setError(true));
  };

  const performPurge = () => {
    setState(State_Purging);
    asjax(
      `/api/v2/gdpr/purgeInactiveUsers${suffix()}`,
      {},
      (description: string, done: number, todo: number) => {
        setDescription(description);
        setDone(done);
        setTodo(todo);
      }
    )
      .then(() => setState(State_Purged))
      .catch(() => setError(true));
  };

  const onFile = (e: React.ChangeEvent<HTMLInputElement>) => {
    const upload = e.target.files?.[0];
    if (!upload) return;
    setUploading(true);
    const data = new FormData();
    data.append('upload', upload);
    axios
      .post<UploadInfo>('/api/v2/uploads', data, ContentTypeMultipart)
      .then(res => setFile(res.data))
      .catch(() => setError(true))
      .finally(() => setUploading(false));
  };

  return (
    <Container fluid>
      <h1 style={{ color: 'salmon' }}>{T.t('overlord.page.Gdpr.name')}</h1>
      <Alert
        color="warning"
        className="my-3"
      >
        <strong>Warning:</strong> This will permanently and irrevocably purge users who have been
        inactive for five years from the system. If you upload a spreadsheet of user identifiers
        for minors (under 18 years old), they will be purged if inactive just a year.
      </Alert>
      <div className="mb-3">
        {file === null ? (
          <Input
            id="minors"
            type="file"
            label="Minor User Identifiers CSV"
            className="w-auto"
            accept=".csv"
            onChange={onFile}
            disabled={state !== State_Init}
          />
        ) : (
          <Alert
            color="info"
            className="my-3 d-flex align-items-center"
          >
            <strong>Minor Users:&nbsp;</strong>
            <span className="flex-grow-1">
              {file.fileName} ({file.size} bytes)
            </span>
            <Button
              className="p-0"
              color="transparent"
              onClick={() => setFile(null)}
              disabled={state !== State_Init}
            >
              <span className="material-icons md-18">close</span>
            </Button>
          </Alert>
        )}
      </div>
      <Button
        disabled={state !== State_Init || uploading}
        onClick={countPurge}
        color="warning"
      >
        Count Users
      </Button>
      {state >= State_Counted && (
        <>
          <Alert
            color="danger"
            className="my-3"
          >
            <strong>Warning:</strong> This will permanently delete {todo} users!
          </Alert>
          <Button
            disabled={state !== State_Counted || !todo}
            onClick={performPurge}
            color="danger"
          >
            Purge Users
          </Button>
        </>
      )}
      {state >= State_Purging && (
        <Alert
          color="danger"
          className="my-3"
        >
          <Progress
            value={state === State_Purging ? done : todo}
            max={todo}
            animated={state === State_Purging}
            color="danger"
          >
            {state === State_Purging ? description : 'Complete'}
          </Progress>
        </Alert>
      )}
      {state >= State_Purged && (
        <Alert
          color="dark"
          className="my-3"
        >
          {todo} voices cried out in terror and were suddenly silenced. I fear something terrible
          has happened.
        </Alert>
      )}
      {error && <Alert color="danger my-3">An error occurred!</Alert>}
    </Container>
  );
};

export default Gdpr;
