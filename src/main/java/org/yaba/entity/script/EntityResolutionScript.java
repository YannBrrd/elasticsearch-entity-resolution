package org.yaba.entity.script;

import no.priv.garshol.duke.Cleaner;
import no.priv.garshol.duke.Comparator;
import no.priv.garshol.duke.Record;
import no.priv.garshol.duke.RecordImpl;
import no.priv.garshol.duke.comparators.Levenshtein;
import no.priv.garshol.duke.utils.ObjectUtils;
import no.priv.garshol.duke.utils.Utils;
import org.elasticsearch.ElasticsearchIllegalArgumentException;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.cache.Cache;
import org.elasticsearch.common.cache.CacheBuilder;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.fielddata.ScriptDocValues;
import org.elasticsearch.node.Node;
import org.elasticsearch.script.AbstractDoubleSearchScript;
import org.elasticsearch.script.ExecutableScript;
import org.elasticsearch.script.NativeScriptFactory;
import org.elasticsearch.search.lookup.DocLookup;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 *
 */
@SuppressWarnings("unchecked")
/**
 * Entity Resolution Script for Elasticsearch
 * @author Yann Barraud
 *
 */
public final class EntityResolutionScript extends AbstractDoubleSearchScript {
    private static final String FIELDS = "fields";
    private static final String COMPARATOR = "comparator";
    private static final String CLEANERS = "cleaners";
    private static final String PARAMS = "params";
    private static final String NAME = "name";
    private static final String HIGH = "high";
    private static final String LOW = "low";
    /**
     * . Average score
     */
    private static final double AVERAGE_SCORE = 0.5;

    /**
     * . Cache to store configuration
     */
    private final Cache<String, HashMap<String, HashMap<String, Object>>> cache;
    /**
     * . Elasticsearch client
     */
    private final Client client;
    /**
     * . The record to be compared to
     */
    private Record comparedRecord;
    /**
     * . Script parameters
     */
    private HashMap<String, HashMap<String, Object>> entityParams;

    /**
     * . Script class
     *
     * @param params  params from JSON payload
     * @param aCache  a cache to store config
     * @param aClient Elasticsearch client to read config from cluster
     */
    private EntityResolutionScript(
            final Map<String, Object> params,
            final Cache<String, HashMap<String, HashMap<String, Object>>> aCache,
            final Client aClient) {

        if (params.get(FIELDS) == null) {
            throw new ElasticsearchIllegalArgumentException(
                    "Missing the 'fields' parameters");
        }

        this.cache = aCache;
        this.client = aClient;

        comparedRecord = null;
        if (params.get("configuration") == null) {
            comparedRecord =
                    configureWithFieldsOnly((ArrayList<Map<String, Object>>) params
                            .get(FIELDS));
        } else {
            comparedRecord =
                    configureWithFieldsAndConfiguration(
                            (Map<String, Object>) params.get("configuration"),
                            (ArrayList<Map<String, Object>>) params
                                    .get("fields"));
        }

    }

    /**
     * . Reads & instantiates cleaners
     *
     * @param cleanersList array of cleaners from JSON
     * @return the list of instantiated cleaners
     */
    private static List<Cleaner> getCleaners(
            final List<Map<String, String>> cleanersList) {
        List<Cleaner> cleanList = new ArrayList<Cleaner>();

        for (Map aCleaner : cleanersList) {
            Cleaner cleaner = (Cleaner) ObjectUtils.instantiate((String) aCleaner.get(NAME));
            setParams(cleaner, aCleaner.get(PARAMS));
            cleanList.add(cleaner);
        }
        return cleanList;
    }


    /**
     * Sets params for cleaners or comparators
     *
     * @param anObject the object to parametrize
     * @param params params list
     */
    private static void setParams(Object anObject, Object params) {
        if (params != null) {
            Map<String, String> paramsMap = (Map<String, String>) params;
            for (String key : paramsMap.keySet()) {
                ObjectUtils.setBeanProperty(anObject, key, paramsMap.get(key), null);
            }
        }
    }

    /**
     * Gets comparator
     *
     * @param value from JSON payload
     * @return instantiated Comparator
     */
    private static Comparator getComparator(final Map<String, Object> value) {
        Map<String, Object> compEntity = (Map<String, Object>) value.get(COMPARATOR);
        String comparatorName =
                ((compEntity.get(NAME) == null) ? Levenshtein.class.getName() : (String) compEntity.get(NAME));


        Comparator comp = (Comparator) ObjectUtils.instantiate(comparatorName);

        setParams(comp, compEntity.get(PARAMS));

        return comp;
    }

    /**
     * . Reads field value & returns it as String
     *
     * @param field the object to cast
     * @return String object String value
     */
    private static String getFieldValue(final Object field) {
        String result = "";

        if (field instanceof ScriptDocValues.Strings) {
            result = ((ScriptDocValues.Strings) field).getValue();
        }
        if (field instanceof ScriptDocValues.Doubles) {
            result =
                    Double.toString(((ScriptDocValues.Doubles) field)
                            .getValue());
        }
        if (field instanceof ScriptDocValues.Longs) {
            result = Long.toString(((ScriptDocValues.Longs) field).getValue());
        }
        if (field instanceof ScriptDocValues.GeoPoints) {
            ScriptDocValues.GeoPoints point = (ScriptDocValues.GeoPoints) field;
            result = String.format("%s,%s", point.getLat(), point.getLon());
        }

        return result;
    }

    /**
     * Compares two records and returns the probability that they represent the
     * same real-world entity.
     *
     * @param r1     1st Record
     * @param r2     2nd Record
     * @param params Parameters for comparison
     * @return Bayesian probability
     */
    private static double compare(
            final Record r1,
            final Record r2,
            final HashMap<String, HashMap<String, Object>> params) {
        double prob = AVERAGE_SCORE;

        for (String propname : r1.getProperties()) {
            Collection<String> vs1 = r1.getValues(propname);
            Collection<String> vs2 = r2.getValues(propname);

            Comparator comp =
                    (Comparator) params.get(propname).get(COMPARATOR);
            ArrayList<Cleaner> cleanersList =
                    (ArrayList<Cleaner>) params.get(propname).get(CLEANERS);

            Double max = (Double) params.get(propname).get(HIGH);
            Double min = (Double) params.get(propname).get(LOW);

            if (vs1 == null || vs1.isEmpty() || vs2 == null || vs2.isEmpty()) {
                continue; // no values to compare, so skip
            }

            double high = 0.0;
            for (String v1 : vs1) {
                if (v1.equals("")) {
                    continue;
                }

                v2fieldloop:
                for (String v2 : vs2) {
                    if (v2.equals("")) {
                        continue;
                    }

                    for (Cleaner cl : cleanersList) {
                        v2 = cl.clean(v2);
                        if ((v2 == null) || v2.equals("")) {
                            continue v2fieldloop;
                        }
                    }
                    double p = compare(v1, v2, max, min, comp);
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
     *
     * @param v1         1st String
     * @param v2         2nd String
     * @param high       max probability
     * @param low        min probability
     * @param comparator the comparator to use
     * @return the computed probability
     */
    private static double compare(
            final String v1,
            final String v2,
            final double high,
            final double low,
            final Comparator comparator) {

        if (comparator == null) {
            return AVERAGE_SCORE; // we ignore properties with no comparator
        }

        double sim = comparator.compare(v1, v2);
        if (sim < AVERAGE_SCORE) {
            return low;
        } else {
            return ((high - AVERAGE_SCORE) * (sim * sim)) + AVERAGE_SCORE;
        }
    }

    /**
     * . Configures with data within ES index
     *
     * @param configuration configuration from JSON request
     * @param fields        fields from JSON request
     * @return the record to compare others with
     */
    private Record configureWithFieldsAndConfiguration(
            final Map<String, Object> configuration,
            final List<Map<String, Object>> fields) {

        /*
      . Index name where to get configuration
     */
        String configIndex = (String) configuration.get("index");
        /*
      . Type name where to get configuration
     */
        String configType = (String) configuration.get("type");
        /*
      . Type ID where to get configuration
     */
        String configName = (String) configuration.get("name");

        entityParams =
                cache.getIfPresent(configIndex + "." + configType + "."
                        + configName);

        if (entityParams == null) {
            GetResponse response =
                    client.prepareGet(configIndex, configType, configName)
                            .setPreference("_local").execute().actionGet();

            if (response.isExists()) {
                Map<String, Object> entityConf =
                        (Map<String, Object>) response.getSource()
                                .get("entity");
                entityParams = new HashMap<String, HashMap<String, Object>>();
                if (entityConf == null) {
                    throw new ElasticsearchIllegalArgumentException(
                            "No conf found in " + configIndex + "/"
                                    + configType + "/" + configName);
                }

                ArrayList<Map<String, Object>> confFields =
                        (ArrayList<Map<String, Object>>) entityConf
                                .get("fields");

                if (confFields == null) {
                    throw new ElasticsearchIllegalArgumentException(
                            "Bad conf found in " + configIndex + "/"
                                    + configType + "/" + configName);
                }

                for (Map<String, Object> confField : confFields) {
                    HashMap<String, Object> map = new HashMap<String, Object>();

                    String field = (String) confField.get("field");
                    List<Cleaner> cleanList =
                            getCleaners((ArrayList<Map<String, String>>) confField
                                    .get(CLEANERS));

                    map.put(CLEANERS, cleanList);

                    Double maxValue = 0.0;
                    if (confField.get(HIGH) != null) {
                        maxValue = ((Double) confField.get(HIGH));
                    }

                    Double minValue = 0.0;
                    if (confField.get(LOW) != null) {
                        minValue = (Double) confField.get(LOW);
                    }

                    Comparator comp = getComparator(confField);
                    map.put(HIGH, maxValue);
                    map.put(LOW, minValue);
                    map.put(COMPARATOR, comp);

                    entityParams.put(field, map);
                }
                cache.put(configIndex + "." + configType + "." + configName,
                        entityParams);
            }
        }

        HashMap<String, Collection<String>> props =
                new HashMap<String, Collection<String>>();

        readFields(fields, props);
        return new RecordImpl(props);
    }

    private void readFields(List<Map<String, Object>> fields, Map<String, Collection<String>> props) {
        for (Map<String, Object> value : fields) {
            String field = (String) value.get("field");
            String fieldValue = (String) value.get("value");
            for (Cleaner cl : (ArrayList<Cleaner>) entityParams.get(field).get(CLEANERS)) {
                fieldValue = cl.clean(fieldValue);
            }
            props.put(field, Collections.singleton(fieldValue));
        }
    }

    /**
     * . Configures with data from JSON payload only
     *
     * @param fieldsParams fields parameters from JSON
     * @return the record for comparison
     */
    private Record configureWithFieldsOnly(
            final List<Map<String, Object>> fieldsParams) {

        Map<String, Collection<String>> props =
                new HashMap<String, Collection<String>>();
        entityParams = new HashMap<String, HashMap<String, Object>>();

        for (Map<String, Object> value : fieldsParams) {
            HashMap<String, Object> map = new HashMap<String, Object>();

            String field = (String) value.get("field");

            List<Cleaner> cleanList =
                    getCleaners((ArrayList<Map<String, String>>) value.get(CLEANERS));

            map.put(CLEANERS, cleanList);

            Double maxValue = 0.0;
            if (value.get(HIGH) != null) {
                maxValue = (Double) value.get(HIGH);
            }

            Double minValue = 0.0;
            if (value.get(LOW) != null) {
                minValue = (Double) value.get(LOW);
            }

            Comparator comp = getComparator(value);

            map.put(HIGH, maxValue);
            map.put(LOW, minValue);
            map.put(COMPARATOR, comp);

            entityParams.put(field, map);
        }

        readFields(fieldsParams, props);
        return new RecordImpl(props);
    }

    /**
     * . Computes probability that objects are the same
     *
     * @return float the computed score
     */
    @Override
    public double runAsDouble() {
        HashMap<String, Collection<String>> props =
                new HashMap<String, Collection<String>>();
        DocLookup doc = doc();
        Collection<String> docKeys = comparedRecord.getProperties();

        for (String key : docKeys) {
            if (doc.containsKey(key)) {
                String value = getFieldValue(doc.get(key));
                props.put(key, value == null
                        ? Collections.singleton("")
                        : Collections.singleton(value));
            }
        }
        Record r2 = new RecordImpl(props);
        return compare(comparedRecord, r2, entityParams);
    }

    /**

     */

    /**
     * Factory
     */
    public static class Factory extends AbstractComponent implements
            NativeScriptFactory {
        /**
         * . Node where the plugin is instantiated
         */
        private final Node node;
        /**
         * . Cache to store configuration
         */
        private final Cache<String, HashMap<String, HashMap<String, Object>>> cache;

        /**
         * . This constructor will be called by guice during initialization
         *
         * @param aNode    node reference injecting the reference to current node to
         *                 get access to node's client
         * @param settings cluster settings
         */
        @Inject
        public Factory(final Node aNode, final Settings settings) {
            super(settings);
            // Node is not fully initialized here
            // All we can do is save a reference to it for future use
            this.node = aNode;

            TimeValue expire =
                    settings.getAsTime("entity-resolution.cache.expire",
                            new TimeValue(1L, TimeUnit.HOURS));
            ByteSizeValue size =
                    settings.getAsBytesSize(
                            "entity-resolution.cache.size", null);
            CacheBuilder<Object, Object> cacheBuilder =
                    CacheBuilder.newBuilder();
            cacheBuilder.expireAfterAccess(expire.seconds(), TimeUnit.SECONDS);
            if (size != null) {
                cacheBuilder.maximumSize(size.bytes());
            }
            cache = cacheBuilder.build();
        }

        /**
         * This method is called for every search on every shard.
         *
         * @param params list of script parameters passed with the query
         * @return new native script
         */
        @Override
        public final ExecutableScript newScript(
                @Nullable final Map<String, Object> params) {
            if (params.get("entity") == null) {
                throw new ElasticsearchIllegalArgumentException(
                        "Missing the parameters");
            }

            return new EntityResolutionScript(
                    (Map<String, Object>) params.get("entity"),
                    cache,
                    node.client());
        }

    }
}
