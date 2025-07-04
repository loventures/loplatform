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
import classnames from 'classnames';
import PropTypes from 'prop-types';
import React from 'react';
import { connect } from 'react-redux';
import { Alert, Button } from 'reactstrap';
import { bindActionCreators } from 'redux';

import LoPropTypes from '../react/loPropTypes';
import * as AnnouncementActions from '../redux/actions/AnnouncementActions';
import { IoAlertCircleOutline } from 'react-icons/io5';

class AnnouncementBar extends React.Component {
  dangerouslyCreateMarkup = html => ({ __html: html });

  isActive = ann => new Date(ann.endTime) > new Date();

  disableAnnouncement = annId => {
    this.props.disableAnnouncement(annId);
    axios
      .post('/api/v2/announcements/hide', { announcementId: annId })
      .then(res => res)
      .catch(err => console.log(err));
  };

  render() {
    const {
      announcements,
      lo_platform: { isProdLike }, // cop out, buddy
    } = this.props;

    const alertCls = style =>
      classnames('announcement-alert-inner-html', {
        'html1-blink-slow': !isProdLike && style === 'warning',
        'html1-blink-fast': !isProdLike && style === 'danger',
      });

    const AlertContentComponent = props =>
      isProdLike ? <span {...props} /> : <marquee {...props} />;

    return announcements.length ? (
      <div id="announcements">
        {announcements.map(ann => (
          <Alert
            key={ann.id}
            color={ann.style}
            isOpen={this.isActive(ann)}
            className="d-flex p-2 pe-2 align-items-start"
          >
            <div className={`flex-grow-1 ${isProdLike ? '' : 'd-flex align-items-center'}`}>
              <IoAlertCircleOutline className="me-2" style={{verticalAlign: "-2px"}} />
              <AlertContentComponent
                className={alertCls(ann.style)}
                dangerouslySetInnerHTML={this.dangerouslyCreateMarkup(ann.message)}
              />
            </div>
            <Button
              color="close"
              className="ms-2"
              aria-label="Close"
              onClick={() => this.disableAnnouncement(ann.id)}
            />
          </Alert>
        ))}
      </div>
    ) : null;
  }
}

AnnouncementBar.propTypes = {
  announcements: PropTypes.array,
  lo_platform: LoPropTypes.lo_platform.isRequired,
};

const mapStateToProps = state => ({
  announcements: state.announcement.announcements,
  lo_platform: state.main.lo_platform,
});

const mapDispatchToProps = dispatch => bindActionCreators(AnnouncementActions, dispatch);

export default connect(mapStateToProps, mapDispatchToProps)(AnnouncementBar);
