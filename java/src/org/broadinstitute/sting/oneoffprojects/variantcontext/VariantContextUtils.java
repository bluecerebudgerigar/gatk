package org.broadinstitute.sting.oneoffprojects.variantcontext;

import java.util.*;
import org.apache.commons.jexl.*;
import org.broadinstitute.sting.oneoffprojects.variantcontext.varianteval2.VariantEvaluator;
import org.broadinstitute.sting.utils.StingException;
import org.broadinstitute.sting.utils.Utils;


public class VariantContextUtils {
    /** */
    public static class MatchExp {
        public String name;
        public String expStr;
        public Expression exp;

        public MatchExp(String name, String str, Expression exp) {
            this.name = name;
            this.exp = exp;
        }
    }

    public static List<MatchExp> initializeMatchExps(String[] names, String[] exps) {
        if ( names == null || exps == null )
            throw new StingException("BUG: neither names nor exps can be null: names " + names + " exps=" + exps );

        if ( names.length != exps.length )
            throw new StingException("Inconsistent number of provided filter names and expressions: names=" + names + " exps=" + exps);

        Map<String, String> map = new HashMap<String, String>();
        for ( int i = 0; i < names.length; i++ ) { map.put(names[i], exps[i]); }

        return VariantContextUtils.initializeMatchExps(map);
    }

    public static List<MatchExp> initializeMatchExps(Map<String, String> names_and_exps) {
        List<MatchExp> exps = new ArrayList<MatchExp>();

        for ( Map.Entry<String, String> elt : names_and_exps.entrySet() ) {
            String name = elt.getKey();
            String expStr = elt.getValue();

            if ( name == null || expStr == null ) throw new IllegalArgumentException("Cannot create null expressions : " + name +  " " + expStr);
            try {
                Expression filterExpression = ExpressionFactory.createExpression(expStr);
                exps.add(new MatchExp(name, expStr, filterExpression));
            } catch (Exception e) {
                throw new StingException("Invalid expression used (" + expStr + "). Please see the JEXL docs for correct syntax.");
            }
        }

        return exps;
    }

    public static boolean match(VariantContext vc, MatchExp exp) {
        return match(vc,Arrays.asList(exp)).get(exp);
    }

    public static Map<MatchExp, Boolean> match(VariantContext vc, Collection<MatchExp> exps) {
        // todo -- actually, we should implement a JEXL context interface to VariantContext,
        // todo -- which just looks up the values assigned statically here.  Much better approach

        Map<String, String> infoMap = new HashMap<String, String>();

        infoMap.put("CHROM", vc.getLocation().getContig());
        infoMap.put("POS", String.valueOf(vc.getLocation().getStart()));
        infoMap.put("TYPE", vc.getType().toString());
        infoMap.put("QUAL", String.valueOf(10 * vc.getNegLog10PError()));

        // add alleles
        infoMap.put("ALLELES", Utils.join(";", vc.getAlleles()));
        infoMap.put("N_ALLELES", String.valueOf(vc.getNAlleles()));

        // add attributes
        addAttributesToMap(infoMap, vc.getAttributes(), "");

        // add filter fields
        infoMap.put("FILTER", String.valueOf(vc.isFiltered() ? "1" : "0"));
        for ( Object filterCode : vc.getFilters() ) {
            infoMap.put(String.valueOf(filterCode), "1");
        }

        // add genotypes
        // todo -- comment this back in when we figure out how to represent it nicely
//        for ( Genotype g : vc.getGenotypes().values() ) {
//            String prefix = g.getSampleName() + ".";
//            addAttributesToMap(infoMap, g.getAttributes(), prefix);
//            infoMap.put(prefix + "GT", g.getGenotypeString());
//        }

        JexlContext jContext = JexlHelper.createContext();
        //System.out.printf(infoMap.toString());
        jContext.setVars(infoMap);

        try {
            Map<MatchExp, Boolean> resultMap = new HashMap<MatchExp, Boolean>();
            for ( MatchExp e : exps ) {
                resultMap.put(e, (Boolean)e.exp.evaluate(jContext));
            }
            return resultMap;
        } catch (Exception e) {
            throw new StingException(e.getMessage());
        }

    }

    private static void addAttributesToMap(Map<String, String> infoMap, Map<String, ?> attributes, String prefix ) {
        for (Map.Entry<String, ?> e : attributes.entrySet()) {
            infoMap.put(prefix + String.valueOf(e.getKey()), String.valueOf(e.getValue()));
        }
    }



    private static final String UNIQUIFIED_SUFFIX = ".unique";

    /**
     * @param other  another variant context
     *
     * throws an exception if there is a collision such that the same sample exists in both contexts
     * @return a context representing the merge of this context and the other provided context
     */
//    public VariantContext merge(VariantContext left, VariantContext other) {
//        return merge(left, other, false);
//    }

    /**
     * @param other            another variant context
     * @param uniquifySamples  if true and there is a collision such that the same sample exists in both contexts,
     *                           the samples will be uniquified(based on their sources);
     *                           otherwise, an exception will be thrown
     *
     * @return a context representing the merge of this context and the other provided context
     */
//    public VariantContext merge(VariantContext left, VariantContext other, boolean uniquifySamples) {
//        // todo -- make functional
//
//        if ( !left.getLocation().equals(other.getLocation()) )
//            throw new IllegalArgumentException("The locations must be identical for two contexts to be merged");
//
//        Set<String> samples = left.getSampleNames();
//        Set<Genotype> Gs = new HashSet<Genotype>(left.getGenotypes().values());
//
//        for ( Genotype g : other.getGenotypes().values() ) {
//            if ( samples.contains(g.getSampleName()) ) {
//                if ( uniquifySamples )
//                    g.setSampleName(g.getSampleName() + UNIQUIFIED_SUFFIX);
//                else
//                    throw new IllegalStateException("The same sample name exists in both contexts when attempting to merge");
//            }
//            Gs.add(g);
//        }
//
//        HashMap<Object, Object> attrs = new HashMap<Object, Object>(left.getAttributes());
//        attrs.putAll(other.getAttributes());
//
//        return new VariantContext(left, Gs, attrs);
//    }


    /**
     * @param subclass  the name of a subclass of variants to select
     *
     * @return a subset of this context which selects based on the given subclass
     */
//    public VariantContextUtils select(String subclass) {
//        HashSet<Genotype> Gs = new HashSet<Genotype>();
//        for ( Genotype g : genotypes ) {
//            if ( g.getAttribute(subclass) != null )
//                Gs.add(g);
//        }
//        return createNewContext(Gs, attributes);
//    }

    /**
     * @param expr  a jexl expression describing how to filter this context
     *
     * @return a subset of this context which is filtered based on the given expression
     */
//    public VariantContextUtils filter(String expr) {
//        HashSet<Genotype> Gs = new HashSet<Genotype>();
//        try {
//            Expression filterExpression = ExpressionFactory.createExpression(expr);
//
//            for ( Genotype g : genotypes ) {
//                JexlContext jContext = JexlHelper.createContext();
//                jContext.setVars(g.getAttributes());
//                if ( (Boolean)filterExpression.evaluate(jContext) )
//                    Gs.add(g);
//            }
//
//        } catch (Exception e) {
//            throw new StingException("JEXL error in VariantContext: " + e.getMessage());
//        }
//
//        return createNewContext(Gs, attributes);
//    }

    /**
     * @return a set of new variant contexts, one for each sample from this context
     */
//    public Set<VariantContextUtils> splitBySample() {
//        Set<VariantContextUtils> contexts = new HashSet<VariantContextUtils>();
//        for ( Genotype g : genotypes ) {
//            HashSet<Genotype> gAsSet = new HashSet<Genotype>();
//            gAsSet.add(g);
//            contexts.add(createNewContext(gAsSet, attributes));
//        }
//        return contexts;
//    }
}