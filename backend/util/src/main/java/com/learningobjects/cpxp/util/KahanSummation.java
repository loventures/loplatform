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

package com.learningobjects.cpxp.util;

/**
 * Taken from Ron Mak's Java Number Cruncher.
 */
public class KahanSummation {
    private double _correction;
    private double _sum;

    public KahanSummation(double sum, double correction) {
        _correction = correction;
        _sum = sum;
    }

    public KahanSummation() {
        reset();
    }

    public void add(double addend) {
        // apply running correction
        double correctedAddend = addend + _correction;
        // update the sum
        double tempSum = _sum + correctedAddend;
        // compute the correction and set the running sum
        // parenthesis isolate the high order bits correctly
        _correction = correctedAddend - (tempSum - _sum);
        _sum = tempSum;
    }

    public double value() {
        return _sum;
    }

    public double correction() {
        return _correction;
    }

    public void reset() {
        _correction = 0.0d;
        _sum = 0.0d;
    }

    @Override
    public String toString() {
        return String.valueOf(_sum);
    }
}
