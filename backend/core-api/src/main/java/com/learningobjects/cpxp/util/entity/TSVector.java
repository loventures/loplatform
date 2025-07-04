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

package com.learningobjects.cpxp.util.entity;

import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * This class exists to work around the fact that writing to a TSVector column alters
 * the value being written. The string "Foo, bar, foo." gets transformed to
 * "'foo':1,3 'bar':2". In a Hibernate update, if that gets read from and  written back
 * to the database then it is transformed into "'1':2 '3':3 'foo':1 'bar':4". Sadly it
 * does not appear straightforward to make TSVector columns write-only. Therefore this
 * class provides a transformation from a stringified TSVector back into a sentence
 * that would transform back to the same value.
 *
 * Original stratagem was write transformation: COALESCE(to_tsvector(lower(?)), column)
 * and read transformation: NULL. This made the column effectively write-only; it would
 * default to its original value on update if the application did not specify a new value.
 * However, this fails on an insert because the column value cannot be read on insert.
 *
 * Another option would be to reconfigure Hibernate to use "dynamic update" which only
 * updates columns in the database that are explicitly changed. However the risks of such
 * a far-reaching change are beyond human knowing.
 *
 * Still looking into other options to make these columns write-only.
 */
@SuppressWarnings("unused")
public class TSVector {
    @Nullable
    public static String fromTsVector(@Nullable String str) {
        if (StringUtils.isEmpty(str)) {
            return str;
        }

        String[] wordsAndIndices = str.split("\\s+");
        boolean isFromTsVector = Stream.of(wordsAndIndices)
                                       .allMatch(wordAndIndices -> wordAndIndices.split(":").length == 2);
        if (!isFromTsVector) {
            return str;
        }

        TreeMap<Integer, String> indexToWord = new TreeMap<>();
        Stream.of(wordsAndIndices)
              .forEach(wordAndIndices -> {
                  String[] split = wordAndIndices.split(":");
                  String word = removeQuotes(split[0]);
                  String indices = split[1];

                  if (StringUtils.isEmpty(word)) {
                      return;
                  }

                  Stream.of(indices.split(","))
                        .filter(index -> index != null)
                        .forEach(index -> indexToWord.put(Integer.valueOf(index), word));
              });


        return indexToWord.entrySet().stream()
                .map(Map.Entry::getValue)
                .filter(new PostHyphenSkipper())
                .collect(Collectors.joining(" "));
    }

    private static boolean isHyphenated(String str) {
        return str.contains("-") && !Character.isDigit(str.charAt(str.length() - 1));
    }

    // Assumes a lot about the input
    private static String removeQuotes(String word) {
        return word.startsWith("'") ? word.substring(1, word.length()-1) : word;
    }

    // foo-bar is encoded as 'bar':3 'foo':2 'foo-bar':1 so
    // if we encounter foo-bar we have to skip the subsequent foo and bar

    private static class PostHyphenSkipper implements Predicate<String> {
        private final List<String> skip = new ArrayList<>();

        @Override
        public boolean test(String word) {
            if (!skip.isEmpty()) {
                if (word.equals(skip.remove(0))) {
                    return false;
                }
                // this would surprise us but let's keep going
                skip.clear();
            }
            if (isHyphenated(word)) {
                Collections.addAll(skip, word.split("-"));
            }
            return true;
        }
    }
}

