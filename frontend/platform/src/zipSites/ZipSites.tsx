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

import Polyglot from 'node-polyglot';
import { useDispatch } from 'react-redux';
import { Route, Routes, useParams } from 'react-router-dom';

import Crumb from '../components/crumbRoute';
import { setPortalAlertStatus } from '../redux/actions/MainActions';
import { useTranslations } from '../redux/state';
import RevisionTable from './RevisionTable';
import ZipSiteTable from './ZipSiteTable';
import { IoFolderOpenOutline } from 'react-icons/io5';

// Reads the :siteId route param so the revisions table can be a plain route element.
const ZipSiteRevisions: React.FC<{
  setLastCrumb: (title: string) => void;
  setPortalAlertStatus: (error: boolean, success: boolean, message: string) => void;
  translations: Polyglot;
}> = ({ setLastCrumb, setPortalAlertStatus, translations }) => {
  const { siteId = '' } = useParams<{ siteId: string }>();
  return (
    <RevisionTable
      setLastCrumb={setLastCrumb}
      siteId={Number.parseInt(siteId, 10)}
      setPortalAlertStatus={setPortalAlertStatus}
      translations={translations}
    />
  );
};

const ZipSites = () => {
  const T = useTranslations();
  const dispatch = useDispatch();
  const setPortalAlert = (error: boolean, success: boolean, message: string) =>
    dispatch(setPortalAlertStatus(error, success, message));
  return (
    <Routes>
      <Route
        path=""
        element={<ZipSiteTable />}
      />

      <Route
        path=":siteId"
        element={
          <Crumb title="">
            {({ setLastCrumb }) => (
              <ZipSiteRevisions
                setLastCrumb={setLastCrumb}
                setPortalAlertStatus={setPortalAlert}
                translations={T}
              />
            )}
          </Crumb>
        }
      />
    </Routes>
  );
};

ZipSites.pageInfo = {
  identifier: 'zipSites',
  link: '/ZipSites',
  icon: IoFolderOpenOutline,
  group: 'media',
  right: 'loi.cp.zip.ZipSiteAdminRight',
  entity: 'zipSites',
};

export default ZipSites;
