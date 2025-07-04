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
import classNames from 'classnames';
import PropTypes from 'prop-types';
import React from 'react';
import { connect } from 'react-redux';
import { Link } from 'react-router-dom';
import { Row } from 'reactstrap';

import { clearSavedTableState } from '../components/reactTable/ReactTable';
import LoPropTypes from '../react/loPropTypes';
import { isDevelopment, staticUrl } from '../services';
import { hasRight } from '../services/Rights.js';
import { ClusterHealthyUrl, StartupTasksStatusUrl } from '../services/URLs.js';
import allPages from './pages';

class Card extends React.Component {
  playSound = () => {
    const { audio } = this.props;
    if (audio) {
      audio().then(data => new Audio(staticUrl(data.default)).play());
    }
  };

  onMouseOver = () => {
    this.timeout = setTimeout(this.playSound, 7000);
  };

  onMouseOut = () => {
    clearTimeout(this.timeout);
  };

  componentWillUnmount() {
    // the component vanishing does not count as the mouse leaving it seems
    this.onMouseOut();
  }

  render() {
    const { child, className, href, identifier, icon, label, xl, T } = this.props;
    const cls = classNames(
      'portal-card',
      xl ? ['offset-md-3', 'col-md-6'] : ['col-xl-3', 'col-md-4', 'col-sm-6'],
      className
    );
    return (
      <div
        id={`portal-card-${identifier}`}
        className={cls}
        onMouseOver={this.onMouseOver}
        onMouseOut={this.onMouseOut}
      >
        {!href ? (
          <Link
            className="portal-card-link"
            to={`/${identifier}`}
            onClick={() => clearSavedTableState()}
          >
            <i
              className="portal-card-icon material-icons"
              aria-hidden="true"
            >
              {icon}
            </i>
            <div className="portal-card-title">
              {label || T.t(`overlord.page.${identifier}.name`)}
            </div>
          </Link>
        ) : (
          <a
            className="portal-card-link"
            href={href}
          >
            <i
              className="portal-card-icon material-icons"
              aria-hidden="true"
            >
              {icon}
            </i>
            <div className="portal-card-title">
              {label || T.t(`overlord.page.${identifier}.name`)}
            </div>
          </a>
        )}
        {child && (
          <Link
            className="bottom-right-link"
            to={`/${child.identifier}`}
          >
            {child.name}
            <div className="peter"></div>
          </Link>
        )}
      </div>
    );
  }
}

const iconia = [
  '3d_rotation',
  'accessibility',
  'accessible',
  'account_balance',
  'account_balance_wallet',
  'account_box',
  'account_circle',
  'add_shopping_cart',
  'alarm',
  'alarm_add',
  'alarm_off',
  'alarm_on',
  'all_out',
  'android',
  'announcement',
  'aspect_ratio',
  'assessment',
  'assignment',
  'assignment_ind',
  'assignment_late',
  'assignment_return',
  'assignment_returned',
  'assignment_turned_in',
  'autorenew',
  'backup',
  'book',
  'bookmark',
  'bookmark_border',
  'bug_report',
  'build',
  'cached',
  'camera_enhance',
  'card_giftcard',
  'card_membership',
  'card_travel',
  'change_history',
  'check_circle',
  'chrome_reader_mode',
  'class',
  'code',
  'compare_arrows',
  'copyright',
  'credit_card',
  'dashboard',
  'date_range',
  'delete',
  'delete_forever',
  'description',
  'dns',
  'done',
  'done_all',
  'donut_large',
  'donut_small',
  'eject',
  'euro_symbol',
  'event',
  'event_seat',
  'exit_to_app',
  'explore',
  'extension',
  'face',
  'favorite',
  'favorite_border',
  'feedback',
  'find_in_page',
  'find_replace',
  'fingerprint',
  'flight_land',
  'flight_takeoff',
  'flip_to_back',
  'flip_to_front',
  'g_translate',
  'gavel',
  'get_app',
  'gif',
  'grade',
  'group_work',
  'help',
  'help_outline',
  'highlight_off',
  'history',
  'home',
  'hourglass_empty',
  'hourglass_full',
  'http',
  'https',
  'important_devices',
  'info',
  'info_outline',
  'input',
  'invert_colors',
  'label',
  'label_outline',
  'language',
  'launch',
  'lightbulb_outline',
  'line_style',
  'line_weight',
  'list',
  'lock',
  'lock_open',
  'lock_outline',
  'loyalty',
  'markunread_mailbox',
  'motorcycle',
  'note_add',
  'offline_pin',
  'opacity',
  'open_in_browser',
  'open_in_new',
  'open_with',
  'pageview',
  'pan_tool',
  'payment',
  'perm_camera_mic',
  'perm_contact_calendar',
  'perm_data_setting',
  'perm_device_information',
  'perm_identity',
  'perm_media',
  'perm_phone_msg',
  'perm_scan_wifi',
  'pets',
  'picture_in_picture',
  'picture_in_picture_alt',
  'play_for_work',
  'polymer',
  'power_settings_new',
  'pregnant_woman',
  'print',
  'query_builder',
  'question_answer',
  'receipt',
  'record_voice_over',
  'redeem',
  'remove_shopping_cart',
  'reorder',
  'report_problem',
  'restore',
  'restore_page',
  'room',
  'rounded_corner',
  'rowing',
  'schedule',
  'search',
  'settings',
  'settings_applications',
  'settings_backup_restore',
  'settings_bluetooth',
  'settings_brightness',
  'settings_cell',
  'settings_ethernet',
  'settings_input_antenna',
  'settings_input_component',
  'settings_input_composite',
  'settings_input_hdmi',
  'settings_input_svideo',
  'settings_overscan',
  'settings_phone',
  'settings_power',
  'settings_remote',
  'settings_voice',
  'shop',
  'shop_two',
  'shopping_basket',
  'shopping_cart',
  'speaker_notes',
  'speaker_notes_off',
  'spellcheck',
  'stars',
];

const portletPeople = ['merlin', 'mfirtion', 'nvanaartsen', 'pryan', 'sjordan'];

const StartupTasksIdentifier = 'StartupTasks';

const ThreeDecaKiloMilliSeconds = 30000;

class Portal extends React.Component {
  constructor(props) {
    super(props);
    const {
      lo_platform: { user },
    } = props;
    this.state = {
      pages: allPages.filter(p => isDevelopment || hasRight(user, p.right)),
      clusterHealthy: true,
      startupHealthy: true,
    };
  }

  componentDidMount() {
    if (this.state.pages.find(p => p.identifier === StartupTasksIdentifier)) {
      this.pollStartupTaskStatus();
      this.pollClusterStatus();
    }
  }

  pollClusterStatus = () => axios.get(ClusterHealthyUrl).then(this.onClusterHealth);

  onClusterHealth = ({ data: clusterHealthy }) => {
    this.setState({ clusterHealthy });
    if (!clusterHealthy) {
      window.setTimeout(this.pollClusterStatus, ThreeDecaKiloMilliSeconds);
    }
  };

  pollStartupTaskStatus = () => axios.get(StartupTasksStatusUrl).then(this.onStartupTaskStatus);

  onStartupTaskStatus = ({ data: status }) => {
    if (status !== 'Success') {
      this.setState(({ pages }) => ({
        pages: pages.map(page => {
          if (page.identifier === StartupTasksIdentifier) {
            const icon = status === 'Failure' ? 'close' : 'refresh';
            return { ...page, icon, className: 'startupTask' + status };
          } else {
            return page;
          }
        }),
        startupHealthy: status !== 'Failure',
      }));
      if (status === 'InProgress') {
        window.setTimeout(this.pollStartupTaskStatus, ThreeDecaKiloMilliSeconds);
      }
    } else {
      this.setState({ startupHealthy: true });
    }
  };

  onClick = () => {
    this.setState(({ pages }) => {
      const index = parseInt((1 + pages.length) * Math.random(), 10);
      const icon = iconia[parseInt(iconia.length * Math.random(), 10)];
      const label = icon
        .split('_')
        .map(a => a.substring(0, 1).toUpperCase() + a.substring(1))
        .join(' ');
      const portlet = { identifier: 'xxx-' + pages.length, icon: icon, label: label, href: '/' };
      return { pages: [...pages.slice(0, index), portlet, ...pages.slice(index)] };
    });
  };

  render() {
    const {
      T,
      lo_platform: { user },
    } = this.props;
    const { pages, clusterHealthy, startupHealthy } = this.state;
    const xl = pages.length === 1;
    const portlets = !xl && portletPeople.indexOf(user.userName) >= 0;
    return (
      <div
        id="overlord-portal"
        className={classNames('px-4 container-fluid', {
          singular: xl,
          unhealthy: !clusterHealthy || !startupHealthy,
        })}
      >
        <Row>
          {pages.map(({ audio, child, className, href, icon, identifier, label }) => (
            <Card
              audio={audio}
              child={child}
              className={className}
              href={href}
              icon={icon}
              identifier={identifier}
              key={identifier}
              label={label}
              xl={xl}
              T={T}
            />
          ))}
          {portlets && (
            <div
              id="add-portlet"
              className="portal-card col-xl-3 col-md-4 col-sm-6"
              style={{ height: '4em' }}
            >
              <a
                className="portal-card-link"
                style={{ height: '4em' }}
                onClick={this.onClick}
              >
                <div className="portal-card-title">Add Portlet</div>
              </a>
            </div>
          )}
          {!clusterHealthy && (
            <div
              className="portal-card col-xl-3 col-md-4 col-sm-6 startupTaskFailure"
              style={{ height: '4em' }}
            >
              <a
                className="portal-card-link"
                style={{ height: '4em' }}
                href="/control/clusterStatus"
              >
                <div className="portal-card-title">Cluster Status</div>
              </a>
            </div>
          )}
        </Row>
      </div>
    );
  }
}

Portal.propTypes = {
  T: LoPropTypes.translations,
  lo_platform: LoPropTypes.lo_platform,
  history: PropTypes.object.isRequired,
};

function mapStateToProps(state) {
  return {
    T: state.main.translations,
    lo_platform: state.main.lo_platform,
  };
}

export default connect(mapStateToProps, null)(Portal);
