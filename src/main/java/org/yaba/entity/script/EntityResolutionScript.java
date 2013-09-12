package org.yaba.entity.script;

import no.priv.garshol.duke.Comparator;
import no.priv.garshol.duke.Property;
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
import org.yaba.entity.config.EntityConfiguration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
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
				parameters = (ArrayList<Map<String, Object>>) params.get("entity");
				logger.info("Duke parameters: " + parameters);
			}
			
			logger.info("Entity Params : " + params.get("entity").getClass());

			return new EntityResolutionScript(parameters);

		}

	}

	private EntityResolutionScript(ArrayList<Map<String, Object>> params) {
		logger.info("------> Duke params " + params);

		EntityConfiguration conf = new EntityConfiguration(params);

		this.entity = params;

	}

	@Override
	public Object run() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public float runAsFloat() {

		HashMap<String, Collection<String>> props = new HashMap<String, Collection<String>>();

		java.util.Iterator<Map<String, Object>> it = entity.iterator();

		while (it.hasNext()) {
			Map<String, Object> value = it.next();

			String field = (String) value.get("field");
			
			logger.info("------> Field " + field);
			
			String fieldValue = (String) value.get("value");
			
			logger.info("------> Value " + value);
			
			props.put(field, Collections.singleton(fieldValue));

		}
		
		Record r1 = new RecordImpl(props);
		
		
		DocLookup doc = doc();
		
		Set docKeys = props.keySet();
		
		props.clear();
		
		java.util.Iterator it2 = docKeys.iterator();
		
		while (it2.hasNext()) {
			String key = (String) it2.next();
			
			logger.info("------> Key " + key);
			
			String value = (String) doc.get(key);
			
			logger.info("------> Value " + value);
			
			props.put(key, Collections.singleton(value));
		}
		
		Record r2 = new RecordImpl (props);

		return new Double(compare(r1,r2)).floatValue();
	}

	  /**
	   * Compares two records and returns the probability that they
	   * represent the same real-world entity.
	   */
	  public double compare(Record r1, Record r2) {
	    double prob = 0.5;
	    
	    Comparator comp = new Levenshtein();
	    
	    for (String propname : r1.getProperties()) {
	      
	      Collection<String> vs1 = r1.getValues(propname);
	      Collection<String> vs2 = r2.getValues(propname);
	      if (vs1 == null || vs1.isEmpty() || vs2 == null || vs2.isEmpty())
	        continue; // no values to compare, so skip
	      
	      double high = 0.0;
	      for (String v1 : vs1) {
	        if (v1.equals("")) // FIXME: these values shouldn't be here at all
	          continue;
	        
	        for (String v2 : vs2) {
	          if (v2.equals("")) // FIXME: these values shouldn't be here at all
	            continue;
	        
	          try {
	            double p = comp.compare(v1, v2);
	            high = Math.max(high, p);
	          } catch (Exception e) {
	            throw new RuntimeException("Comparison of values '" + v1 + "' and "+
	                                       "'" + v2 + "' with " +
	                                       "Levenshtein failed", e);
	          }
	        }
	      }
	      prob = Utils.computeBayes(prob, high);
	    }
	    return prob;
	  }
}
