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

import classnames from 'classnames';
import PropTypes from 'prop-types';
import React from 'react';
import {
  Button,
  ButtonGroup,
  Col,
  FormGroup,
  Input,
  InputGroup,
  InputGroupText,
  Label,
  Row,
} from 'reactstrap';

class LtiLaunchInfo extends React.Component {
  selectAll = e => e.target.select();

  renderCourseLevelIntegration = () => {
    const { baseName, urls, T } = this.props;
    return (
      <React.Fragment>
        <h3 className="row block-header">{T.t(`${baseName}.courseLevelIntegration`)}</h3>
        {['launchUrl', 'xmlUrl'].map(field => {
          const id = `ltiLaunchInfo-${field}`;
          return (
            <FormGroup
              row
              key={field}
            >
              <Label
                lg={3}
                for={id}
              >
                {T.t(`${baseName}.label.${field}`)}
              </Label>
              <Col lg={9}>
                <Input
                  id={id}
                  readOnly
                  value={urls[field]}
                  onClick={this.selectAll}
                />
              </Col>
            </FormGroup>
          );
        })}
        <FormGroup row>
          <Label
            lg={3}
            for="ltiLaunchInfo-thinCommonCartridge"
          >
            {T.t(`${baseName}.label.thinCommonCartridge`)}
          </Label>
          <Col lg={9}>
            {urls.ccUrl2 ? (
              <ButtonGroup style={{ width: '100%' }}>
                <Button
                  id="ltiLaunchInfo-thinCommonCartridge"
                  download
                  href={urls.ccUrl}
                  style={{ flex: '1', borderRight: '1px solid white' }}
                >
                  {T.t(`${baseName}.button.basicOutcomes`)}
                  <i className="material-icons md-18 ms-1">file_download</i>
                </Button>
                <Button
                  id="ltiLaunchInfo-thinCommonCartridge2"
                  download
                  href={urls.ccUrl2}
                  style={{ flex: '1', borderLeft: '1px solid white' }}
                >
                  {T.t(`${baseName}.button.gradeServices`)}
                  <i className="material-icons md-18 ms-1">file_download</i>
                </Button>
              </ButtonGroup>
            ) : (
              <Button
                id="ltiLaunchInfo-thinCommonCartridge"
                download
                href={urls.ccUrl}
                block
              >
                {T.t(`${baseName}.button.download`)}
                <i className="material-icons md-18 ms-1">file_download</i>
              </Button>
            )}
          </Col>
        </FormGroup>
      </React.Fragment>
    );
  };

  renderActivityHeader = () => {
    const { baseName, dlUrl, T } = this.props;
    return (
      <h3 className="row block-header">
        {T.t(`${baseName}.activityLevelIntegration`)}
        <a
          id="ltiLaunchInfo-activityDownload"
          download
          href={dlUrl}
          style={{ display: 'inline-block', color: '#333' }}
          className="ms-2"
        >
          <i
            className="material-icons md-18"
            style={{ verticalAlign: 'middle' }}
          >
            file_download
          </i>
        </a>
      </h3>
    );
  };

  contentItemToRow = ci => {
    const { baseName, T } = this.props;
    const id = `ltiLaunchInfo-${ci.id}`;
    const cn = `ltiLaunch-item-depth-${ci.depth}`;
    return (
      <Row key={ci.id}>
        <Label
          lg={4}
          for={`id`}
          className={classnames({ 'col-form-label-sm': true, [cn]: true })}
        >
          {ci.name}
        </Label>
        <Col
          lg={8}
          className="input-group-sm"
        >
          {ci.graded ? (
            <InputGroup className="input-group-sm">
              <Input
                id={id}
                readOnly
                value={ci.url}
                onClick={this.selectAll}
              />
              <InputGroupText title={T.t(`${baseName}.label.gradebookSupported`)}>
                <span className="material-icons md-18">show_chart</span>
              </InputGroupText>
            </InputGroup>
          ) : (
            <Input
              id={id}
              readOnly
              value={ci.url}
              onClick={this.selectAll}
            />
          )}
        </Col>
      </Row>
    );
  };

  renderActivityLevelIntegration = () => {
    const { contentItems } = this.props;
    if (!contentItems.length) return null;
    return (
      <React.Fragment>
        {this.renderActivityHeader()}
        <Row>
          <div
            id="ltiLaunchInfo-activityLinks"
            className="ltiLaunch-list container-fluid"
          >
            {contentItems.map(this.contentItemToRow)}
          </div>
        </Row>
      </React.Fragment>
    );
  };

  render() {
    return (
      <React.Fragment>
        {this.renderCourseLevelIntegration()}
        {this.renderActivityLevelIntegration()}
      </React.Fragment>
    );
  }
}

LtiLaunchInfo.propTypes = {
  baseName: PropTypes.string.isRequired,
  urls: PropTypes.object.isRequired,
  T: PropTypes.object.isRequired,
  dlUrl: PropTypes.string.isRequired,
  contentItems: PropTypes.array.isRequired,
};

export default LtiLaunchInfo;
