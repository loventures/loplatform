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

import React from 'react';
import { Route, Routes, useParams } from 'react-router-dom';

import Dropbox from './Dropbox';

// Descendant routes (mounted under DcmApp's `dropbox/*`), so paths are relative to dropboxPath.
const DropboxFolder: React.FC = () => {
  const { folder } = useParams<{ folder: string }>();
  return <Dropbox folder={parseInt(folder!)} />;
};

const DropboxRoutes: React.FC = () => {
  return (
    <Routes>
      <Route
        index
        element={<Dropbox folder={null} />}
      />
      <Route
        path=":folder"
        element={<DropboxFolder />}
      />
    </Routes>
  );
};

export default DropboxRoutes;
