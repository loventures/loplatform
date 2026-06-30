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

import classnames from 'classnames';
import React, { useEffect, useRef, useState } from 'react';

import { TranslationProvider, useTranslation } from '../../i18n/translationContext';

type SettingKey = 'autoMarkUnread' | 'closeDiscussion';

interface DiscussionBoardSettingsProps {
  settings: Record<SettingKey, boolean> & Record<string, any>;
  updateSettings: Record<SettingKey, (update: Record<string, boolean>) => void>;
}

const SETTINGS: Array<{ key: SettingKey; label: string; tooltip: string }> = [
  {
    key: 'autoMarkUnread',
    label: 'DISCUSSION_SETTING_MARK_AUTOMATICALLY',
    tooltip: 'DISCUSSION_SETTING_MARK_AUTOMATICALLY_TOOLTIP',
  },
  {
    key: 'closeDiscussion',
    label: 'DISCUSSION_SETTING_CLOSE_DISCUSSION',
    tooltip: 'DISCUSSION_SETTING_CLOSE_DISCUSSION_TOOLTIP',
  },
];

/**
 * React port of the `discussionBoardSettings` component (B2, discussion subsystem — the first leaf):
 * the cog dropdown with the "auto mark read" + "close discussion" toggles. Previously an Angular
 * component (uib-dropdown); now native React, bridged back via react2angular so the still-Angular
 * `discussionBoard.html` keeps rendering `<discussion-board-settings>`. DOM preserved for the
 * `DiscussionPage` Selenide page object: `.discussion-user-settings` (click opens), the
 * `.discussion-setting.dropdown-item` rows, and the `.lo-toggle` toggles (`on`/`off`).
 */
export const DiscussionBoardSettings: React.FC<DiscussionBoardSettingsProps> = ({ settings, updateSettings }) => {
  const translate = useTranslation();
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);

  // uib-dropdown closes on an outside click.
  useEffect(() => {
    if (!open) return;
    const onDocMouseDown = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false);
    };
    document.addEventListener('mousedown', onDocMouseDown);
    return () => document.removeEventListener('mousedown', onDocMouseDown);
  }, [open]);

  const toggleSetting = (key: SettingKey, e: React.MouseEvent) => {
    // stopPropagation kept the uib-dropdown open after a toggle.
    e.stopPropagation();
    updateSettings[key]({ [key]: !settings[key] });
  };

  return (
    <div
      className="discussion-user-settings"
      ref={ref}
    >
      <button
        className="btn btn-link p-0 d-flex align-items-center"
        type="button"
        onClick={() => setOpen(o => !o)}
      >
        <span className="sr-only">{translate('DISCUSSION_USER_SETTINGS')}</span>
        <span className="icon icon-cog h3 m-0" />
      </button>

      {open && (
        <div className="discussion-user-settings-list dropdown-menu dropdown-menu-right show">
          {SETTINGS.map(s => (
            // eslint-disable-next-line jsx-a11y/no-static-element-interactions, jsx-a11y/click-events-have-key-events
            <div
              key={s.key}
              className="discussion-setting dropdown-item"
              onClick={e => toggleSetting(s.key, e)}
              title={translate(s.tooltip)}
            >
              <span className="setting-label">{translate(s.label)}</span>
              <span
                className={classnames('setting-toggle', 'lo-toggle', {
                  on: settings[s.key],
                  off: !settings[s.key],
                })}
              >
                <label
                  className="sr-only toggle-label-on"
                  role="checkbox"
                  aria-checked={true}
                >
                  On
                </label>
                <label
                  className="sr-only toggle-label-off"
                  role="checkbox"
                  aria-checked={false}
                >
                  Off
                </label>
                <div className="lo-toggle-knob" />
              </span>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

