/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.yaba.entity.script;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import no.priv.garshol.duke.Cleaner;
import no.priv.garshol.duke.Comparator;
import no.priv.garshol.duke.Record;
import no.priv.garshol.duke.RecordImpl;
import no.priv.garshol.duke.comparators.Levenshtein;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.Nullable;
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
import org.elasticsearch.search.lookup.LeafDocLookup;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static no.priv.garshol.duke.utils.ObjectUtils.instantiate;
import static no.priv.garshol.duke.utils.ObjectUtils.setBeanProperty;
import static no.priv.garshol.duke.utils.Utils.computeBayes;

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
    final static public String SCRIPT_NAME = "entity-resolution";
    private static final String FIELDS = "fields";
    private static final String COMPARATOR = "comparator";
    private static final String CLEANERS = "cleaners";
    private static final String PARAMS = "params";
    private static final String OBJECTS = "objects";
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
    private Map<String, HashMap<String, Object>> entityParams;

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
            throw new IllegalArgumentException(
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
        List<Cleaner> cleanList = new ArrayList<>();

        for (Map aCleaner : cleanersList) {
            Cleaner cleaner = (Cleaner) instantiate((String) aCleaner.get(NAME));
            setParams(cleaner, aCleaner.get(PARAMS));
            cleanList.add(cleaner);
        }
        return cleanList;
    }

    /**
     * Sets params for cleaners or comparators
     *
     * @param anObject the object to parametrize
     * @param params   params list
     */
    private static void setParams(Object anObject, Object params) {
        if (params != null) {
            Map<String, String> paramsMap = (Map<String, String>) params;
            for (Map.Entry<String, String> entry : paramsMap.entrySet()) {
                setBeanProperty(anObject, entry.getKey(), entry.getValue(), null);
            }
        }
    }


    /**
     * Sets objects for comparators
     *
     * @param anObject the object to parametrize
     * @param objects  objects list
     */
    private static void setObjects(Object anObject, Object objects) {
        if (objects != null) {
            Object currentobj;
            HashMap<String, Object> list = new HashMap<>();
            Map<String, HashMap> paramsMap = (Map<String, HashMap>) objects;
            for (Map.Entry<String, HashMap> entry : paramsMap.entrySet()) {
                HashMap<String, String> object = entry.getValue();
                String klass = object.get("class");
                String name = object.get("name");
                currentobj = instantiate(klass);
                list.put(klass, currentobj);
                setBeanProperty(anObject, name, klass, list);
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


        Comparator comp = (Comparator) instantiate(comparatorName);

        setParams(comp, compEntity.get(PARAMS));

        setObjects(comp, compEntity.get(OBJECTS));

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
            result = String.format(Locale.getDefault(), "%s,%s", point.getLat(), point.getLon());
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
            final Map<String, HashMap<String, Object>> params) {
        double prob = AVERAGE_SCORE;

        for (String propname : r1.getProperties()) {
            Collection<String> vs1 = r1.getValues(propname);
            Collection<String> vs2 = r2.getValues(propname);

            Boolean v1empty = true;
            for (String v1 : vs1) {
                if (!v1.equals("")) {
                    v1empty = false;
                    break;
                }
            }

            Boolean v2empty = true;
            for (String v2 : vs2) {
                if (!v2.equals("")) {
                    v2empty = false;
                    break;
                }
            }


            if (vs1.isEmpty() || vs2.isEmpty() || v1empty || v2empty) {
                continue; // no values to compare, so skip
            }

            Comparator comp =
                    (Comparator) params.get(propname).get(COMPARATOR);
            ArrayList<Cleaner> cleanersList =
                    (ArrayList<Cleaner>) params.get(propname).get(CLEANERS);

            Double max = (Double) params.get(propname).get(HIGH);
            Double min = (Double) params.get(propname).get(LOW);

            double high = computeProb(vs1, vs2, comp, cleanersList, max, min);
            prob = computeBayes(prob, high);
        }
        return prob;
    }

    private static double computeProb(Collection<String> vs1, Collection<String> vs2, Comparator comp, List<Cleaner> cleanersList, Double max, Double min) {
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
        return high;
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
                entityParams = new HashMap<>();
                if (entityConf == null) {
                    throw new IllegalArgumentException(
                            "No conf found in " + configIndex + "/"
                                    + configType + "/" + configName);
                }

                ArrayList<Map<String, Object>> confFields =
                        (ArrayList<Map<String, Object>>) entityConf
                                .get("fields");

                if (confFields == null) {
                    throw new IllegalArgumentException(
                            "Bad conf found in " + configIndex + "/"
                                    + configType + "/" + configName);
                }

                for (Map<String, Object> confField : confFields) {
                    HashMap<String, Object> map = new HashMap<>();

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
                        (HashMap) entityParams);
            }
        }

        HashMap<String, Collection<String>> props =
                new HashMap<>();

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
                new HashMap<>();
        entityParams = new HashMap<>();

        for (Map<String, Object> value : fieldsParams) {
            HashMap<String, Object> map = new HashMap<>();

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
                new HashMap<>();
        LeafDocLookup doc = doc();
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
        @SuppressWarnings("unchecked")
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
                throw new IllegalArgumentException(
                        "Missing the parameters");
            }

            return new EntityResolutionScript(
                    (Map<String, Object>) params.get("entity"),
                    cache,
                    node.client());
        }

        /**
         * Indicates if document scores may be needed by the produced scripts.
         *
         * @return {@code true} if scores are needed.
         */
        @Override
        public boolean needsScores() {
            return false;
        }

    }
}
