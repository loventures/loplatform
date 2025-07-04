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

import { statusFlagToggleActionCreatorMaker } from '../utilities/statusFlagReducer';
import { useSelector } from 'react-redux';
import { courseReduxStore } from '../loRedux';

const feedbackOpenToggleAction = statusFlagToggleActionCreatorMaker({
  sliceName: 'feedbackOpenState',
});

const feedbackAddToggleAction = statusFlagToggleActionCreatorMaker({
  sliceName: 'feedbackAddState',
});

const feedbackEnabledToggleAction = statusFlagToggleActionCreatorMaker({
  sliceName: 'feedbackEnabledState',
});

class FeedbackStateService {
  openFeedback(add: boolean) {
    courseReduxStore.dispatch(feedbackAddToggleAction(add));
    courseReduxStore.dispatch(feedbackOpenToggleAction(true));
  }

  closeFeedback() {
    courseReduxStore.dispatch(feedbackOpenToggleAction(false));
  }

  toggleFeedbackEnabled(enabled?: boolean) {
    courseReduxStore.dispatch(feedbackEnabledToggleAction(enabled));
  }

  get showFeedback() {
    return courseReduxStore.getState().ui.feedbackOpenState.status;
  }

  get addFeedback() {
    return courseReduxStore.getState().ui.feedbackAddState.status;
  }

  get feedbackEnabled() {
    return courseReduxStore.getState().ui.feedbackEnabledState.status;
  }
}

const feedbackSingleton = new FeedbackStateService();

export const useFeedbackOpen = (): [boolean, boolean, (open?: boolean, add?: boolean) => void] => {
  const open = useSelector(() => feedbackSingleton.showFeedback);
  const add = useSelector(() => feedbackSingleton.addFeedback);
  const toggle = (tgt?: boolean, add?: boolean) => {
    if (tgt == null ? !open : tgt) {
      feedbackSingleton.openFeedback(!!add);
    } else {
      feedbackSingleton.closeFeedback();
    }
  };
  return [open, add, toggle];
};

export const useFeedbackEnabled = (): [boolean, (enabled?: boolean) => void] => {
  // This pattern is just horrid
  const enabled = useSelector(
    () =>
      courseReduxStore.getState().preferences.enableInstructorFeedback &&
      feedbackSingleton.feedbackEnabled
  );
  const toggle = () => feedbackSingleton.toggleFeedbackEnabled();
  return [enabled, toggle];
};
