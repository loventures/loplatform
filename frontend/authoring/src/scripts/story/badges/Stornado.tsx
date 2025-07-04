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

import classNames from 'classnames';
import React from 'react';
import { GiSpiderWeb, GiTornado } from 'react-icons/gi';

import { useIsReusedContent } from '../../graphEdit';
import {
  useCustomizedAsset,
  useRemoteAssetProject,
} from '../../structurePanel/projectGraphActions';
import { NodeName } from '../../types/asset';
import { useProjectAccess } from '../hooks';

export const Stornado: React.FC<{
  name: NodeName | undefined;
  size?: 'sm' | 'sm-no-ml' | 'md' | 'md-noml';
  className?: string;
}> = ({ name, size = 'md', className }) => {
  const remote = useRemoteAssetProject(name);
  const customized = useCustomizedAsset(name);
  const projectAccess = useProjectAccess();
  const reused = useIsReusedContent(name);

  return (
    <>
      {reused ? (
        <GiSpiderWeb
          size={size === 'sm' || size === 'sm-no-ml' ? '12px' : '16px'}
          title="Linked Content"
          className={classNames(
            'reused-content flex-shrink-0',
            size === 'sm' ? 'ms-1' : size === 'md' ? 'ms-2' : undefined,
            className
          )}
        />
      ) : null}
      {remote && projectAccess.ViewMultiverse ? (
        <GiTornado
          size={size === 'sm' || size === 'sm-no-ml' ? '12px' : '16px'}
          title={`From ${remote.branchName}${customized ? ' (modified)' : ''}`}
          className={classNames(
            'gi-tornado flex-shrink-0',
            size === 'sm' ? 'ms-1' : size === 'md' ? 'ms-2' : undefined,
            customized ? 'text-warning' : 'text-muted',
            className
          )}
        />
      ) : null}
    </>
  );
};
