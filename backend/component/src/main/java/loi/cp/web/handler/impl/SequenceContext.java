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

package loi.cp.web.handler.impl;

import com.learningobjects.cpxp.component.web.DePathSegment;
import org.apache.commons.collections4.ListUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Things we want to accumulate as we iterate the handler sequence that handles the
 * HTTP message.
 */
public class SequenceContext {

    /**
     * The transaction name of the entire sequence. Typically `className#methodName`
     * concatenation of each method invoked in the sequence.
     */
    private final String transactionName;

    /**
     * The path segments of the request-URI that have been used to service the HTTP
     * message. Includes synthetic path segments that were added for internal use by the
     * server. That is, there may be path segments in this list whose existence should not
     * be leaked to the HTTP client.
     *
     * @see #spentPathSegments
     */
    private final List<DePathSegment> effectiveSpentPathSegments;

    /**
     * The path segments of the request-URI that have been used to service the HTTP
     * message. The list does not include synthetic path segments that the server added to
     * assist routing.
     *
     * @see #effectiveSpentPathSegments
     */
    private final List<DePathSegment> spentPathSegments;

    /**
     * Whether or not method results using this sequence context are being embedded in
     * another method result
     * <p>This smells funny.</p>
     */
    private final boolean embedded;

    /**
     * A deprecation message to report to the frontend.
     */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private final Optional<String> deprecation;

    public SequenceContext() {
        this("",
                Collections.emptyList(),
                Collections.emptyList(), false, Optional.empty());
    }

    public static SequenceContext newEmbeddedInstance() {
        return new SequenceContext("",
                Collections.emptyList(),
                Collections.emptyList(), true, Optional.empty());
    }

    private SequenceContext(
            final String transactionName,
            final List<DePathSegment> effectiveSpentPathSegments,
            final List<DePathSegment> spentPathSegments,
            final boolean embedded,
            final Optional<String> deprecation
    ) {
        this.transactionName = transactionName;
        this.effectiveSpentPathSegments = effectiveSpentPathSegments;
        this.spentPathSegments = spentPathSegments;
        this.embedded = embedded;
        this.deprecation = deprecation;
    }

    public String getTransactionName() {
        return transactionName;
    }

    /**
     * Gets the path segments of the request-URI that have been used to service the HTTP
     * message. Includes synthetic path segments that were added for internal use by the
     * server. That is, there may be path segments in this list whose existence should not
     * be leaked to the HTTP client.
     *
     * @return the path segments of the request-URI that have been used to service the
     * HTTP message.
     * @see #getSpentPathSegments()
     */
    public List<DePathSegment> getEffectiveSpentPathSegments() {
        return effectiveSpentPathSegments;
    }

    /**
     * Gets the path segments of the request-URI that have been used to service the HTTP
     * message. The list does not include synthetic path segments that the server added to
     * assist routing.
     *
     * @return the path segments of the request-URI that have been used to service the
     * HTTP message
     * @see #getEffectiveSpentPathSegments()
     */
    public List<DePathSegment> getSpentPathSegments() {
        return spentPathSegments;
    }

    public boolean isEmbedded() {
        return embedded;
    }

    public Optional<String> getDeprecation() {
        return deprecation;
    }

    public SequenceContext with(final String transactionName) {

        final String newName = this.transactionName + transactionName;

        return new SequenceContext(
          newName, effectiveSpentPathSegments, spentPathSegments, embedded, deprecation);
    }

    /**
     * Adds the given path segments to the effective and actual path segment lists of
     * this context. Method handlers should only use this if they have not added synthetic
     * path segments to the given list.
     *
     * @param spentPathSegments actual path segments from the request-URI
     * @return a new sequence context with the given path segments
     * @see #withPathSegments(List, List)
     */
    public SequenceContext withPathSegments(final List<DePathSegment> spentPathSegments) {
        return withPathSegments(spentPathSegments, spentPathSegments);
    }

    /**
     * Adds the effective and actual path segments to the respective lists of this
     * context. Actual path segments are path segments that the client sent in the
     * request-URI. Effective path segments include actual path segments and any
     * additional segments that a handler may add to alter routing. Effective path
     * segments should never be leaked out to the client.
     *
     * @param effectiveSpentPathSegments actual path segments from the request-URI and any
     * synthetic ones added for internal server purposes.
     * @param spentPathSegments actual path segments from the request-URI
     * @return a new sequence context with the given path segments
     */
    public SequenceContext withPathSegments(
            final List<DePathSegment> effectiveSpentPathSegments,
            final List<DePathSegment> spentPathSegments) {

        final List<DePathSegment> newEffectiveSpentPathSegments =
                ListUtils.union(this.effectiveSpentPathSegments, effectiveSpentPathSegments);

        final List<DePathSegment> newSpentPathSegments =
                ListUtils.union(this.spentPathSegments, spentPathSegments);

        return new SequenceContext(transactionName, newEffectiveSpentPathSegments,
                newSpentPathSegments, embedded, deprecation);
    }

    public SequenceContext withEmbedded(final boolean embedded) {
        return new SequenceContext(transactionName, effectiveSpentPathSegments,
                spentPathSegments, embedded, deprecation);
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public SequenceContext withDeprecationMsg(final Optional<String> msg) {
        Optional<String> newMsg = msg.isPresent() ? msg : deprecation;
        return new SequenceContext(
          transactionName, effectiveSpentPathSegments, spentPathSegments, embedded, newMsg);
    }
}
