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
import PropTypes from 'prop-types';
import React from 'react';

import { Hyperlink, ponyImageName, ponyName } from '../services';

// https://stackoverflow.com/questions/67822238/how-to-import-a-json-file-using-vite-dynamicly
const ponies = import.meta.glob('../imgs/ponies/*.svg');

const getChangelogUrl = branch => {
  switch (branch) {
    case 'master':
      return 'https://new-jenkins.dev.campuspack.net/job/bfr/job/master/lastSuccessfulBuild/artifact/de/target/changelog.html';
    case 'release/11.0-staging':
      return 'https://new-jenkins.dev.campuspack.net/job/bfr/job/release%252F11.0-staging/lastSuccessfulBuild/artifact/de/target/changelog.html';
    case 'release/11.0-release':
      return 'https://new-jenkins.dev.campuspack.net/job/bfr/job/release%252F11.0-release/lastSuccessfulBuild/artifact/de/target/changelog.html';
  }
};

class About extends React.Component {
  constructor(props) {
    super(props);
    this.state = { description: null, image: '' };
  }

  componentDidMount() {
    axios.get('/sys/describe').then(res => {
      this.setState({ description: res.data });
      const path = '../imgs/ponies/' + ponyImageName(res.data.platform.version);
      ponies[path]?.().then(res => {
        const imgURL = new URL(path, import.meta.url);
        this.setState({ image: imgURL.href });
      });
    });
  }

  render() {
    const { naked, T } = this.props;
    const { description, image } = this.state;
    if (!description) return null;
    const {
      platform: { version, branch },
    } = description;
    const name = ponyName(version);
    const changelogUrl = getChangelogUrl(branch);
    const containerStyle = {
      position: 'absolute',
      height: naked ? '100vh' : 'calc(100vh - 9rem)',
      paddingTop: naked ? '15px' : '0',
      paddingBottom: naked ? '15px' : '0',
      zIndex: 1,
    };
    return (
      <React.Fragment>
        <style
          dangerouslySetInnerHTML={{
            __html: `
          .container-fluid::after {
            content: "";
            background: center no-repeat url(${image});
            background-size: contain;
            background-origin: content-box;
            top: 0;
            left: 0;
            right: 0;
            bottom: 0;
            position: absolute;
            z-index: -1;
            padding-top: 5rem;
          }
        `,
          }}
        />
        <div
          style={containerStyle}
          className="container-fluid"
        >
          <h2 className="aboutPony text-center">{name}</h2>
          <div className="aboutMeta">
            <div style={{ whiteSpace: 'nowrap' }}>
              {T.t('about.buildDate', description.platform)}
            </div>
            <div>
              <Hyperlink
                label={T.t('about.commit', description.platform)}
                style={{ color: '#777', whiteSpace: 'nowrap' }}
                target="_blank"
                href={description.platform.stashDetails}
              />
            </div>
            {changelogUrl && (
              <div>
                <Hyperlink
                  label={T.t('about.changelog')}
                  style={{ color: '#777', whiteSpace: 'nowrap' }}
                  target="_blank"
                  href={changelogUrl}
                />
              </div>
            )}
          </div>
        </div>
      </React.Fragment>
    );
  }
}

About.propTypes = {
  T: PropTypes.object.isRequired,
  naked: PropTypes.bool.isRequired,
};

export default About;
