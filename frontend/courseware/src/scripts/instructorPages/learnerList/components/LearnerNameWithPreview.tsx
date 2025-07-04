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
import Course from '../../../bootstrap/course';
import { pickPreviewUserActionCreator } from '../../../landmarks/learnerPreviewHeader/actions';
import { useTranslation } from '../../../i18n/translationContext';
import React from 'react';
import { AiOutlineEye } from 'react-icons/ai';
import { GoMail } from 'react-icons/go';
import { LuGhost } from 'react-icons/lu';
import { MdOutlineDisabledVisible } from 'react-icons/md';
import { ConnectedProps, connect } from 'react-redux';
import { Button } from 'reactstrap';

import { NameFormatter } from '../learnerListStore';

type LearnerNameWithPreviewOwnProps = {
  learner: UserInfo;
  formatName: NameFormatter;
  openModal?: (ui: UserInfo) => void;
};

const connector = connect(null, {
  viewAsLearner: pickPreviewUserActionCreator,
});

type PropsFromRedux = ConnectedProps<typeof connector>;

const LearnerNameWithPreview: React.FC<LearnerNameWithPreviewOwnProps & PropsFromRedux> = ({
  viewAsLearner,
  learner,
  formatName,
  openModal,
}) => {
  const translate = useTranslation();
  const preview = learner.user_type === 'Preview' && Course.groupType !== 'PreviewSection';

  return (
    <div className="d-flex justify-content-between align-items-center">
      {preview ? (
        <span>
          <LuGhost className="text-warning me-2" />
          {formatName(learner)}
        </span>
      ) : (
        formatName(learner)
      )}
      {learner.inactive ? (
        <Button
          color="danger"
          outline
          className="border-0 px-2 not-a-student d-flex align-items-center"
          disabled
        >
          <MdOutlineDisabledVisible />
        </Button>
      ) : (
        <div className="d-flex">
          {openModal && (
            <Button
              color="primary"
              outline
              className="border-0 px-2 message-learner d-flex align-items-center"
              onClick={() => openModal(learner)}
              title={translate('SEND_MESSAGE_TITLE')}
            >
              <GoMail />
            </Button>
          )}
          <Button
            color="primary"
            outline
            className="border-0 px-2 preview-as-learner d-flex align-items-center"
            onClick={() => viewAsLearner(learner)}
            title={translate('INSTRUCTOR_PREVIEW_AS_LEARNER')}
          >
            <AiOutlineEye />
          </Button>
        </div>
      )}
    </div>
  );
};

export default connector(LearnerNameWithPreview);
