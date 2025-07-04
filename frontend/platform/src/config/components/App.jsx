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

import PropTypes from 'prop-types';
import React from 'react';
import Form from 'react-jsonschema-form/lib/components/Form';
import { connect } from 'react-redux';
import { Col, Row } from 'reactstrap';
import { bindActionCreators } from 'redux';

import * as MainActions from '../../redux/actions/MainActions';
import { getPlatform } from '../../services';
import * as configApi from '../configApi';
import fields from './fields/index';
import LabellingTemplate from './misc/LabellingTemplate';
import SchemaDropdown from './SchemaDropdown';
import widgets from './widgets/index';

class App extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      currentSchema: '',
      currentDefaults: {},
      currentOverrides: {},
      saved: false,
    };
  }

  componentDidMount() {
    const { schema, item } = this.props;
    configApi
      .getConfig(schema, item)
      .then(res => {
        this.setState({
          currentSchema: schema,
          currentDefaults: res.data.defaults,
          currentOverrides: res.data.overrides,
          saved: false,
        });
      })
      .catch(err => {
        window.alert(`error getting configuration for ${schema}: ${err.text}`); // shame
      });
  }

  render() {
    const { currentDefaults, currentOverrides, currentSchema, saved } = this.state;
    const { path, schemata, search } = this.props;

    const ctx = {
      defaults: currentDefaults,
    };

    const form = currentSchema && (
      <React.Fragment>
        <Form
          id={`schema-form-${currentSchema}`}
          schema={schemata[currentSchema]}
          className="form form-inline schema-form"
          formData={currentOverrides}
          formContext={ctx}
          FieldTemplate={LabellingTemplate}
          widgets={widgets}
          fields={fields}
          noValidate={true}
          onChange={this.refreshFormData.bind(this)}
        >
          {saved ? (
            <button
              id="config-saved-alert"
              className="btn btn-success"
              disabled
            >
              Saved!
            </button>
          ) : (
            <button
              id="config-save-btn"
              className="btn btn-success"
              type="submit"
              onClick={this.submitData.bind(this)}
            >
              Save
            </button>
          )}
        </Form>
      </React.Fragment>
    );

    const singular = Object.keys(schemata).length === 1;

    return (
      <Row>
        {!singular && (
          <Col sm={3}>
            <SchemaDropdown
              path={path}
              search={search}
              schemata={schemata}
              current={currentSchema}
            />
          </Col>
        )}
        <Col sm={singular ? 12 : 9}>
          {form}
          <div className="preview">
            {currentSchema && (
              <pre
                id="config-pre"
                className="json-value"
              >
                {JSON.stringify(currentOverrides, null, 2)}
              </pre>
            )}
          </div>
        </Col>
      </Row>
    );
  }

  refreshFormData(ev) {
    this.setState({
      currentOverrides: ev.formData,
      saved: false,
    });
  }

  submitData() {
    const { setLoPlatform, item } = this.props;
    const { currentSchema, currentOverrides } = this.state;

    const jefreshPlatform = () => getPlatform(true).then(({ data }) => setLoPlatform(data));

    /* if we send up a raw string it doesn't get stringified and therefore looks like bad json
     * if we send up nothing it is weird --m */
    const adapted =
      typeof currentOverrides === 'undefined' ? 'null' : JSON.stringify(currentOverrides);
    configApi
      .putConfig(currentSchema, item, adapted)
      .then(() => {
        this.setState({ saved: true });
        jefreshPlatform();
      })
      .catch(err => {
        window.alert(
          `error setting configuration on ${currentSchema}:\n${
            err.response?.data?.message ?? err.message
          }`
        ); // shame
      });
  }
}

App.propTypes = {
  item: PropTypes.number,
  loadData: PropTypes.func,
  path: PropTypes.string.isRequired,
  saveData: PropTypes.func,
  schema: PropTypes.string.isRequired,
  schemata: PropTypes.objectOf(PropTypes.object).isRequired,
  search: PropTypes.string,
  setLoPlatform: PropTypes.func.isRequired,
};

function mapDispatchToProps(dispatch) {
  return bindActionCreators(MainActions, dispatch);
}

export default connect(null, mapDispatchToProps)(App);
