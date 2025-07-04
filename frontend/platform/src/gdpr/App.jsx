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
import React from 'react';
import { connect } from 'react-redux';
import { Alert, Button, Container, Input, Progress } from 'reactstrap';
import { bindActionCreators } from 'redux';

import * as MainActions from '../redux/actions/MainActions';
import { ContentTypeMultipart, asjax } from '../services';

const State_Init = 0;
const State_Counting = 1;
const State_Counted = 2;
const State_Purging = 3;
const State_Purged = 4;

class App extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      state: State_Init,
      done: 0,
      todo: 0,
      description: '',
      error: false,
      file: null,
      uploading: false,
    };
  }

  countPurge = () => {
    this.setState({ state: State_Counting });
    axios
      .get(`/api/v2/gdpr/inactiveUserCount${this.suffix()}`)
      .then(res => {
        this.setState({ state: State_Counted, todo: res.data });
      })
      .catch(() => this.setState({ error: true }));
  };

  performPurge = () => {
    this.setState({ state: State_Purging });
    asjax(`/api/v2/gdpr/purgeInactiveUsers${this.suffix()}`, {}, (description, done, todo) => {
      this.setState({ description, done, todo });
    })
      .then(() => {
        this.setState({ state: State_Purged });
      })
      .catch(() => this.setState({ error: true }));
  };

  suffix = () => (this.state.file ? `?minors=${this.state.file.guid}` : '');

  onFile = e => {
    const file = e.target.files[0];
    if (!file) return;
    this.setState({ uploading: true });
    const data = new FormData();
    data.append('upload', file);
    axios
      .post('/api/v2/uploads', data, ContentTypeMultipart)
      .then(res => this.setState({ file: res.data }))
      .catch(() => this.setState({ error: true }))
      .finally(() => this.setState({ uploading: false }));
  };

  render() {
    const T = this.props.translations;
    const { state, description, done, todo, error, file, uploading } = this.state;
    return (
      <Container fluid>
        <h1 style={{ color: 'salmon' }}>{T.t('overlord.page.Gdpr.name')}</h1>
        <Alert
          color="warning"
          className="my-3"
        >
          <strong>Warning:</strong> This will permanently and irrevocably purge users who have been
          inactive for five years from the system. If you upload a spreadsheet of user identifiers
          for minors (under 18 years old), they will be purged if inactive just a year.
        </Alert>
        <div className="mb-3">
          {file === null ? (
            <Input
              id="minors"
              type="file"
              label="Minor User Identifiers CSV"
              className="w-auto"
              accept=".csv"
              onChange={this.onFile}
              disabled={state !== State_Init}
            />
          ) : (
            <Alert
              color="info"
              className="my-3 d-flex align-items-center"
            >
              <strong>Minor Users:&nbsp;</strong>
              <span className="flex-grow-1">
                {file.fileName} ({file.size} bytes)
              </span>
              <Button
                className="p-0"
                color="transparent"
                onClick={() => this.setState({ file: null })}
                disabled={state !== State_Init}
              >
                <span className="material-icons md-18">close</span>
              </Button>
            </Alert>
          )}
        </div>
        <Button
          disabled={state !== State_Init || uploading}
          onClick={this.countPurge}
          color="warning"
        >
          Count Users
        </Button>
        {state >= State_Counted && (
          <>
            <Alert
              color="danger"
              className="my-3"
            >
              <strong>Warning:</strong> This will permanently delete {todo} users!
            </Alert>
            <Button
              disabled={state !== State_Counted || !todo}
              onClick={this.performPurge}
              color="danger"
            >
              Purge Users
            </Button>
          </>
        )}
        {state >= State_Purging && (
          <Alert
            color="danger"
            className="my-3"
          >
            <Progress
              value={state === State_Purging ? done : todo}
              max={todo}
              animated={state === State_Purging}
              color="danger"
            >
              {state === State_Purging ? description : 'Complete'}
            </Progress>
          </Alert>
        )}
        {state >= State_Purged && (
          <Alert
            color="dark"
            className="my-3"
          >
            {todo} voices cried out in terror and were suddenly silenced. I fear something terrible
            has happened.
          </Alert>
        )}
        {error && <Alert color="danger my-3">An error occurred!</Alert>}
      </Container>
    );
  }
}

function mapStateToProps(state) {
  return {
    translations: state.main.translations,
  };
}

function mapDispatchToProps(dispatch) {
  return bindActionCreators(MainActions, dispatch);
}

export default connect(mapStateToProps, mapDispatchToProps)(App);
