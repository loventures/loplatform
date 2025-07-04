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

import React, { useMemo } from 'react';
import { useDispatch } from 'react-redux';
import { Link } from 'react-router-dom';
import { FormGroup, Input, Label } from 'reactstrap';

import {
  useDcmSelector,
  useDocumentTitle,
  useNumericRouterPathVariable,
  usePolyglot,
} from '../hooks';
import { QuillMenu } from '../story/NarrativeActionBar/QuillMenu';
import { setDropboxState } from './dropboxActions';

const DropboxActionBar: React.FC = () => {
  const polyglot = usePolyglot();
  const dispatch = useDispatch();
  const { filter, archived, directories } = useDcmSelector(state => state.dropbox);
  const folderId = useNumericRouterPathVariable('folder');
  const path = directories[folderId]?.path ?? [];

  const crumbs = useMemo(
    () => [polyglot.t('DROPBOX_LINK_TITLE'), ...path.map(dir => dir.fileName)],
    [path]
  );
  useDocumentTitle(crumbs);

  return (
    <div className="d-flex align-items-center h-100 px-3 narrative-action-bar">
      <h6 className="m-0 flex-grow-1 d-flex align-items-center minw-0">
        <QuillMenu />
        {!path.length ? (
          polyglot.t('DROPBOX_LINK_TITLE')
        ) : (
          <div className="text-truncate">
            <Link to="../dropbox">{polyglot.t('DROPBOX_LINK_TITLE')}</Link>
            {path.map((dir, index) => (
              <React.Fragment key={index}>
                <span className="text-muted">{' / '}</span>
                {dir.id === folderId ? (
                  <span className="text-muted">{dir.fileName}</span>
                ) : (
                  <Link to={`${dir.id}`}>{dir.fileName}</Link>
                )}
              </React.Fragment>
            ))}
          </div>
        )}
      </h6>
      <div className="d-flex align-items-center flex-grow-0 flex-shrink-0 form-inline">
        <FormGroup switch>
          <Input
            id="archive-switch"
            type="switch"
            checked={archived}
            onChange={e => dispatch(setDropboxState({ archived: e.target.checked }))}
          />
          <Label
            for="archive-switch"
            className="text-dark"
            check
          >
            Archived files
          </Label>
        </FormGroup>
        <Input
          type="search"
          value={filter}
          onChange={e => dispatch(setDropboxState({ filter: e.target.value }))}
          className="ms-3"
          bsSize="sm"
          style={{
            borderRadius: 'calc(0.75em + 0.375rem + 1px)',
            paddingLeft: '0.75rem',
            width: '12rem',
          }}
          placeholder={polyglot.t('DROPBOX_FILE_NAME_CONTAINS')}
        />
      </div>
    </div>
  );
};

export default DropboxActionBar;
