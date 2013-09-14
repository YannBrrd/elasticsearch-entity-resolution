package org.yaba.entity.script;

import no.priv.garshol.duke.Cleaner;
import no.priv.garshol.duke.Comparator;
import no.priv.garshol.duke.Record;
import no.priv.garshol.duke.RecordImpl;
import no.priv.garshol.duke.comparators.Levenshtein;
import no.priv.garshol.duke.utils.Utils;
import no.priv.garshol.duke.utils.ObjectUtils;

import org.elasticsearch.script.AbstractSearchScript;
import org.elasticsearch.ElasticSearchException;
import org.elasticsearch.ElasticSearchIllegalArgumentException;
import org.elasticsearch.script.ExecutableScript;
import org.elasticsearch.script.NativeScriptFactory;
import org.elasticsearch.search.lookup.DocLookup;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.index.fielddata.ScriptDocValues;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@SuppressWarnings("unchecked")
public class EntityResolutionScript extends AbstractSearchScript {

    /*
     * private final static ESLogger logger = Loggers
     * .getLogger(EntityResolutionScript.class);
     */

    private Record comparedRecord = null;
    private HashMap<String, HashMap<String, Object>> entityParams = new HashMap<String, HashMap<String, Object>>();

    /**
     * Native scripts are build using factories that are registered in the
     * {@link org.elasticsearch.examples.nativescript.plugin.NativeScriptExamplesPlugin#onModule(org.elasticsearch.script.ScriptModule)}
     * method when plugin is loaded.
     */
    public static class Factory implements NativeScriptFactory {

        /**
         * This method is called for every search on every shard.
         * 
         * @param params
         *            list of script parameters passed with the query
         * @return new native script
         */
        @Override
        public ExecutableScript newScript(@Nullable Map<String, Object> params) {
            if (params.get("entity") == null) {
                throw new ElasticSearchIllegalArgumentException(
                        "Missing the parameters");
            }

            return new EntityResolutionScript(
                    (Map<String, Object>) params.get("entity"));
        }

    }

    private EntityResolutionScript(Map<String, Object> params) {
        HashMap<String, Collection<String>> props = new HashMap<String, Collection<String>>();
        ArrayList<Map<String, Object>> fieldsParams = null;

        if (params.get("fields") == null)
            throw new ElasticSearchIllegalArgumentException(
                    "Missing the 'fields' parameters");
        else
            fieldsParams = (ArrayList<Map<String, Object>>) params
                    .get("fields");

        Iterator<Map<String, Object>> it = fieldsParams.iterator();
        while (it.hasNext()) {
            Map<String, Object> value = it.next();
            HashMap<String, Object> map = new HashMap<String, Object>();

            String field = (String) value.get("field");
            String fieldValue = (String) value.get("value");

            Iterator<String> cleanIt = ((ArrayList<String>) value
                    .get("cleaners")).iterator();
            ArrayList<Cleaner> cleanList = new ArrayList<Cleaner>();

            while (cleanIt.hasNext()) {
                String cleanerName = (String) cleanIt.next();

                Cleaner cleaner = (Cleaner) ObjectUtils.instantiate(cleanerName);
                cleanList.add(cleaner);
                fieldValue = cleaner.clean(fieldValue);
            }

            map.put("cleaners", cleanList);

            props.put(field, Collections.singleton(fieldValue));

            Double maxValue = (value.get("high") == null ? 0.0 : Double
                    .valueOf(((Double) value.get("high"))));
            Double minValue = (value.get("low") == null ? 0.0 : Double
                    .valueOf(((Double) value.get("low"))));

            String comparatorName = (value.get("comparator") == null ? Levenshtein.class
                    .getName() : (String) value.get("comparator"));

            Comparator comp = (Comparator) ObjectUtils.instantiate(comparatorName);
            map.put("high", maxValue);
            map.put("low", minValue);
            map.put("comparator", comp);

            entityParams.put(fieldValue, map);
        }

        comparedRecord = new RecordImpl(props);
    }

    @Override
    public float runAsFloat() {
        HashMap<String, Collection<String>> props2 = new HashMap<String, Collection<String>>();

        DocLookup doc = doc();
        Collection<String> docKeys = comparedRecord.getProperties();
        Iterator<String> it = docKeys.iterator();

        while (it.hasNext()) {
            String key = (String) it.next();

            if (doc.get(key) != null) {
                String value = getFieldValue(doc.get(key));
                props2.put(key, value == null ? Collections.singleton("")
                        : Collections.singleton(value));
            }
        }

        Record r2 = new RecordImpl(props2);

        return new Double(compare(comparedRecord, r2, entityParams))
                .floatValue();
    }

    /**
     * Reads field value & returns it as String
     * 
     * @param object
     * @return String
     */
    private String getFieldValue(Object field) {

        String result = "";

        if (field instanceof ScriptDocValues.Strings)
            result = ((ScriptDocValues.Strings) field).getValue();
        if (field instanceof ScriptDocValues.Doubles)
            result = ((ScriptDocValues.Doubles) field).getValue() + "";
        if (field instanceof ScriptDocValues.Longs)
            result = ((ScriptDocValues.Longs) field).getValue() + "";
        if (field instanceof ScriptDocValues.GeoPoints)
            throw new ElasticSearchException(
                    "No comparator implemented for GeoPoints");

        return result;
    }

    /**
     * Compares two records and returns the probability that they represent the
     * same real-world entity.
     */
    private double compare(Record r1, Record r2,
            HashMap<String, HashMap<String, Object>> params) {
        double prob = 0.5;

        for (String propname : r1.getProperties()) {

            Collection<String> vs1 = r1.getValues(propname);
            Collection<String> vs2 = r2.getValues(propname);

            if (vs1 == null || vs1.isEmpty() || vs2 == null || vs2.isEmpty())
                continue; // no values to compare, so skip

            double high = 0.0;
            for (String v1 : vs1) {
                if (v1.equals(""))
                    continue;

                for (String v2 : vs2) {
                    if (v2.equals(""))
                        continue;

                    Comparator comp = (Comparator) params.get(v1).get(
                            "comparator");

                    ArrayList<Cleaner> cleanersList = (ArrayList<Cleaner>) params
                            .get(v1).get("cleaners");

                    Iterator<Cleaner> clIt = cleanersList.iterator();

                    while (clIt.hasNext()) {
                        v2 = clIt.next().clean(v2);
                    }

                    double p = compare(v1, v2,
                            (Double) params.get(v1).get("high"),
                            (Double) params.get(v1).get("low"), comp);
                    high = Math.max(high, p);
                }
            }
            prob = Utils.computeBayes(prob, high);
        }
        return prob;
    }

    /**
     * Returns the probability that the records v1 and v2 came from represent
     * the same entity, based on high and low probability settings etc.
     */
    public double compare(String v1, String v2, double high, double low,
            Comparator comparator) {

        if (comparator == null)
            return 0.5; // we ignore properties with no comparator

        double sim = comparator.compare(v1, v2);
        if (sim >= 0.5)
            return ((high - 0.5) * (sim * sim)) + 0.5;
        else
            return low;
    }

    @Override
    public Object run() {
        // TODO Auto-generated method stub
        return null;
    }
}
