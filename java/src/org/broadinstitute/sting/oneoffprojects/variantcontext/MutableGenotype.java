package org.broadinstitute.sting.oneoffprojects.variantcontext;

import org.broadinstitute.sting.utils.Utils;

import java.util.*;

/**
 * This class emcompasses all the basic information about a genotype.  It is immutable.
 *
 * @author Mark DePristo
 */
public class MutableGenotype extends Genotype {
    public MutableGenotype(Genotype parent) {
        super(parent.getSampleName(), parent.getAlleles(), parent.getNegLog10PError(), parent.getFilters(), parent.getAttributes());
    }

    // todo -- add rest of genotype constructors here
    public MutableGenotype(String sampleName, List<Allele> alleles, double negLog10PError) {
        super(sampleName, alleles, negLog10PError);
    }

    public Genotype unmodifiableGenotype() {
        return new Genotype(getSampleName(), getAlleles(), getNegLog10PError(), getFilters(), getAttributes());
    }


    /**
     *
     * @param alleles
     */
    public void setAlleles(List<Allele> alleles) {
        this.alleles.clear();

        // todo -- add validation checking here

        if ( alleles == null ) throw new IllegalArgumentException("BUG: alleles cannot be null in setAlleles");
        if ( alleles.size() == 0) throw new IllegalArgumentException("BUG: alleles cannot be of size 0 in setAlleles");

        int nNoCalls = 0;
        for ( Allele allele : alleles ) { nNoCalls += allele.isNoCall() ? 1 : 0; }
        if ( nNoCalls > 0 && nNoCalls != alleles.size() )
            throw new IllegalArgumentException("BUG: alleles include some No Calls and some Calls, an illegal state " + this);

        for ( Allele allele : alleles ) {
            addAllele(allele);
        }
    }

    public void addAllele(Allele allele) {
        if ( allele == null ) throw new IllegalArgumentException("BUG: Cannot add a null allele to a genotype");
        this.alleles.add(allele);
    }

    // ---------------------------------------------------------------------------------------------------------
    //
    // InferredGeneticContext mutation operators
    //
    // ---------------------------------------------------------------------------------------------------------
    public void setName(String name)                    { commonInfo.setName(name); }
    public void addFilter(String filter)                { commonInfo.addFilter(filter); }
    public void addFilters(Collection<String> filters)  { commonInfo.addFilters(filters); }
    public void clearFilters()                          { commonInfo.clearFilters(); }
    public void setFilters(Collection<String> filters)  { commonInfo.setFilters(filters); }
    public void setAttributes(Map<String, ?> map)       { commonInfo.setAttributes(map); }
    public void putAttribute(String key, Object value)  { commonInfo.putAttribute(key, value); }
    public void removeAttribute(String key)             { commonInfo.removeAttribute(key); }
    public void putAttributes(Map<String, ?> map)       { commonInfo.putAttributes(map); }
    public void setNegLog10PError(double negLog10PError) { commonInfo.setNegLog10PError(negLog10PError); }
    public void putAttribute(String key, Object value, boolean allowOverwrites) { commonInfo.putAttribute(key, value, allowOverwrites); }

}