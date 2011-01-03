/*
 * Copyright (c) 2010, The Broad Institute
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package org.broadinstitute.sting.gatk.traversals;

import org.apache.log4j.Logger;
import org.broadinstitute.sting.gatk.datasources.providers.ShardDataProvider;
import org.broadinstitute.sting.gatk.datasources.shards.Shard;
import org.broadinstitute.sting.gatk.walkers.Walker;
import org.broadinstitute.sting.gatk.ReadMetrics;
import org.broadinstitute.sting.gatk.GenomeAnalysisEngine;
import org.broadinstitute.sting.utils.GenomeLoc;
import org.broadinstitute.sting.utils.Utils;
import org.broadinstitute.sting.utils.MathUtils;
import org.broadinstitute.sting.utils.exceptions.UserException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Map;

public abstract class TraversalEngine<M,T,WalkerType extends Walker<M,T>,ProviderType extends ShardDataProvider> {
    // Time in milliseconds since we initialized this engine
    private long startTime = -1;
    private long lastProgressPrintTime = -1;                // When was the last time we printed our progress?

    // How long can we go without printing some progress info?
    private final long MAX_PROGRESS_PRINT_TIME = 30 * 1000;        // in seconds
    private final long N_RECORDS_TO_PRINT = 1000000;

    // for performance log
    private static final boolean PERFORMANCE_LOG_ENABLED = true;
    private PrintStream performanceLog = null;
    private long lastPerformanceLogPrintTime = -1;                  // When was the last time we printed to the performance log?
    private final long PERFORMANCE_LOG_PRINT_FREQUENCY = 1 * 1000;  // in seconds


    /** our log, which we want to capture anything from this class */
    protected static Logger logger = Logger.getLogger(TraversalEngine.class);

    protected GenomeAnalysisEngine engine;

    /**
     * Gets the named traversal type associated with the given traversal.
     * @return A user-friendly name for the given traversal type.
     */
    protected abstract String getTraversalType();

    /**
     * @param curTime (current runtime, in millisecs)
     * @param lastPrintTime the last time we printed, in machine milliseconds
     * @param printFreq maximum permitted difference between last print and current times
     *
     * @return true if the maximum interval (in millisecs) has passed since the last printing
     */
    private boolean maxElapsedIntervalForPrinting(final long curTime, long lastPrintTime, long printFreq) {
        return (curTime - lastPrintTime) > printFreq;
    }

    /**
     * Forward request to printProgress
     *
     * @param shard the given shard currently being processed.
     * @param loc  the location
     */
    public void printProgress(Shard shard,GenomeLoc loc) {
        // A bypass is inserted here for unit testing.
        // TODO: print metrics outside of the traversal engine to more easily handle cumulative stats.
        ReadMetrics cumulativeMetrics = engine.getCumulativeMetrics() != null ? engine.getCumulativeMetrics().clone() : new ReadMetrics();
        cumulativeMetrics.incrementMetrics(shard.getReadMetrics());
        printProgress(loc, cumulativeMetrics, false);
    }

    /**
     * Utility routine that prints out process information (including timing) every N records or
     * every M seconds, for N and M set in global variables.
     *
     * @param loc       Current location
     * @param metrics   Metrics of reads filtered in/out.
     * @param mustPrint If true, will print out info, regardless of nRecords or time interval
     */
    private void printProgress(GenomeLoc loc, ReadMetrics metrics, boolean mustPrint) {
        final long nRecords = metrics.getNumIterations();
        final long curTime = System.currentTimeMillis();
        final double elapsed = (curTime - startTime) / 1000.0;
        final double secsPer1MReads = (elapsed * 1000000.0) / Math.max(nRecords, 1);

        if (mustPrint
                || nRecords == 1
                || nRecords % N_RECORDS_TO_PRINT == 0
                || maxElapsedIntervalForPrinting(curTime, lastProgressPrintTime, MAX_PROGRESS_PRINT_TIME)) {
            lastProgressPrintTime = curTime;

            if ( nRecords == 1 )
                logger.info("[INITIALIZATION COMPLETE; TRAVERSAL STARTING]");
            else {
                if (loc != null)
                    logger.info(String.format("[PROGRESS] Traversed to %s, processing %,d %s in %.2f secs (%.2f secs per 1M %s)", loc, nRecords, getTraversalType(), elapsed, secsPer1MReads, getTraversalType()));
                else
                    logger.info(String.format("[PROGRESS] Traversed %,d %s in %.2f secs (%.2f secs per 1M %s)", nRecords, getTraversalType(), elapsed, secsPer1MReads, getTraversalType()));
            }
        }

        //
        // code to process the performance log
        //
        if ( performanceLog != null && maxElapsedIntervalForPrinting(curTime, lastPerformanceLogPrintTime, PERFORMANCE_LOG_PRINT_FREQUENCY)) {
            lastPerformanceLogPrintTime = curTime;
            if ( nRecords > 1 ) performanceLog.printf("%.2f\t%d\t%.2f%n", elapsed, nRecords, secsPer1MReads);
        }
    }

    /**
     * Called after a traversal to print out information about the traversal process
     */
    public void printOnTraversalDone(ReadMetrics cumulativeMetrics) {
        printProgress(null, cumulativeMetrics, true);

        final long curTime = System.currentTimeMillis();
        final double elapsed = (curTime - startTime) / 1000.0;

        // count up the number of skipped reads by summing over all filters
        long nSkippedReads = 0L;
        for ( Map.Entry<Class, Long> countsByFilter: cumulativeMetrics.getCountsByFilter().entrySet())
            nSkippedReads += countsByFilter.getValue();

        logger.info(String.format("Total runtime %.2f secs, %.2f min, %.2f hours", elapsed, elapsed / 60, elapsed / 3600));
        if ( cumulativeMetrics.getNumReadsSeen() > 0 )
            logger.info(String.format("%d reads were filtered out during traversal out of %d total (%.2f%%)",
                    nSkippedReads,
                    cumulativeMetrics.getNumReadsSeen(),
                    100.0 * MathUtils.ratio(nSkippedReads,cumulativeMetrics.getNumReadsSeen())));
        for ( Map.Entry<Class, Long> filterCounts : cumulativeMetrics.getCountsByFilter().entrySet() ) {
            long count = filterCounts.getValue();
            logger.info(String.format("  -> %d reads (%.2f%% of total) failing %s",
                    count, 100.0 * MathUtils.ratio(count,cumulativeMetrics.getNumReadsSeen()), Utils.getClassName(filterCounts.getKey())));
        }

        if ( performanceLog != null ) performanceLog.close();
    }

    /**
     * Initialize the traversal engine.  After this point traversals can be run over the data
     * @param engine GenomeAnalysisEngine for this traversal
     */
    public void initialize(GenomeAnalysisEngine engine) {
        this.engine = engine;

        if ( PERFORMANCE_LOG_ENABLED && engine != null && engine.getArguments() != null && engine.getArguments().performanceLog != null ) {
            try {
                performanceLog = new PrintStream(new FileOutputStream(engine.getArguments().performanceLog));
                performanceLog.println(Utils.join("\t", Arrays.asList("elapsed.time", "units.processed", "processing.speed")));
            } catch (FileNotFoundException e) {
                throw new UserException.CouldNotCreateOutputFile(engine.getArguments().performanceLog, e);
            }
        }
    }

    /**
     * Should be called to indicate that we're going to process records and the timer should start ticking
     */
    public void startTimers() {
        lastProgressPrintTime = startTime = System.currentTimeMillis();
    }

    /**
     * this method must be implemented by all traversal engines
     *
     * @param walker       the walker to run with
     * @param dataProvider the data provider that generates data given the shard
     * @param sum          the accumulator
     *
     * @return an object of the reduce type
     */
    public abstract T traverse(WalkerType walker,
                               ProviderType dataProvider,
                               T sum);
}
