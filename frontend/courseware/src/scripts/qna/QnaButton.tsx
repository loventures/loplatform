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
import { useSidepanelOpen } from '../commonPages/sideNav/SideNavStateService';
import { useCourseSelector } from '../loRedux';
import { toggleQnaSideBar } from '../qna/qnaActions';
import { useQnaSummary } from '../qna/qnaHooks';
import { selectContent } from '../courseContentModule/selectors/contentEntrySelectors';
import { useTranslation } from '../i18n/translationContext';
import React from 'react';
import { useDispatch } from 'react-redux';
import { useMedia } from 'react-use';
import { Button, DropdownItem } from 'reactstrap';

import Tutorial from '../tutorial/Tutorial';
import QnaIcon from './QnaIcon';

const QnaButton: React.FC<{ dropdown?: boolean }> = ({ dropdown }) => {
  const dispatch = useDispatch();
  const wide = useMedia('(min-width: 94em)'); // epicscreen
  const [, toggleSidepanel] = useSidepanelOpen();
  const content = useCourseSelector(selectContent);
  const summary = useQnaSummary(content.id);
  const translate = useTranslation();

  const onClick = () => {
    if (!wide) toggleSidepanel(false);
    dispatch(toggleQnaSideBar(undefined));
  };

  return (
    <>
      {dropdown ? (
        <DropdownItem onClick={onClick}>{translate('QNA_SIDEBAR_TITLE')}</DropdownItem>
      ) : (
        <Button
          id="qna-button"
          color="primary"
          outline
          className={classNames('border-white px-2 position-relative', {
            'open-question': summary?.answered,
            'has-questions': summary?.count,
          })}
          onClick={onClick}
          title={translate('QNA_SIDEBAR_TITLE')}
          aria-label={translate('QNA_SIDEBAR_TITLE')}
        >
          <QnaIcon
            style={{ height: '2rem', width: '2rem' }}
            aria-hidden={true}
          />
        </Button>
      )}
      <Tutorial name="qna" />
    </>
  );
};

export default QnaButton;
