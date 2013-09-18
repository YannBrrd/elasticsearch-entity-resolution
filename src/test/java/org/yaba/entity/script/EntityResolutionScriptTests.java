package org.yaba.entity.script;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.collect.MapBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.indices.IndexMissingException;
import org.testng.annotations.Test;

public class EntityResolutionScriptTests extends AbstractSearchScriptTests {

    @Test
    public void testEntity() throws Exception {
	// Delete the old index
	try {
	    node.client().admin().indices().prepareDelete("test").execute()
		    .actionGet();
	    node.client().admin().indices().prepareDelete("entity-test")
		    .execute().actionGet();
	} catch (IndexMissingException ex) {
	    // Ignore
	}

	// Create a new test index
	String test_mapping = XContentFactory.jsonBuilder().startObject()
		.startObject("city").startObject("properties")
		.startObject("city").field("type", "string").endObject()
		.startObject("state").field("type", "string")
		.field("index", "not_analyzed").endObject()
		.startObject("population").field("type", "integer").endObject()
		.endObject().endObject().endObject().string();
	
	node.client().admin().indices().prepareCreate("test")
		.addMapping("city", test_mapping).execute().actionGet();

	// Index main records
	node.client()
		.prepareIndex("test", "city", "1")
		.setSource("city", "Cambridge", "state", "MA", "population",
			105162).execute().actionGet();
	node.client()
		.prepareIndex("test", "city", "2")
		.setSource("city", "South Burlington", "state", "VT",
			"population", 17904).execute().actionGet();
	node.client()
		.prepareIndex("test", "city", "3")
		.setSource("city", "South Portland", "state", "ME",
			"population", 25002).execute().actionGet();
	node.client().prepareIndex("test", "city", "4")
		.setSource("city", "Essex", "state", "VT", "population", 19587)
		.execute().actionGet();
	node.client()
		.prepareIndex("test", "city", "5")
		.setSource("city", "Portland", "state", "ME", "population",
			66194).execute().actionGet();
	node.client()
		.prepareIndex("test", "city", "6")
		.setSource("city", "Burlington", "state", "VT", "population",
			42417).execute().actionGet();
	node.client()
		.prepareIndex("test", "city", "7")
		.setSource("city", "Stamford", "state", "CT", "population",
			122643).execute().actionGet();
	node.client()
		.prepareIndex("test", "city", "8")
		.setSource("city", "Colchester", "state", "VT", "population",
			17067).execute().actionGet();
	node.client()
		.prepareIndex("test", "city", "9")
		.setSource("city", "Concord", "state", "NH", "population",
			42695).execute().actionGet();
	node.client()
		.prepareIndex("test", "city", "10")
		.setSource("city", "Boston", "state", "MA", "population",
			617594).execute().actionGet();

	node.client().admin().indices().prepareRefresh("test").execute()
		.actionGet();

	// Script parameters
	Map<String, Object> params = MapBuilder
		.<String, Object> newMapBuilder().map();

	ArrayList<Map<String, Object>> fields = new ArrayList<Map<String, Object>>();

	Map<String, Object> aField = MapBuilder
		.<String, Object> newMapBuilder()
		.put("field", "city")
		.put("value", "South")
		.put("comparator",
			"no.priv.garshol.duke.comparators.Levenshtein")
		.put("low", 0.1)
		.put("high", 0.95)
		.put("cleaners",
			new String[] {
				"no.priv.garshol.duke.cleaners.TrimCleaner",
				"no.priv.garshol.duke.cleaners.LowerCaseNormalizeCleaner" })
		.map();

	fields.add(aField);

	aField = MapBuilder
		.<String, Object> newMapBuilder()
		.put("field", "state")
		.put("value", "ME")
		.put("comparator",
			"no.priv.garshol.duke.comparators.Levenshtein")
		.put("low", 0.1)
		.put("high", 0.95)
		.put("cleaners",
			new String[] { "no.priv.garshol.duke.cleaners.LowerCaseNormalizeCleaner" })
		.map();

	fields.add(aField);

	aField = MapBuilder
		.<String, Object> newMapBuilder()
		.put("field", "population")
		.put("value", "26000")
		.put("comparator",
			"no.priv.garshol.duke.comparators.NumericComparator")
		.put("low", 0.1)
		.put("high", 0.95)
		.put("cleaners",
			new String[] { "no.priv.garshol.duke.cleaners.DigitsOnlyCleaner" })
		.map();

	fields.add(aField);

	params.put(
		"entity",
		new MapBuilder<String, ArrayList<Map<String, Object>>>().put(
			"fields", fields).map());

	// Find all objects
	SearchResponse searchResponse = node
		.client()
		.prepareSearch("test")
		.setTypes("city")
		.setQuery(
			QueryBuilders
				.customScoreQuery(
					(QueryBuilders.matchAllQuery()))
				.script("entity-resolution").lang("native")
				.params(params)).setSize(4).execute()
		.actionGet();

	assertThat(Arrays.toString(searchResponse.getShardFailures()),
		searchResponse.getFailedShards(), equalTo(0));

	assertThat(searchResponse.getHits().getAt(0).getSource().get("city")
		.toString(), equalTo("South Portland"));
	assertThat(searchResponse.getHits().getAt(0).getScore(), equalTo(Float
		.valueOf("0.95843065").floatValue()));

	assertThat(searchResponse.getHits().getAt(1).getSource().get("city")
		.toString(), equalTo("Portland"));
	assertThat(searchResponse.getHits().getAt(1).getScore(), equalTo(Float
		.valueOf("0.19").floatValue()));

	assertThat(searchResponse.getHits().getAt(2).getSource().get("city")
		.toString(), equalTo("Essex"));
	assertThat(searchResponse.getHits().getAt(2).getScore(), equalTo(Float
		.valueOf("0.03672479").floatValue()));

	assertThat(searchResponse.getHits().getAt(3).getSource().get("city")
		.toString(), equalTo("South Burlington"));
	assertThat(searchResponse.getHits().getAt(3).getScore(), equalTo(Float
		.valueOf("0.029812464").floatValue()));
    }
}
