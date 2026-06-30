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

import { debounce } from 'lodash';
import { Component } from 'react';
import PropTypes from 'prop-types';

interface AutosaverProps {
  delayAfterSave?: number;
  delayAfterUpdate?: number;
  lastSaved?: number | string | null;
  lastUpdated?: number;
  save: () => void;
}

type DebouncedAutosave = (() => void) & { cancel: () => void };

const dummyDebounced = (() => {}) as DebouncedAutosave;
dummyDebounced.cancel = () => {};

class Autosaver extends Component<AutosaverProps> {
  static propsTypes = {
    delayAfterSave: PropTypes.number,
    delayAfterUpdate: PropTypes.number,
    lastSaved: PropTypes.number,
    lastUpdated: PropTypes.number,
    save: PropTypes.func,
  };

  scheduleAutosaveAfterSaved: DebouncedAutosave;
  scheduleAutosaveAfterChanged: DebouncedAutosave;

  constructor(props: AutosaverProps) {
    super(props);
    const debouceSave = (delay?: number): DebouncedAutosave => {
      if (delay && delay > 0) {
        return debounce(() => this.doAutosave(), delay);
      } else {
        return dummyDebounced;
      }
    };

    this.scheduleAutosaveAfterSaved = debouceSave(props.delayAfterSave);
    this.scheduleAutosaveAfterChanged = debouceSave(props.delayAfterUpdate);
  }

  doAutosave() {
    this.props.save();
    this.scheduleAutosaveAfterSaved();
    this.scheduleAutosaveAfterChanged.cancel();
  }

  componentDidMount() {
    this.scheduleAutosaveAfterSaved();
  }

  componentDidUpdate(prevProps: AutosaverProps) {
    if (this.props.lastSaved !== prevProps.lastSaved) {
      this.scheduleAutosaveAfterSaved();
    }
    if (this.props.lastUpdated !== prevProps.lastUpdated) {
      this.scheduleAutosaveAfterChanged();
    }
  }

  componentWillUnmount() {
    this.scheduleAutosaveAfterSaved.cancel();
    this.scheduleAutosaveAfterChanged.cancel();
  }

  render() {
    return '';
  }
}

export default Autosaver;
