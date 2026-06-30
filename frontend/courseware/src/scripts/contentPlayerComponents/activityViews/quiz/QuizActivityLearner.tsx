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

import { ScrollToTopContext } from '../../../landmarks/ScrollToTopProvider.js';
import { selectQuizActivityComponent } from '../../../courseActivityModule/selectors/quizActivitySelectors.js';
import { selectRouter } from '../../../utilities/rootSelectors.js';
import { useContext, useEffect, useRef, useState } from 'react';
import { connect } from 'react-redux';

import QuizActivityLoader from './loaders/QuizActivityLoader.js';
import QuizActivityLearnerPlay from './QuizActivityLearnerPlay.js';
import QuizActivityLearnerResults from './QuizActivityLearnerResults.js';

interface QuizActivityViewProps {
  content: any;
  printView?: boolean;
  onLoaded?: () => void;
  isPlaying: boolean;
  setIsPlaying: (playing: boolean) => void;
  isSoftLimitActive: any;
  setIsSoftLimitActive?: (active: any) => void;
  [key: string]: any;
}

const QuizActivityView = ({
  content,
  printView,
  onLoaded,
  isPlaying,
  setIsPlaying,
  isSoftLimitActive,
}: QuizActivityViewProps) => {
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

interface QuizActivityViewInnerProps {
  quiz: any;
  viewingAs: any;
  isRetaking: boolean;
  wasPlaying: boolean;
  setWasPlaying: (playing: boolean) => void;
  content: any;
  [key: string]: any;
}

const QuizActivityViewInner = (props: QuizActivityViewInnerProps) => {
  const { quiz, viewingAs, isRetaking, wasPlaying, setWasPlaying, content } = props;

  const [isPlaying, setIsPlaying] = useState(() => {
    if (viewingAs.isPreviewing) {
      return !quiz.latestSubmittedAttempt;
    }
    if (isRetaking && !wasPlaying) {
      return true;
    }
    return !quiz.latestAttempt || quiz.isLatestAttemptOpen;
  });

  const [isSoftLimitActive, setIsSoftLimitActive] = useState(null);

  // Replicates lifecycle componentDidUpdate watching quiz.isLatestAttemptOpen
  // transitioning from true -> false while playing.
  const prevIsLatestAttemptOpen = useRef(quiz.isLatestAttemptOpen);
  useEffect(() => {
    if (prevIsLatestAttemptOpen.current && !quiz.isLatestAttemptOpen && isPlaying) {
      setIsPlaying(false);
    }
    prevIsLatestAttemptOpen.current = quiz.isLatestAttemptOpen;
  }, [quiz.isLatestAttemptOpen, isPlaying]);

  // Replicates lifecycle componentDidMount + componentDidUpdate (runs every render).
  useEffect(() => {
    if (!wasPlaying && isPlaying) {
      setWasPlaying(true);
    }
  });

  // Replicates lifecycle componentDidUpdate guarded on unchanged content.id.
  // (The original componentDidMount called this.setState on the lifecycle HOC's
  // own internal state, which never propagated as a prop — effectively a no-op.)
  const prevContentId = useRef(content.id);
  useEffect(() => {
    if (
      prevContentId.current === content.id &&
      wasPlaying &&
      !isPlaying &&
      isSoftLimitActive === null
    ) {
      setIsSoftLimitActive(false);
    }
    prevContentId.current = content.id;
  });

  return (
    <QuizActivityView
      {...props}
      isPlaying={isPlaying}
      setIsPlaying={setIsPlaying}
      isSoftLimitActive={isSoftLimitActive}
      setIsSoftLimitActive={setIsSoftLimitActive}
    />
  );
};

const QuizActivityViewOuter = connect(selectQuizActivityComponent)(QuizActivityViewInner);

interface QuizActivityLearnerProps {
  content: any;
  viewingAs: any;
  actualUser: any;
  printView?: boolean;
  onLoaded?: () => void;
  isRetaking: boolean;
  wasPlaying: boolean;
  setWasPlaying: (playing: boolean) => void;
  [key: string]: any;
}

const QuizActivityLearner = ({
  content,
  viewingAs,
  actualUser,
  printView,
  onLoaded,
  isRetaking,
  wasPlaying,
  setWasPlaying,
}: QuizActivityLearnerProps) => {
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

const QuizActivityLearnerWithState = (props: any) => {
  const [isRetaking, setIsRetaking] = useState(() => props.searchParams.playQuiz);
  const [wasPlaying, setWasPlaying] = useState(false);
  return (
    <QuizActivityLearner
      {...props}
      isRetaking={isRetaking}
      setIsRetaking={setIsRetaking}
      wasPlaying={wasPlaying}
      setWasPlaying={setWasPlaying}
    />
  );
};

export default connect(selectRouter)(QuizActivityLearnerWithState);
