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

import GradebookTableControls from './parts/GradebookTableControls';
import GradebookTableFooter from './parts/GradebookTableFooter';
import GradebookGradesLoader from './parts/GradebookGradesLoader';

import GradebookTables from './parts/GradebookTables';

const GradebookGradesPage = () => (
  <div className="gradebook-grades">
    <GradebookTableControls />
    <GradebookGradesLoader useOverlay={true}>
      <GradebookTables />
    </GradebookGradesLoader>
    <GradebookTableFooter />
  </div>
);

export default GradebookGradesPage;
