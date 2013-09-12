package org.yaba.entity.script;

import no.priv.garshol.duke.Comparator;
import no.priv.garshol.duke.Record;
import no.priv.garshol.duke.RecordImpl;
import no.priv.garshol.duke.comparators.Levenshtein;
import no.priv.garshol.duke.utils.Utils;

import org.elasticsearch.script.AbstractSearchScript;
import org.elasticsearch.ElasticSearchIllegalArgumentException;
import org.elasticsearch.script.ExecutableScript;
import org.elasticsearch.script.NativeScriptFactory;
import org.elasticsearch.search.lookup.DocLookup;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.xcontent.support.XContentMapValues;
import org.elasticsearch.index.fielddata.ScriptDocValues;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("unchecked")
public class EntityResolutionScript extends AbstractSearchScript {

	private final static ESLogger logger = Loggers
			.getLogger(EntityResolutionScript.class);

	private ArrayList<Map<String, Object>> entity;

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
			// Mandatory array parameter

			if (params.get("entity") == null) {
				throw new ElasticSearchIllegalArgumentException(
						"Missing the parameters");
			}

			boolean isArray = XContentMapValues.isArray(params.get("entity"));

			ArrayList<Map<String, Object>> parameters = null;

			if (isArray) {
				parameters = (ArrayList<Map<String, Object>>) params
						.get("entity");
			} else {
				{
					throw new ElasticSearchIllegalArgumentException(
							"entity parameters should be an array");
				}
			}

			return new EntityResolutionScript(parameters);

		}

	}

	private EntityResolutionScript(ArrayList<Map<String, Object>> params) {
		this.entity = params;
	}

	@Override
	public float runAsFloat() {

		HashMap<String, Collection<String>> props = new HashMap<String, Collection<String>>();
		HashMap<String, Collection<String>> props2 = new HashMap<String, Collection<String>>();

		HashMap<String, HashMap<String, Object>> params = new HashMap<String, HashMap<String, Object>>();

		Iterator<Map<String, Object>> it = entity.iterator();

		while (it.hasNext()) {
			Map<String, Object> value = it.next();

			String field = (String) value.get("field");
			String fieldValue = (String) value.get("value");

			props.put(field, Collections.singleton(fieldValue));

			Double maxValue = (value.get("high") == null ? 0.0 : Double
					.valueOf(((Double) value.get("high"))));
			Double minValue = (value.get("low") == null ? 0.0 : Double
					.valueOf(((Double) value.get("low"))));

			String comparator = (value.get("comparator") == null ? null
					: (String) value.get("comparator"));

			HashMap<String, Object> map = new HashMap<String, Object>();

			map.put("high", maxValue);
			map.put("low", minValue);
			map.put("comparator", comparator);

			params.put(fieldValue, map);

		}

		Record r1 = new RecordImpl(props);

		DocLookup doc = doc();
		Set<String> docKeys = props.keySet();
		Iterator<String> it2 = docKeys.iterator();

		while (it2.hasNext()) {
			String key = (String) it2.next();

			String value = ((ScriptDocValues.Strings) (doc.get(key)))
					.getValue();
			props2.put(key, value == null ? Collections.singleton("")
					: Collections.singleton(value));
		}

		Record r2 = new RecordImpl(props2);

		return new Double(compare(r1, r2, params)).floatValue();
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
				if (v1.equals("")) // FIXME: these values shouldn't be here at
									// all
					continue;

				for (String v2 : vs2) {
					if (v2.equals("")) // FIXME: these values shouldn't be here
										// at all
						continue;

					try {

						Class<?> kompClass = Class.forName((String) params.get(
								v1).get("comparator"));
						Comparator comp = (Comparator) kompClass.newInstance();

						double p = compare(v1, v2,
								(Double) params.get(v1).get("high"),
								(Double) params.get(v1).get("low"), comp);
						high = Math.max(high, p);
					} catch (Exception e) {
						throw new RuntimeException("Comparison of values '"
								+ v1 + "' and " + "'" + v2 + "' with "
								+ (String) params.get(v1).get("comparator")
								+ " failed", e);
					}
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
