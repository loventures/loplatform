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

import { UserInfo } from '../../../../loPlatform';
import { Content } from '../../../api/contentsApi';
import { Customizations } from '../../../api/customizationApi';
import { GateOverrides } from '../../../api/gatingPoliciesApi';
import { LoadableSetDataWithCB } from '../../../utils/loaderHooks';
import { createContext } from 'react';

type GatingEditorData = {
  overrides: GateOverrides;
  usersById: Record<string, UserInfo>;
  activeStudent: UserInfo | null;
  contents: Content[];
  customizations: Customizations;

  setGatingData: LoadableSetDataWithCB<{
    overrides: GateOverrides;
    usersById: Record<string, UserInfo>;
  }>;
  setActiveStudent: (student: UserInfo) => void;
  setCustomisations: (customizations: Customizations) => void;
};

const GatingEditorContext = createContext<GatingEditorData | null>(null);

export default GatingEditorContext;
