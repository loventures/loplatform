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

import axios from 'axios';
import Course from '../../bootstrap/course';
import { useFeedbackOpen } from '../../feedback/FeedbackStateService';
import { useCourseSelector } from '../../loRedux';
import { toggleQnaSideBar } from '../../qna/qnaActions';
import {
  InstructorQnaPreviewPageLink,
  QnaPageLink,
  SearchLink,
  SendMessagePageLink,
} from '../../utils/pageLinks';
import { AuthoringAppRight } from '../../utils/rights';
import { useTranslation } from '../../i18n/translationContext';
import { appIsFramed } from '../../utilities/deviceType';
import { history } from '../../utilities/history';
import { selectActualUser, selectRouter } from '../../utilities/rootSelectors';
import React, { useState } from 'react';
import { GiTeacher } from 'react-icons/gi';
import { IoMdPerson, IoMdSchool } from 'react-icons/io';
import { allowDirectMessaging, contentSearch } from '../../utilities/preferences';
import { useDispatch } from 'react-redux';
import {
  Button,
  DropdownItem,
  DropdownMenu,
  DropdownToggle,
  Modal,
  ModalBody,
  ModalFooter,
  ModalHeader,
  UncontrolledDropdown,
} from 'reactstrap';

import AppLogout from './AppLogout';
import { Link } from 'react-router-dom';

const adminLink = window.lo_platform.adminLink;
const authoringLink = window.lo_platform.authoringLink;

type PreviewRole = 'Instructor' | 'Learner';

type Studont = {
  id: number;
  userName: string;
};

const doStudo = (role: PreviewRole) => {
  axios.post<Studont>(`/api/v2/lwc/${Course.id}/studo`, { role }).then(() => {
    window.location.assign(
      `${window.lo_platform.course.url}/${role.toLowerCase()}${window.location.hash}`
    );
  });
};

const getStudont = (role: PreviewRole): Promise<Studont | null> =>
  axios.get<Studont | null>(`/api/v2/lwc/${Course.id}/studo?role=${role}`).then(res => res.data);

const AppNavUserDropdown: React.FC<{ authoringUrl: string }> = ({ authoringUrl }) => {
  // close the feedback on open menu becaus of z-index tragedy
  const dispatch = useDispatch();
  const [, , toggleFeedback] = useFeedbackOpen();
  const actualUser = useCourseSelector(selectActualUser);
  const translate = useTranslation();
  const canAuthor = actualUser.rights.includes(AuthoringAppRight);
  const isPreviewing = actualUser.user_type === 'Preview';
  const isStudent = actualUser.isStudent;
  const [confirmStudo, setConfirmStudo] = useState<PreviewRole | undefined>(undefined);
  const path = useCourseSelector(s => selectRouter(s).path);

  const canLearnerView = actualUser.isInstructor && !isPreviewing;
  // Let authors bounce in as instructors in preview sections
  const canInstructorView =
    canLearnerView && window.lo_platform.course.groupType === 'PreviewSection';

  const cancelStudo = () => setConfirmStudo(undefined);
  const studo = (role: PreviewRole) => {
    // Don't bother confirming studo in preview/test sections
    if (window.lo_platform.course.groupType === 'CourseSection') {
      getStudont(role).then(res => {
        if (res) {
          doStudo(role);
        } else {
          setConfirmStudo(role);
        }
      });
    } else {
      doStudo(role);
    }
  };
  return (
    <UncontrolledDropdown
      className="user-dropdown"
      inNavbar
    >
      <DropdownToggle
        id="nav-user-dropdown"
        className="border-white"
        color="outline-primary"
        aria-label={translate('APP_HEADER_SHOW_USER_OPTIONS')}
        title={translate('APP_HEADER_SHOW_USER_OPTIONS')}
        onClick={() => {
          // The menu cannot appear over sidebars, so hide all the sidebars...
          toggleFeedback(false);
          if (isStudent) {
            dispatch(toggleQnaSideBar({ open: false }));
          } else if (InstructorQnaPreviewPageLink.match(path)) {
            // This sidebar doesn't hide so navigate away!!!
            history.push(QnaPageLink.toLink());
          }
        }}
      >
        <span className="flex-center-center">
          {isPreviewing && isStudent ? (
            <IoMdSchool
              size="1.5rem"
              aria-hidden={true}
            />
          ) : isPreviewing ? (
            <GiTeacher
              size="1.5rem"
              aria-hidden={true}
            />
          ) : (
            <IoMdPerson
              size="1.5rem"
              aria-hidden={true}
            />
          )}

          <span className="menu-user-name nav-user-fullname">
            &nbsp;{actualUser.fullName}&nbsp;
          </span>
          <span
            className="icon icon-chevron-down"
            aria-hidden
          />
        </span>
      </DropdownToggle>
      <DropdownMenu end>
        {canAuthor && (
          <DropdownItem
            className="lo-authoring-link"
            id="nav-user-dropdown-authoring"
            href={authoringLink}
          >
            {translate('APP_HEADER_AUTHORING')}
          </DropdownItem>
        )}

        {adminLink && (
          <DropdownItem
            className="lo-admin-link"
            id="nav-user-dropdown-admin"
            href={adminLink}
          >
            {translate('APP_HEADER_ADMIN')}
          </DropdownItem>
        )}

        {(canAuthor || adminLink) && <DropdownItem divider />}

        {canAuthor && (
          <DropdownItem
            className="d-md-none"
            href={authoringUrl}
          >
            {translate('PAGE_HEADER_EDIT_IN_AUTHORING')}
          </DropdownItem>
        )}
        {contentSearch && (
          <DropdownItem
            tag={Link}
            to={SearchLink.toLink}
            className="d-md-none"
          >
            {translate('PAGE_HEADER_SEARCH')}
          </DropdownItem>
        )}
        {allowDirectMessaging && (
          <DropdownItem
            tag={Link}
            to={SendMessagePageLink.toLink({})}
            className="d-md-none"
          >
            {translate('PAGE_HEADER_SEND_MESSAGE')}
          </DropdownItem>
        )}

        {(canAuthor || contentSearch || allowDirectMessaging) && (
          <DropdownItem
            divider
            className="d-md-none"
          />
        )}

        {canInstructorView && (
          <DropdownItem
            className="lo-studo-link"
            id="nav-user-dropdown-studo"
            onClick={() => studo('Instructor')}
          >
            {translate('APP_HEADER_INSTRUCTOR_VIEW')}
          </DropdownItem>
        )}
        {canLearnerView && (
          <DropdownItem
            className="lo-studo-link"
            id="nav-user-dropdown-studo"
            onClick={() => studo('Learner')}
          >
            {translate('APP_HEADER_STUDO')}
          </DropdownItem>
        )}
        {(!appIsFramed || isPreviewing) && (
          <DropdownItem
            tag={AppLogout}
            isPreviewing={isPreviewing}
            isStudent={isStudent}
          />
        )}
      </DropdownMenu>
      {confirmStudo && (
        <Modal
          isOpen
          toggle={cancelStudo}
        >
          <ModalHeader toggle={cancelStudo}>{translate('APP_HEADER_STUDO')}</ModalHeader>
          <ModalBody>{translate('STUDO_INSTRUCTIONS')}</ModalBody>
          <ModalFooter>
            <Button
              outline
              color="primary"
              onClick={cancelStudo}
            >
              {translate('CANCEL')}
            </Button>
            <Button
              color="primary"
              onClick={() => doStudo(confirmStudo)}
            >
              {translate('CONTINUE')}
            </Button>
          </ModalFooter>
        </Modal>
      )}
    </UncontrolledDropdown>
  );
};

export default AppNavUserDropdown;
