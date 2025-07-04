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

import { ScrollToTopContext } from '../../../landmarks/ScrollToTopProvider.js';
import { selectQuizActivityComponent } from '../../../courseActivityModule/selectors/quizActivitySelectors.js';
import { selectRouter } from '../../../utilities/rootSelectors.js';
import { useContext, useEffect } from 'react';
import { connect } from 'react-redux';
import { compose, lifecycle, withState } from 'recompose';

import QuizActivityLoader from './loaders/QuizActivityLoader.js';
import QuizActivityLearnerPlay from './QuizActivityLearnerPlay.js';
import QuizActivityLearnerResults from './QuizActivityLearnerResults.js';

const QuizActivityView = ({
  content,
  printView,
  onLoaded,
  isPlaying,
  setIsPlaying,
  isSoftLimitActive,
}) => {
  const scrollToTop = useContext(ScrollToTopContext);
  useEffect(() => onLoaded?.(), [onLoaded]);
  useEffect(() => scrollToTop(), [scrollToTop, isPlaying]);
  return (
    <>
      {isPlaying ? (
        <QuizActivityLearnerPlay
          content={content}
          printView={printView}
        />
      ) : (
        <QuizActivityLearnerResults
          isSoftLimitActive={isSoftLimitActive}
          playAttempt={() => setIsPlaying(true)}
          content={content}
          printView={printView}
        />
      )}
    </>
  );
};

const QuizActivityViewOuter = compose(
  connect(selectQuizActivityComponent),
  withState('isPlaying', 'setIsPlaying', ({ quiz, viewingAs, isRetaking, wasPlaying }) => {
    if (viewingAs.isPreviewing) {
      return !quiz.latestSubmittedAttempt;
    }
    if (isRetaking && !wasPlaying) {
      return true;
    }
    return !quiz.latestAttempt || quiz.isLatestAttemptOpen;
  }),
  lifecycle({
    componentDidUpdate(prevProps) {
      if (
        prevProps.quiz.isLatestAttemptOpen &&
        !this.props.quiz.isLatestAttemptOpen &&
        this.props.isPlaying
      ) {
        this.props.setIsPlaying(false);
      }
    },
  }),
  lifecycle({
    componentDidMount() {
      if (!this.props.wasPlaying && this.props.isPlaying) {
        this.props.setWasPlaying(true);
      }
    },
    componentDidUpdate() {
      if (!this.props.wasPlaying && this.props.isPlaying) {
        this.props.setWasPlaying(true);
      }
    },
  }),
  withState('isSoftLimitActive', 'setIsSoftLimitActive', null),
  lifecycle({
    componentDidMount() {
      if (this.props.wasPlaying && !this.props.isPlaying) {
        this.setState({
          isSoftLimitActive: false,
        });
      }
    },
    componentDidUpdate(prevProps) {
      if (
        prevProps.content.id === this.props.content.id &&
        this.props.wasPlaying &&
        !this.props.isPlaying &&
        this.props.isSoftLimitActive === null
      ) {
        this.props.setIsSoftLimitActive(false);
      }
    },
  })
)(QuizActivityView);

const QuizActivityLearner = ({
  content,
  viewingAs,
  actualUser,
  printView,
  onLoaded,
  isRetaking,
  wasPlaying,
  setWasPlaying,
}) => {
  return (
    <QuizActivityLoader
      content={content}
      viewingAs={viewingAs}
      actualUserId={actualUser.id}
      printView={printView}
    >
      <QuizActivityViewOuter
        content={content}
        isRetaking={isRetaking}
        wasPlaying={wasPlaying}
        setWasPlaying={setWasPlaying}
        printView={printView}
        onLoaded={onLoaded}
      />
    </QuizActivityLoader>
  );
};

export default compose(
  connect(selectRouter),
  withState('isRetaking', 'setIsRetaking', ({ searchParams }) => {
    return searchParams.playQuiz;
  }),
  withState('wasPlaying', 'setWasPlaying', false)
)(QuizActivityLearner);
