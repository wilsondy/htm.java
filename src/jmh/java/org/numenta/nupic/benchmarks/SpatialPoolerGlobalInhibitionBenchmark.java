/* ---------------------------------------------------------------------
 * Numenta Platform for Intelligent Computing (NuPIC)
 * Copyright (C) 2014, Numenta, Inc.  Unless you have an agreement
 * with Numenta, Inc., for a separate license for this software code, the
 * following terms and conditions apply:
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as
 * published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses.
 *
 * http://numenta.org/licenses/
 * ---------------------------------------------------------------------
 */

package org.numenta.nupic.benchmarks;

import java.util.concurrent.TimeUnit;

import org.numenta.nupic.Parameters;
import org.numenta.nupic.Parameters.KEY;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.infra.Blackhole;

public class SpatialPoolerGlobalInhibitionBenchmark extends AbstractAlgorithmBenchmark {

    private int[][] input;

    @Setup
    public void init() {
        super.init();

        input = new int[7][8];
        for(int i = 0;i < 7;i++) {
            input[i] = encoder.encode((double) i + 1);
        }
    }

    @Override
    protected Parameters getParameters() {
        Parameters parameters = super.getParameters();
        parameters.setParameterByKey(KEY.GLOBAL_INHIBITIONS, true);
        return parameters;
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public int[] measureAvgCompute_7_Times(Blackhole bh) throws InterruptedException {
        for(int i = 0;i < 7;i++) {
            pooler.compute(memory, input[i], SDR, true, false);
        }

        return SDR;
    }

}
