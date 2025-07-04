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

import { debounce } from 'lodash';
import { Component } from 'react';
import PropTypes from 'prop-types';

const dummyDebounced = () => {};
dummyDebounced.cancel = () => {};

class Autosaver extends Component {
  static propsTypes = {
    delayAfterSave: PropTypes.number,
    delayAfterUpdate: PropTypes.number,
    lastSaved: PropTypes.number,
    lastUpdated: PropTypes.number,
    save: PropTypes.func,
  };

  constructor(props) {
    super(props);
    const debouceSave = delay => {
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

  componentDidUpdate(prevProps) {
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
