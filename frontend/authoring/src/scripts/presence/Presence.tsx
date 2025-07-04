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
import { History } from 'history';
import qs from 'qs';
import React, { CSSProperties, MouseEventHandler, useCallback, useEffect, useState } from 'react';
import { BsWifiOff } from 'react-icons/bs';
import {
  TbLetterA,
  TbLetterB,
  TbLetterC,
  TbLetterD,
  TbLetterE,
  TbLetterF,
  TbLetterG,
  TbLetterH,
  TbLetterI,
  TbLetterJ,
  TbLetterK,
  TbLetterL,
  TbLetterM,
  TbLetterN,
  TbLetterO,
  TbLetterP,
  TbLetterQ,
  TbLetterR,
  TbLetterS,
  TbLetterT,
  TbLetterU,
  TbLetterV,
  TbLetterW,
  TbLetterX,
  TbLetterY,
  TbLetterZ,
  TbQuestionMark,
} from 'react-icons/tb';
import { useDispatch } from 'react-redux';
import { useHistory } from 'react-router';
import { Button, Tooltip } from 'reactstrap';

import { fromNow } from '../dateUtil';
import { withIconErrorBoundary } from '../hoc';
import { useDcmSelector, usePolyglot } from '../hooks';
import { subPageNames } from '../story/story';
import { Thunk } from '../types/dcmState';
import { toggleBranchChat } from './PresenceActions';
import { FullPresentUser, isFullPresentUser } from './PresenceReducer';
import PresenceService from './services/PresenceService';

const MaxProfiles = 4;

export const SecondsUntilAway = 180;
export const SecondsUntilLastActiveShown = 120;

const Letters = {
  A: TbLetterA,
  B: TbLetterB,
  C: TbLetterC,
  D: TbLetterD,
  E: TbLetterE,
  F: TbLetterF,
  G: TbLetterG,
  H: TbLetterH,
  I: TbLetterI,
  J: TbLetterJ,
  K: TbLetterK,
  L: TbLetterL,
  M: TbLetterM,
  N: TbLetterN,
  O: TbLetterO,
  P: TbLetterP,
  Q: TbLetterQ,
  R: TbLetterR,
  S: TbLetterS,
  T: TbLetterT,
  U: TbLetterU,
  V: TbLetterV,
  W: TbLetterW,
  X: TbLetterX,
  Y: TbLetterY,
  Z: TbLetterZ,
  '?': TbQuestionMark,
};

const gotoLocation =
  (handle: string, history: History): Thunk =>
  (_dispatch, getState) => {
    const { presence, projectGraph, graphEdits } = getState();
    const profile = presence.profiles[handle];
    if (isFullPresentUser(profile) && profile.location) {
      const { branchId, homeNodeName } = projectGraph;
      const location = profile.location;
      const name =
        projectGraph.nodes[location]?.typeId === 'competencySet.1' ? 'objectives' : location;
      const contextPath = subPageNames[name]
        ? homeNodeName
        : graphEdits.contentTree.contextPaths[name];
      const search = qs.stringify({ contextPath });
      history.push({ pathname: `/branch/${branchId}/story/${name}`, search });
    }
  };

const Face: React.FC<{
  profile: any;
  style: CSSProperties;
  size: string;
  onClick: MouseEventHandler;
}> = ({
  profile: { handle, fullName, letter, color, presence, imageUrl, ago },
  style,
  size,
  onClick,
}) => {
  const polyglot = usePolyglot();
  const dispatch = useDispatch();
  const history = useHistory();
  const [brokenImage, setBrokenImage] = useState(false);
  const [tooltipOpen, setTooltipOpen] = useState(false);
  const tooltipId = `present-${handle}`;
  const Letter = Letters[letter.toUpperCase()];
  return (
    <li
      id={tooltipId}
      className={`present-user ${presence}`}
      style={style}
    >
      <a
        className="present-user-circle"
        aria-label={fullName}
        href=""
        onClick={onClick}
        style={{ width: size, height: size }}
      >
        <div
          className="present-user-photo"
          style={{ backgroundColor: color }}
          role="presentation"
        >
          {imageUrl && !brokenImage ? (
            <img
              className="user-photo"
              src={imageUrl}
              alt=""
              onError={() => setBrokenImage(true)}
            />
          ) : Letter ? (
            <Letter
              className="user-letter"
              size="75%"
              strokeWidth={1.5}
            />
          ) : (
            <div className="user-letter">{letter}</div>
          )}
          <div className="user-mask"></div>
        </div>
      </a>
      <Tooltip
        isOpen={tooltipOpen}
        placement="bottom"
        target={tooltipId}
        toggle={() => setTooltipOpen(!tooltipOpen)}
        delay={250}
        autohide={false}
      >
        <div>{fullName}</div>
        {ago && <div>{polyglot.t('LAST_ACTIVE', { ago })}</div>}
        <div>
          <Button
            size="sm"
            color="link"
            className="text-primary-light"
            onClick={() => {
              dispatch(gotoLocation(handle, history));
              setTooltipOpen(false);
            }}
          >
            Go to location
          </Button>
        </div>
      </Tooltip>
    </li>
  );
};

const Presence: React.FC<{ compact?: boolean }> = ({ compact }) => {
  const dispatch = useDispatch();
  const polyglot = usePolyglot();
  const branch = useDcmSelector(state => state.layout.branchId);
  const { offline, online, presentUsers, profiles, unreadCount, idling, tabVisible } =
    useDcmSelector(state => state.presence);
  const chatEnabled = useDcmSelector(state => state.configuration.chatEnabled);
  const now = new Date().getTime();
  const yourself = useDcmSelector(state => state.user.profile.handle);
  const visible = useDcmSelector(state => state.user.profile.user_type !== 'Overlord');

  const profileWidth = compact ? 2 : 2.375;
  const profileOverlap = compact ? 0.3 : 0.375;

  // Schedule state changes for when users transition from active to idle so the present
  // users widget updates itself as people go idle.
  const [generation, setGeneration] = useState(0);
  useEffect(() => {
    // Compute how long until each user transitions to idle
    const transitions = presentUsers
      .map(([handle, lastActive]) => SecondsUntilAway * 1000 + lastActive - now) // eslint-disable-line
      .filter(idle => idle > 0);
    if (transitions.length) {
      const timeout = setTimeout(() => setGeneration(1 + generation), Math.min(...transitions));
      return () => clearTimeout(timeout);
    }
  }, [presentUsers, generation]);

  const openChat = useCallback(
    (e: React.MouseEvent) => {
      e.preventDefault();
      if (chatEnabled) dispatch(toggleBranchChat(branch));
    },
    [chatEnabled, branch]
  );

  const reconnect = useCallback(
    (e: React.MouseEvent) => {
      e.preventDefault();
      PresenceService.start({ branchId: branch, visible });
    },
    [branch, visible]
  );

  // if you are offline and active we will try to reconnect after 100ms
  // then after 5s, 10s, 20s, ...
  const doReconnect = !online && !idling && tabVisible;
  useEffect(() => {
    if (doReconnect) {
      let delay = 2500,
        timeout;
      const tryConnect = () => {
        if (!PresenceService.presenceState.online) {
          delay = Math.min(delay * 2, 300000);
          timeout = setTimeout(tryConnect, delay);
          PresenceService.start({ branchId: branch, visible });
        }
      };
      timeout = setTimeout(tryConnect, 100);
      return () => clearTimeout(timeout);
    }
  }, [doReconnect, branch, visible]);

  const presences = presentUsers
    .map(([handle, lastActive]) => [handle, lastActive, profiles[handle]] as const)
    .filter(item => isFullPresentUser(item[2]) && item[0] !== yourself)
    .map(([handle, lastActive, profil]) => {
      const profile = profil as FullPresentUser;
      const imageUrl = profile.thumbnailId
        ? `/api/v2/profiles/${handle}/thumbnail/${profile.thumbnailId};size=medium`
        : null;
      const idle = (now - lastActive) / 1000;
      const presence = idle >= SecondsUntilAway ? 'Away' : 'Active';
      const ago = idle >= SecondsUntilLastActiveShown && fromNow(lastActive);
      return { ...profile, presence, imageUrl, ago };
    })
    .sort(({ fullName: a }, { fullName: b }) => a.toLowerCase().localeCompare(b.toLowerCase()));
  const presenceN = presences.length;
  const presenceM = Math.min(presenceN, MaxProfiles);
  const presenceWidth = Math.max(0, presenceM * profileWidth - (presenceM - 1) * profileOverlap);
  const presenceX = index =>
    presenceN === 1
      ? 0
      : ((presenceWidth - profileWidth) * (presenceN - 1 - index)) / (presenceN - 1);
  return offline ? (
    <Button
      color="transparent"
      className="presence-offline align-self-center text-danger p-1 border-0 text-danger d-flex"
      title={polyglot.t('PRESENCE_OFFLINE')}
      onClick={reconnect}
    >
      <BsWifiOff size="1.2rem" />
    </Button>
  ) : (
    <>
      <ul
        className={classNames('present-users list-unstyled align-self-center', { compact })}
        style={{ width: `${presenceWidth}rem`, height: `${profileWidth}rem` }}
      >
        {presences.map((profile, index) => (
          <Face
            key={profile.handle}
            profile={profile}
            onClick={openChat}
            style={{
              right: `${presenceX(index)}rem`,
            }}
            size={`${profileWidth}rem`}
          />
        ))}
      </ul>
      <div className="chat-count">
        {chatEnabled && !!unreadCount && (
          <a
            className="chat-bubble"
            href=""
            onClick={openChat}
            title={polyglot.t('CHAT_UNREAD_MESSAGES', { unreadCount })}
          >
            <i className="material-icons md-24">chat_bubble</i>
            <span className="count">{Math.min(99, unreadCount)}</span>
          </a>
        )}
      </div>
    </>
  );
};

export default withIconErrorBoundary(Presence);
