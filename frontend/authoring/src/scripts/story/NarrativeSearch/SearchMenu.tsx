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

import gretchen from '../../grfetchen/';
import React, { useState } from 'react';
import { FiAlertTriangle } from 'react-icons/fi';
import { IoMenuOutline } from 'react-icons/io5';
import { useDispatch } from 'react-redux';
import { DropdownItem, DropdownMenu, DropdownToggle, UncontrolledDropdown } from 'reactstrap';

import { useBranchId, useDcmSelector, usePolyglot, useRouterQueryParam } from '../../hooks';
import { branchReindexing } from '../../presence/PresenceActions';
import { TOAST_TYPES, openToast } from '../../toast/actions';
import { DropdownAItem } from '../components/DropdownAItem';
import { useProjectAccess } from '../hooks';
import { ExternalLinkCheck } from './ExternalLinkCheck';
import { AllTypes, InitialTypes } from './SearchFilter';

export const SearchMenu: React.FC = () => {
  const polyglot = usePolyglot();
  const dispatch = useDispatch();
  const branchId = useBranchId();
  const projectAccess = useProjectAccess();
  const [modalOpen, setModalOpen] = useState(false);
  const searchParam = useRouterQueryParam('search');
  const htmlParam = useRouterQueryParam('html');
  const typesParam = useRouterQueryParam('types');
  const { probableAdmin } = useDcmSelector(state => state.layout);
  const html = htmlParam === 'true';
  const webQuery = JSON.stringify({
    query: searchParam,
    typeIds: html
      ? undefined
      : typesParam === 'all'
        ? Array.from(AllTypes)
        : !typesParam
          ? Array.from(InitialTypes)
          : typesParam.split(','),
  });
  const encodedQuery = encodeURIComponent(webQuery);
  const reindexing = useDcmSelector(state => state.presence.reindexing[branchId]);

  const downloadUrl = html
    ? `/api/v2/authoring/search/${branchId}/html/results.csv?query=${encodedQuery}`
    : `/api/v2/authoring/search/branch/${branchId}/results.csv?query=${encodedQuery}`;

  const doReindex = () => {
    gretchen
      .post(`/api/v2/authoring/branches/${branchId}/reindex`)
      .exec()
      .then(() => {
        dispatch(branchReindexing(branchId, true));
      })
      .catch(e => {
        window.console.log('Search error', e);
        dispatch(openToast('Reindex failed.', TOAST_TYPES.DANGER));
      });
  };

  // While one would expect to use <a download /> links, Chrome randomly fails
  // with "Network error" that never actually hits the server. Perceived wisdom
  // is to use target="_blank" instead.
  return (
    <>
      <UncontrolledDropdown
        id="search-menu"
        className="d-inline-block"
      >
        <DropdownToggle
          color="primary"
          outline
          caret
          className="border-0 asset-settings unhover-muted hover-white"
        >
          <IoMenuOutline size="1.75rem" />
        </DropdownToggle>
        <DropdownMenu end>
          <DropdownAItem
            id="results-download-button"
            target="_blank"
            href={downloadUrl}
            disabled={!searchParam && !typesParam}
          >
            Download Search Results
          </DropdownAItem>
          <DropdownItem divider />
          <DropdownAItem
            id="ip-download-button"
            target="_blank"
            href={`/api/v2/authoring/search/${branchId}/html/ipReport.csv`}
            disabled={!projectAccess.ContentRepo}
          >
            Download IP Report
          </DropdownAItem>
          <DropdownAItem
            id="link-download-button"
            target="_blank"
            href={`/api/v2/authoring/search/${branchId}/html/links.csv`}
            disabled={!projectAccess.ContentRepo}
          >
            Download Link Report
          </DropdownAItem>
          <DropdownItem
            id="link-check-button"
            disabled={!projectAccess.ContentRepo}
            onClick={() => setModalOpen(true)}
          >
            {polyglot.t('CHECK_EXTERNAL_LINKS')}
          </DropdownItem>
          {probableAdmin && (
            <>
              <DropdownItem divider />
              <DropdownItem
                id="reindex-button"
                disabled={reindexing}
                onClick={() => doReindex()}
                className="d-flex align-items-center"
              >
                Reindex Project
                <FiAlertTriangle className="ms-2 text-warning" />
              </DropdownItem>
            </>
          )}
        </DropdownMenu>
      </UncontrolledDropdown>
      <ExternalLinkCheck
        isOpen={modalOpen}
        toggle={() => setModalOpen(false)}
      />
    </>
  );
};
