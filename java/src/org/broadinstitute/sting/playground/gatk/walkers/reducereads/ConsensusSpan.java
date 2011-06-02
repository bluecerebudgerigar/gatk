package org.broadinstitute.sting.oneoffprojects.walkers.reducereads;

import org.broadinstitute.sting.utils.GenomeLoc;

/**
* Created by IntelliJ IDEA.
* User: depristo
* Date: 4/8/11
* Time: 3:01 PM
* To change this template use File | Settings | File Templates.
*/
final class ConsensusSpan {

    /**
     * The type of an span is either conserved (little variability within the span) or
     * variable (too many differences among the reads in the span to determine the exact
     * haplotype sequence).
     */
    public enum Type {
        CONSERVED, VARIABLE;

        public static Type otherType(Type t) {
            switch ( t ) {
                case CONSERVED: return VARIABLE;
                case VARIABLE: return CONSERVED;
            }
            return CONSERVED;
        }
    }


    final int refStart; // the start position on the reference for relative calculations
    final GenomeLoc loc;
    final Type consensusType;

    public ConsensusSpan(final int refStart, GenomeLoc loc, ConsensusSpan.Type consensusType) {
        this.refStart = refStart;
        this.loc = loc;
        this.consensusType = consensusType;
    }

    public int getOffsetFromStartOfSites() {
        return loc.getStart() - refStart;
    }

    public int getGenomeStart() {
        return loc.getStart();
    }

    public int getGenomeStop() {
        return loc.getStop();
    }

    public ConsensusSpan.Type getConsensusType() {
        return consensusType;
    }

    public int size() {
        return getGenomeStop() - getGenomeStart() + 1;
    }

    public boolean isConserved() { return getConsensusType() == Type.CONSERVED; }
    public boolean isVariable() { return getConsensusType() == Type.VARIABLE; }

    public String toString() {
        return String.format("%s %s", consensusType, loc);
    }
}