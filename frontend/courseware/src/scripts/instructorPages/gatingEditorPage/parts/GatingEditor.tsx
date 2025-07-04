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
import { Content, buildContentTree } from '../../../api/contentsApi';
import { Customizations } from '../../../api/customizationApi';
import { GateOverrides, getGatingOverrides } from '../../../api/gatingPoliciesApi';
import { fetchStudents } from '../../../api/rosterApi';
import Loader from '../../../components/Loader';
import { Loadable, sequenceObj } from '../../../types/loadable';
import { isPresent } from '../../../types/option';
import { CourseState } from '../../../loRedux';
import { StringMap } from '../../../loRedux/createKeyedReducer';
import { useLoadable } from '../../../utils/loaderHooks';
import { filter, includes, keys, map, mapKeys } from 'lodash';
import React, { useState } from 'react';
import { connect } from 'react-redux';

import { findAll } from '../../customization/Tree';
import GatingEditorContext from './GatingEditorContext';
import GatingEditorModuleCard from './GatingEditorModuleCard';
import GatingEditorSelectUser from './GatingEditorSelectUser';

export const getActiveActivityGates = (content: Content, overrides: GateOverrides) => {
  const existingGates =
    isPresent(content.gatingInformation.gate) &&
    isPresent(content.gatingInformation.gate.activityGatingPolicy)
      ? content.gatingInformation.gate.activityGatingPolicy.gates
      : null;

  const gateOverrides = overrides.assignment ? overrides.assignment[content.id] : [];

  return filter(
    existingGates,
    gate => !gate.disabled && !includes(gateOverrides, gate.assignmentId)
  );
};

const getGatingEditableContent = (
  contents: Content[],
  customizations: Customizations,
  overrides: GateOverrides
) => {
  const contentTree = buildContentTree(contents);

  return map(contentTree.children, moduleNode => {
    return {
      module: moduleNode.value,
      customization: customizations[moduleNode.value.id],
      gatedChildren: findAll(moduleNode, child => {
        //currently we only support editing assignment gates on children
        const activeActivityGates = getActiveActivityGates(child, overrides);
        return child.depth > 1 && activeActivityGates.length > 0;
      }),
    };
  });
};

const GatingEditor: React.FC<{
  contents: StringMap<Loadable<Content[]>>;
  contentOverlayUpdates: Customizations;
}> = ({ contents, contentOverlayUpdates }) => {
  const loader = () =>
    getGatingOverrides().then(overrides => {
      const users = keys(overrides.perUser);
      if (users.length === 0) {
        return {
          overrides,
          usersById: {},
        };
      }
      return fetchStudents(
        users.join(','),
        ['id'],
        'in',
        void 0,
        void 0,
        void 0,
        users.length
      ).then(users => {
        return { overrides, usersById: mapKeys(users.objects, 'id') };
      });
    });

  const [gatingData, , setGatingData] = useLoadable(loader);
  const overridesWithContent = sequenceObj({
    gatingData,
    contents: contents[window.lo_platform.user.id],
  });
  const [activeStudent, setActiveStudent] = useState<UserInfo | null>(null);
  const [customizations, setCustomisations] = useState<Customizations>(contentOverlayUpdates);

  return (
    <Loader loadable={overridesWithContent}>
      {({ gatingData, contents }) => {
        const editableContent = getGatingEditableContent(
          contents,
          customizations,
          gatingData.overrides
        );

        return (
          <GatingEditorContext.Provider
            value={{
              ...gatingData,
              activeStudent,
              contents,
              customizations,

              setGatingData,
              setActiveStudent,
              setCustomisations,
            }}
          >
            <GatingEditorSelectUser
              i18nKey="GATING_EDITOR_FOR"
              onClearStudent={() => setActiveStudent(null)}
              onSetStudent={student => {
                setActiveStudent(student);
                setGatingData(({ overrides, usersById }) => {
                  return {
                    overrides,
                    usersById: { ...usersById, [student.id]: student },
                  };
                });
              }}
            />
            {map(editableContent, ({ module, customization, gatedChildren }) => {
              return (
                <GatingEditorModuleCard
                  module={module}
                  customization={customization}
                  gatedChildren={gatedChildren}
                  key={module.id}
                />
              );
            })}
          </GatingEditorContext.Provider>
        );
      }}
    </Loader>
  );
};
export default connect((state: CourseState) => ({
  contents: state.api.contents,
  contentOverlayUpdates: state.api.contentOverlayUpdates,
}))(GatingEditor);
