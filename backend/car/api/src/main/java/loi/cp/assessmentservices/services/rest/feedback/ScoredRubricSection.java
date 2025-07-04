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

package loi.cp.assessmentservices.services.rest.feedback;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class ScoredRubricSection {

    @JsonProperty
    private Double levelGrade;

    @JsonProperty
    private Long levelIndex;

    @JsonProperty
    private Boolean manual;

    @JsonProperty
    private String feedback;

    // For the benefit of Jackson
    private ScoredRubricSection() {
    }

    public ScoredRubricSection(Double levelGrade, Long levelIndex, Boolean manual, String feedback) {
        this.levelGrade = levelGrade;
        this.levelIndex = levelIndex;
        this.manual = manual;
        this.feedback = feedback;
    }

    public Double getLevelGrade() {
        return levelGrade;
    }

    public void setLevelGrade(Double levelGrade) {
        this.levelGrade = levelGrade;
    }

    public Long getLevelIndex() {
        return levelIndex;
    }

    public void setLevelIndex(Long levelIndex) {
        this.levelIndex = levelIndex;
    }

    public Boolean getManual() {
        return manual;
    }

    public void setManual(Boolean manual) {
        this.manual = manual;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("levelGrade", levelGrade)
                .append("levelIndex", levelIndex)
                .append("manual", manual)
                .append("feedback", feedback)
                .toString();
    }
}
