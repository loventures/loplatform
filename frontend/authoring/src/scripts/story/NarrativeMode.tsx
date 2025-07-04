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
import React, { Suspense } from 'react';
import { Spinner } from 'reactstrap';
import CompetenciesEditor from '../competency/CompetenciesEditor';
import { useFeedbackOn, useFeedbackOpen } from '../feedback/feedbackHooks';
import GradebookPage from '../gradebook/GradebookPage';
import { useCurrentAssetName, useCurrentContextPath, useEditedAssetTypeId } from '../graphEdit';
import { useRouterPathVariable } from '../hooks';
import { NarrativeDeleted } from '../revision/NarrativeDeleted';
import { useProjectGraphLoading } from '../structurePanel/projectGraphHooks';
import TimelineApp from '../timeline/TimelineApp';
import { FullIndex } from './FullIndex';
import { useProjectAccess } from './hooks';
import { useContentAccess } from './hooks/useContentAccess';
import { HtmlFeedback } from './HtmlFeedback';
import { NarrativeAsset } from './NarrativeAsset';
import { NarrativeChildren } from './NarrativeChildren';
import NarrativeHooks from './NarrativeHooks';
import { NarrativeMultiverse } from './NarrativeMultiverse';
import { NarrativeSearch } from './NarrativeSearch';
import DefaultsPage from './pages/DefaultsPage';
import { ProjectHistory } from './pages/ProjectHistory';
import ProjectSettings from './pages/ProjectSettings';
import { subPageNames } from './story';
import {
  useIsInlineNarrativeView,
  useIsStoryEditMode,
  useRevisionCommit,
  useStorySelector,
} from './storyHooks';
import { GiOrbital0, GiOrbital1, GiOrbital2 } from '../projects/icons';

const NarrativeGraph = React.lazy(
  () => import(/* webpackChunkName: "NarrativeGraph" */ './NarrativeGraph')
);

// https://lepture.com/en/2022/windows-country-flags-emoji
const windows = /windows/i.test(navigator.userAgent);

export const NarrativeMode: React.FC = () => {
  const inlineView = useIsInlineNarrativeView();
  const feedbackOpen = useFeedbackOpen();
  const feedbackOn = useFeedbackOn();
  const current = useRouterPathVariable('name');
  const name = useCurrentAssetName();
  const typeId = useEditedAssetTypeId(name);
  const editMode = useIsStoryEditMode();
  const contextPath = useCurrentContextPath();
  const loading = useProjectGraphLoading();
  const logicalView = !!subPageNames[current]; // one of the logical pages
  const demarginalize = feedbackOpen && inlineView && !logicalView;
  const commit = useRevisionCommit();
  const projectAccess = useProjectAccess();
  const contentAccess = useContentAccess(name);
  const feedbackEnabled = contentAccess.AddFeedback && feedbackOn && !commit;
  const flagMode = useStorySelector(s => s.flagMode);

  return (
    <>
      <div
        id="narrative-mode"
        className={classNames(
          'narrative-editor narrative-mode py-0 py-sm-4 position-relative',
          feedbackEnabled && 'feedback-enabled',
          demarginalize && 'demargin-right',
          editMode && 'flash-edit',
          flagMode && 'flag-mode',
          windows && 'windows'
        )}
      >
        <NarrativeHooks />
        {loading ? (
          <div className="loading orbitals gray-600">
            <GiOrbital0 size="4rem" />
            <GiOrbital1 size="4rem" />
            <GiOrbital2 size="4rem" />
          </div>
        ) : !typeId ? (
          <NarrativeDeleted name={name} />
        ) : logicalView ? (
          <div className="container narrative-container">
            <div className="story-element">
              {current === 'index' ? (
                <FullIndex
                  name={name}
                  contextPath={contextPath}
                />
              ) : current === 'search' ? (
                <NarrativeSearch />
              ) : current === 'graph' ? (
                <Suspense
                  fallback={
                    <div className="d-flex align-items-center justify-content-center">
                      <div className="my-5 text-center fade-in text-muted">
                        <Spinner />
                      </div>
                    </div>
                  }
                >
                  <NarrativeGraph />
                </Suspense>
              ) : projectAccess.ViewProjectHistory && current === 'history' ? (
                <ProjectHistory />
              ) : projectAccess.ViewMultiverse && current === 'multiverse' ? (
                <NarrativeMultiverse />
              ) : projectAccess.ViewObjectives && current === 'objectives' ? (
                <CompetenciesEditor />
              ) : projectAccess.ViewTimeline && current === 'timeline' ? (
                <TimelineApp />
              ) : projectAccess.ViewGradebook && current === 'gradebook' ? (
                <GradebookPage />
              ) : contentAccess.EditSettings && current === 'defaults' ? (
                <DefaultsPage />
              ) : projectAccess.ProjectSettings && current === 'settings' ? (
                <ProjectSettings />
              ) : (
                <div className="text-center text-danger my-5">
                  Your rôle does not grant you access to this page.
                </div>
              )}
            </div>
          </div>
        ) : !inlineView ? (
          <NarrativeAsset
            key={name}
            name={name}
            contextPath={contextPath}
            mode="apex"
          />
        ) : (
          <>
            <NarrativeAsset
              key={`${name}-top`}
              name={name}
              contextPath={contextPath}
              mode="apex"
              bottom={false}
            />

            <NarrativeChildren
              name={name}
              contextPath={contextPath}
            />

            <NarrativeAsset
              key={`${name}-bottom`}
              name={name}
              contextPath={contextPath}
              mode="apex"
              top={false}
            />
          </>
        )}
      </div>
      <HtmlFeedback />
    </>
  );
};
