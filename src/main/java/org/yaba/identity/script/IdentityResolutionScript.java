package org.yaba.identity.script;

import org.elasticsearch.script.AbstractSearchScript;
import org.elasticsearch.ElasticSearchIllegalArgumentException;
import org.elasticsearch.script.ExecutableScript;
import org.elasticsearch.script.NativeScriptFactory;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.xcontent.support.XContentMapValues;

import java.util.Map;

public class IdentityResolutionScript extends AbstractSearchScript {

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
			// Mandatory arr parameter
			// The XContentMapValues helper class can be used to simplify
			// parameter parsing
			String fieldName = params == null ? null : params.get("fields");
			if (fieldName == null) {
				throw new ElasticSearchIllegalArgumentException(
						"Missing the field parameter");
			}

			// Example of an optional integer parameter
			int certainty = params == null ? 10 : XContentMapValues
					.nodeIntegerValue(params.get("certainty"), 10);
			return new IdentityResolutionScript(fieldName, certainty);
		}
	}

	@Override
	public Object run() {
		// TODO Auto-generated method stub
		return null;
	}

}
