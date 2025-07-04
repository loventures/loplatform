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
import { ContentWithRelationships } from '../../../courseContentModule/selectors/assembleContentView.ts';
import { ViewingAs } from '../../../courseContentModule/selectors/contentEntry';
import React from 'react';

import { RefreshingSubmissionActivityLoader, SubmissionActivityLoader } from './loaders';
import SubmissionActivityLearnerSticky from './SubmissionActivityLearner.tsx';

const SubmissionActivityLearnerLoader: React.FC<{
  content: ContentWithRelationships;
  viewingAs: ViewingAs;
  actualUser: UserInfo;
  printView?: boolean;
  onLoaded?: () => void | (() => void);
}> = ({ content, viewingAs, actualUser, printView, onLoaded }) => {
  const Loader = viewingAs.isPreviewing
    ? RefreshingSubmissionActivityLoader
    : SubmissionActivityLoader;
  return (
    <Loader
      content={content}
      viewingAs={viewingAs}
      actualUserId={actualUser.id}
      printView={printView}
    >
      <SubmissionActivityLearnerSticky
        content={content}
        viewingAs={viewingAs}
        actualUser={actualUser}
        printView={printView}
        onLoaded={onLoaded}
      />
    </Loader>
  );
};

export default SubmissionActivityLearnerLoader;
