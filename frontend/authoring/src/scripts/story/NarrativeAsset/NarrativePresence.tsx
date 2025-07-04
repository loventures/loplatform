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

import React, { useEffect, useMemo, useState } from 'react';
import { UncontrolledTooltip } from 'reactstrap';

import { fromNow } from '../../dateUtil';
import { useDcmSelector, usePolyglot } from '../../hooks';
import { SecondsUntilAway, SecondsUntilLastActiveShown } from '../../presence/Presence';
import { FullPresentUser, isFullPresentUser } from '../../presence/PresenceReducer';

const MaxProfiles = 4;
const Infinitude = 600000;

// name is garbage for a unique id, it should be present session id instead
const Face: React.FC<{
  profile: FullPresentUser & { color: string; ago: string };
  name: string;
  right: string;
  size: string;
}> = ({ profile: { handle, fullName, color, ago }, right, name, size }) => {
  const polyglot = usePolyglot();
  const tooltipId = `present-${name}-${handle}`;
  return (
    <li
      id={tooltipId}
      className="present-user"
      style={{ right, width: size, height: size, backgroundColor: color }}
      aria-label={fullName}
    >
      <UncontrolledTooltip
        placement="bottom"
        target={tooltipId}
        delay={0}
        innerClassName="bg-dark"
      >
        <div>{fullName}</div>
        {ago && <div>{polyglot.t('LAST_ACTIVE', { ago })}</div>}
      </UncontrolledTooltip>
    </li>
  );
};

// TODO: Why does chrome mark me as having left the current page, but not
// the current project, when I swipe to a new desktop???

export const usePresentUsers = (handles: string[]) => {
  const profiles = useDcmSelector(state => state.presence.profiles);

  // Schedule state changes for when users transition from active to idle so the present
  // users widget updates itself as people go idle.
  const [generation, setGeneration] = useState(0);

  const presences = useMemo(() => {
    const now = new Date().getTime();
    return handles
      .map(handle => profiles[handle])
      .filter(isFullPresentUser)
      .map(profile => {
        // ms until idle
        const idle = now - profile.lastActive;
        const away = SecondsUntilAway * 1000 - idle;
        const opacity = away <= 0 ? 50 : 100;
        // this is semiduplicative of the color on the profile, just with lightness added
        const color = `hsla(${((profile.id || 0) * 47) % 360}, 50%, 40%, ${opacity}%)`;
        const ago = idle >= SecondsUntilLastActiveShown * 1000 && fromNow(profile.lastActive);
        return { ...profile, color, ago, away };
      })
      .sort(({ fullName: a }, { fullName: b }) => a.toLowerCase().localeCompare(b.toLowerCase()));
  }, [handles, profiles, generation]);

  useEffect(() => {
    // Compute how long until next user transitions to idle
    let delay = Infinitude;
    for (const { away } of presences) {
      if (away > 0 && away < delay) delay = away;
    }
    if (delay < Infinitude) {
      const timeout = setTimeout(() => setGeneration(1 + generation), delay);
      return () => clearTimeout(timeout);
    }
  }, [presences]);

  return presences;
};

const NarrativePresenceFaces: React.FC<{ name?: string; handles: string[] }> = ({
  name,
  handles,
}) => {
  const presences = usePresentUsers(handles);
  const profileWidth = 1.5;
  const profileOverlap = 0.5;
  const presenceN = presences.length;
  const presenceM = Math.min(presenceN, MaxProfiles);
  const presenceWidth = Math.max(0, presenceM * profileWidth - (presenceM - 1) * profileOverlap);
  const presenceX = index =>
    presenceN === 1
      ? 0
      : ((presenceWidth - profileWidth) * (presenceN - 1 - index)) / (presenceN - 1);
  return presenceN ? (
    <ul
      className="present-users list-unstyled"
      style={{ width: `${presenceWidth}rem`, height: `${profileWidth}rem` }}
    >
      {presences.map((profile, index) => (
        <Face
          key={profile.handle}
          name={name}
          profile={profile}
          right={`${presenceX(index)}rem`}
          size={`${profileWidth}rem`}
        />
      ))}
    </ul>
  ) : null;
};

const NarrativePresence: React.FC<{ name: string; children: React.ReactElement }> = ({
  name,
  children,
}) => {
  const present = useDcmSelector(state => state.presence.usersAtAsset[name]);
  return (
    <div className="preview-menu-wrapper">
      {present && (
        <NarrativePresenceFaces
          name={name}
          handles={present}
        />
      )}
      {children}
    </div>
  );
};

export default NarrativePresence;
