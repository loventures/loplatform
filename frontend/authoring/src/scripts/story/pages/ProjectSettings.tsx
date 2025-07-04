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

import React, { useState } from 'react';

import { useCurrentAssetName, useEditedAsset } from '../../graphEdit';
import { NewAsset } from '../../types/asset';
import { ProjectActionsMenu } from './ProjectSettings/ProjectActionsMenu';
import { ProjectSettingsForm } from './ProjectSettings/ProjectSettingsForm';
import { useDcmSelector } from '../../hooks';

const ProjectSettings: React.FC = () => {
  const name = useCurrentAssetName();
  const course = useEditedAsset(name) as NewAsset<'course.1'>;
  const [dirty, setDirty] = useState(false);
  const project = useDcmSelector(s => s.layout.project);

  return (
    <div className="project-settings">
      <div style={{ display: 'grid', gridTemplateColumns: 'auto 1fr auto' }}>
        <ProjectActionsMenu dirty={dirty} />
        <div className="d-flex align-items-center justify-content-center minw-0 text-muted">
          Project Settings
        </div>
        <div className="button-spacer d-flex align-items-center justify-content-center actions-icon"></div>
      </div>
      <div className="asset-title d-flex flex-column mt-3">
        <h2>{project.name}</h2>
      </div>
      <ProjectSettingsForm
        dirty={dirty}
        setDirty={setDirty}
      />
    </div>
  );
};

export default ProjectSettings;
