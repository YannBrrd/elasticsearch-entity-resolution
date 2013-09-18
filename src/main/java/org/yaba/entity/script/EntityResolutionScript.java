package org.yaba.entity.script;

import no.priv.garshol.duke.Cleaner;
import no.priv.garshol.duke.Comparator;
import no.priv.garshol.duke.Record;
import no.priv.garshol.duke.RecordImpl;
import no.priv.garshol.duke.comparators.Levenshtein;
import no.priv.garshol.duke.utils.Utils;
import no.priv.garshol.duke.utils.ObjectUtils;

import org.elasticsearch.node.Node;
import org.elasticsearch.script.AbstractSearchScript;
import org.elasticsearch.ElasticSearchException;
import org.elasticsearch.ElasticSearchIllegalArgumentException;
import org.elasticsearch.script.ExecutableScript;
import org.elasticsearch.script.NativeScriptFactory;
import org.elasticsearch.search.lookup.DocLookup;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.cache.Cache;
import org.elasticsearch.common.cache.CacheBuilder;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.fielddata.ScriptDocValues;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("unchecked")
public class EntityResolutionScript extends AbstractSearchScript {

    private Record comparedRecord = null;
    private HashMap<String, HashMap<String, Object>> entityParams;

    private final Cache<String, HashMap<String, HashMap<String, Object>>> cache;
    private final Client client;
    private String configIndex = "entity-index";
    private String configType = "configuration";
    private String configName = "configuration";

    /**
     * Native scripts are build using factories that are registered in the
     * {@link org.elasticsearch.examples.nativescript.plugin.NativeScriptExamplesPlugin#onModule(org.elasticsearch.script.ScriptModule)}
     * method when plugin is loaded.
     */
    public static class Factory extends AbstractComponent implements NativeScriptFactory {

	private final Node node;

	private final Cache<String, HashMap<String, HashMap<String, Object>>> cache;

	/**
	 * This constructor will be called by guice during initialization
	 * 
	 * @param node
	 *            injecting the reference to current node to get access to
	 *            node's client
	 */
	@Inject
	public Factory(Node node, Settings settings) {
	    super(settings);
	    // Node is not fully initialized here
	    // All we can do is save a reference to it for future use
	    this.node = node;

	    TimeValue expire = settings.getAsTime("expire", null);
	    CacheBuilder<Object, Object> cacheBuilder = CacheBuilder.newBuilder();
	    if (expire != null) {
		cacheBuilder.expireAfterAccess(expire.seconds(), TimeUnit.SECONDS);
	    } else {
		// Default expiration is 1 hour
		cacheBuilder.expireAfterAccess(1L, TimeUnit.HOURS);
	    }
	    cache = cacheBuilder.build();
	}

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
		throw new ElasticSearchIllegalArgumentException("Missing the parameters");
	    }

	    return new EntityResolutionScript((Map<String, Object>) params.get("entity"), cache, node.client());
	}

    }

    private EntityResolutionScript(Map<String, Object> params, Cache<String, HashMap<String, HashMap<String, Object>>> cache, Client client) {

	if (params.get("fields") == null)
	    throw new ElasticSearchIllegalArgumentException("Missing the 'fields' parameters");

	this.cache = cache;
	this.client = client;

	if (params.get("configuration") == null) {
	    comparedRecord = configureWithFieldsOnly((ArrayList<Map<String, Object>>) params.get("fields"));
	} else {
	    comparedRecord = configureWithFieldsAndConfiguration((Map<String, Object>) params.get("configuration"), (ArrayList<Map<String, Object>>) params.get("fields"));
	}

    }

    /**
     * Configures with data within ES index
     * 
     * @param configuration
     * @param fields
     * @return
     */
    private Record configureWithFieldsAndConfiguration(Map<String, Object> configuration, ArrayList<Map<String, Object>> fields) {

	configIndex = (String) configuration.get("index");
	configType = (String) configuration.get("type");
	configName = (String) configuration.get("name");

	if ((entityParams = cache.getIfPresent(configIndex + "." + configType + "." + configName)) == null) {
	    GetResponse response = client.prepareGet(configIndex, configType, configName).setPreference("_local").execute().actionGet();

	    if (response.isExists()) {
		Map<String, Object> entityConf = (Map<String, Object>) response.getSource().get("entity");
		entityParams = new HashMap<String, HashMap<String, Object>>();
		if (entityConf == null)
		    throw new ElasticSearchIllegalArgumentException("No conf found in " + configIndex + "/" + configType + "/" + configName);

		ArrayList<Map<String, Object>> confFields = (ArrayList<Map<String, Object>>) entityConf.get("fields");

		if (confFields == null)
		    throw new ElasticSearchIllegalArgumentException("Bad conf found in " + configIndex + "/" + configType + "/" + configName);

		System.out.println("Config : " + confFields);

		Iterator<Map<String, Object>> it = confFields.iterator();

		while (it.hasNext()) {
		    HashMap<String, Object> map = new HashMap<String, Object>();
		    Map<String, Object> value = it.next();

		    String field = (String) value.get("field");
		    System.out.println("Field : " + field);
		    Iterator<String> cleanIt = ((ArrayList<String>) value.get("cleaners")).iterator();
		    ArrayList<Cleaner> cleanList = new ArrayList<Cleaner>();

		    while (cleanIt.hasNext()) {
			String cleanerName = (String) cleanIt.next();
			System.out.println("Cleaner : " + cleanerName);

			Cleaner cleaner = (Cleaner) ObjectUtils.instantiate(cleanerName);
			cleanList.add(cleaner);
		    }

		    map.put("cleaners", cleanList);

		    Double maxValue = (value.get("high") == null ? 0.0 : Double.valueOf(((Double) value.get("high"))));
		    Double minValue = (value.get("low") == null ? 0.0 : Double.valueOf(((Double) value.get("low"))));

		    String comparatorName = (value.get("comparator") == null ? Levenshtein.class.getName() : (String) value.get("comparator"));

		    Comparator comp = (Comparator) ObjectUtils.instantiate(comparatorName);
		    map.put("high", maxValue);
		    map.put("low", minValue);
		    map.put("comparator", comp);

		    System.out.println("High : " + maxValue);
		    System.out.println("Low : " + minValue);
		    System.out.println("Comparator : " + comparatorName);

		    System.out.println("Map : " + map);

		    entityParams.put(field, map);
		}

		System.out.println("Entity Params : " + entityParams);

		cache.put(configIndex + "." + configType + "." + configName, entityParams);
	    }

	}

	HashMap<String, Collection<String>> props = new HashMap<String, Collection<String>>();

	Iterator<Map<String, Object>> it = fields.iterator();
	while (it.hasNext()) {
	    Map<String, Object> value = it.next();

	    String field = (String) value.get("field");
	    String fieldValue = (String) value.get("value");
	    props.put(field, Collections.singleton(fieldValue));

	}
	return new RecordImpl(props);
    }

    /**
     * Configures with data from JSON payload only
     * 
     * @param fieldsParams
     * @return the record for comparison
     */
    private Record configureWithFieldsOnly(ArrayList<Map<String, Object>> fieldsParams) {

	HashMap<String, Collection<String>> props = new HashMap<String, Collection<String>>();
	entityParams = new HashMap<String, HashMap<String, Object>>();

	Iterator<Map<String, Object>> it = fieldsParams.iterator();
	while (it.hasNext()) {
	    Map<String, Object> value = it.next();
	    HashMap<String, Object> map = new HashMap<String, Object>();

	    String field = (String) value.get("field");
	    String fieldValue = (String) value.get("value");

	    Iterator<String> cleanIt = ((ArrayList<String>) value.get("cleaners")).iterator();
	    ArrayList<Cleaner> cleanList = new ArrayList<Cleaner>();

	    while (cleanIt.hasNext()) {
		String cleanerName = (String) cleanIt.next();

		Cleaner cleaner = (Cleaner) ObjectUtils.instantiate(cleanerName);
		cleanList.add(cleaner);
		fieldValue = cleaner.clean(fieldValue);
	    }

	    map.put("cleaners", cleanList);

	    props.put(field, Collections.singleton(fieldValue));

	    Double maxValue = (value.get("high") == null ? 0.0 : Double.valueOf(((Double) value.get("high"))));
	    Double minValue = (value.get("low") == null ? 0.0 : Double.valueOf(((Double) value.get("low"))));

	    String comparatorName = (value.get("comparator") == null ? Levenshtein.class.getName() : (String) value.get("comparator"));

	    Comparator comp = (Comparator) ObjectUtils.instantiate(comparatorName);
	    map.put("high", maxValue);
	    map.put("low", minValue);
	    map.put("comparator", comp);

	    entityParams.put(field, map);
	}
	return new RecordImpl(props);
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
		props2.put(key, value == null ? Collections.singleton("") : Collections.singleton(value));
	    }
	}

	Record r2 = new RecordImpl(props2);

	return new Double(compare(comparedRecord, r2, entityParams)).floatValue();
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
	    throw new ElasticSearchException("No comparator implemented for GeoPoints");

	return result;
    }

    /**
     * Compares two records and returns the probability that they represent the
     * same real-world entity.
     */
    private double compare(Record r1, Record r2, HashMap<String, HashMap<String, Object>> params) {
	double prob = 0.5;

	System.out.println("Comparing " + r1 + " " + r2 + " params : " + params);
	System.out.println("Properties " + r1.getProperties());

	for (String propname : r1.getProperties()) {
	    System.out.println("Propname " + propname);

	    Collection<String> vs1 = r1.getValues(propname);
	    Collection<String> vs2 = r2.getValues(propname);

	    Double max = (Double) params.get(propname).get("high");
	    Double min = (Double) params.get(propname).get("low");

	    if (vs1 == null || vs1.isEmpty() || vs2 == null || vs2.isEmpty())
		continue; // no values to compare, so skip

	    double high = 0.0;
	    for (String v1 : vs1) {
		if (v1.equals(""))
		    continue;

		for (String v2 : vs2) {
		    if (v2.equals(""))
			continue;

		    Comparator comp = (Comparator) params.get(propname).get("comparator");

		    ArrayList<Cleaner> cleanersList = (ArrayList<Cleaner>) params.get(propname).get("cleaners");

		    Iterator<Cleaner> clIt = cleanersList.iterator();

		    System.out.println("V1 : " + v1);
		    System.out.println("V2 : " + v2);

		    while (clIt.hasNext()) {
			Cleaner cl = clIt.next();
			System.out.println("cl : " + cl);

			v1 = cl.clean(v1);
			v2 = cl.clean(v2);
			System.out.println("V1 : " + v1);
			System.out.println("V2 : " + v2);

		    }

		    System.out.println("Comparing " + v1 + " " + v2 + " max : " + max + " min : " + min + " using : " + comp);

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
     */
    public double compare(String v1, String v2, double high, double low, Comparator comparator) {

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
	return null;
    }
}
