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

import { statusFlagToggleActionCreatorMaker } from '../../utilities/statusFlagReducer';
import { useSelector } from 'react-redux';
import { courseReduxStore } from '../../loRedux';

const statusFlagToggleAction = statusFlagToggleActionCreatorMaker({
  sliceName: 'sideNavOpenState',
});

class SideNavStateService {
  openSideNav() {
    courseReduxStore.dispatch(statusFlagToggleAction(true));
  }

  closeSideNav() {
    courseReduxStore.dispatch(statusFlagToggleAction(false));
  }

  get showSideNav() {
    return courseReduxStore.getState().ui.sideNavOpenState.status;
  }

  get hideSideNav() {
    return !courseReduxStore.getState().ui.sideNavOpenState.status;
  }
}

const singleton = new SideNavStateService();
export default singleton;

export const useSidepanelOpen = (): [boolean, (open?: boolean) => void] => {
  const open = useSelector(() => singleton.showSideNav);
  const toggle = (tgt?: boolean) => {
    if (tgt == null ? !open : tgt) {
      singleton.openSideNav();
    } else {
      singleton.closeSideNav();
    }
  };
  return [open, toggle];
};
