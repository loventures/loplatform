/*
 * LO Platform copyright (C) 2007–2026 LO Ventures LLC.
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

import React, { useEffect, useRef } from 'react';

import { checkSession } from '../services/';

const OneMinute = 60000;

interface SessionExpiredProps {
  onExpired?: () => void;
  children?: React.ReactNode;
}

const SessionExpired: React.FC<SessionExpiredProps> = ({ onExpired, children }) => {
  const mounted = useRef(false);
  const timeout = useRef<ReturnType<typeof setTimeout>>(undefined);

  useEffect(() => {
    const schedule = () => {
      if (mounted.current) {
        timeout.current = setTimeout(isSessionExpired, OneMinute);
      }
    };

    const isSessionExpired = (): Promise<void> =>
      checkSession().then(res => {
        const { valid, err } = res as { valid?: boolean; err?: unknown };
        if (valid) {
          schedule();
        } else if (err) {
          console.log(err);
          schedule();
        } else {
          onExpired?.();
        }
      });

    mounted.current = true;
    isSessionExpired();

    return () => {
      mounted.current = false;
      clearTimeout(timeout.current);
    };
  }, []);

  return <>{children}</>;
};

export default SessionExpired;
