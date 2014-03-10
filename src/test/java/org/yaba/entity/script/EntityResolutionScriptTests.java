package org.yaba.entity.script;

import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.collect.MapBuilder;
import org.elasticsearch.common.lucene.search.function.CombineFunction;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static java.lang.Float.valueOf;
import static org.elasticsearch.index.query.QueryBuilders.functionScoreQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.elasticsearch.test.hamcrest.ElasticsearchAssertions.assertAcked;
import static org.hamcrest.Matchers.equalTo;

public class EntityResolutionScriptTests extends AbstractSearchScriptTests {

    private final static Logger logger = Logger.getAnonymousLogger();

    @Test
    public void testEntity() throws Exception {


        // Create a new test index
        String test_mapping =
                XContentFactory.jsonBuilder()
                        .startObject()
                        .startObject("city")
                        .startObject("properties")
                        .startObject("city")
                        .field("type", "string")
                        .endObject()
                        .startObject("state")
                        .field("type", "string")
                        .field("index", "not_analyzed")
                        .endObject()
                        .startObject("population")
                        .field("type", "integer")
                        .endObject()
                        .startObject("position")
                        .field("type", "geo_point")
                        .endObject()
                        .endObject()
                        .endObject()
                        .endObject()
                        .string();

        assertAcked(prepareCreate("test").addMapping("city", test_mapping));

        List<IndexRequestBuilder> indexBuilders = new ArrayList<>();

        // Index main records
        indexBuilders.add(client()
                .prepareIndex("test", "city", "1")
                .setSource("city", "Cambridge", "state", "MA", "population",
                        105162, "position", "42.373746,71.110554"));
        indexBuilders.add(client()
                .prepareIndex("test", "city", "2")
                .setSource("city", "South Burlington", "state", "VT",
                        "population", 17904, "position", "44.451846,73.181710"));
        indexBuilders.add(client()
                .prepareIndex("test", "city", "3")
                .setSource("city", "South Portland", "state", "ME",
                        "population", 25002, "position", "43.631549,70.272724"));
        indexBuilders.add(client().prepareIndex("test", "city", "4")
                .setSource("city", "Essex", "state", "VT", "population", 19587, "position", "44.492905,73.108601")
        );
        indexBuilders.add(client()
                .prepareIndex("test", "city", "5")
                .setSource("city", "Portland", "state", "ME", "population",
                        66194, "position", "43.665116,70.269086"));
        indexBuilders.add(client()
                .prepareIndex("test", "city", "6")
                .setSource("city", "Burlington", "state", "VT", "population",
                        42417, "position", "44.484748,73.223157"));
        indexBuilders.add(client()
                .prepareIndex("test", "city", "7")
                .setSource("city", "Stamford", "state", "CT", "population",
                        122643, "position", "41.074448,73.541316"));
        indexBuilders.add(client()
                .prepareIndex("test", "city", "8")
                .setSource("city", "Colchester", "state", "VT", "population",
                        17067, "position", "44.3231,73.148"));
        indexBuilders.add(client()
                .prepareIndex("test", "city", "9")
                .setSource("city", "Concord", "state", "NH", "population",
                        42695, "position", "43.220093,71.549127"));
        indexBuilders.add(client()
                .prepareIndex("test", "city", "10")
                .setSource("city", "Boston", "state", "MA", "population",
                        617594, "position", "42.321597,71.089115"));

        indexRandom(true, indexBuilders);

        // Script parameters
        Map<String, Object> params =
                MapBuilder.<String, Object>newMapBuilder().map();

        ArrayList<Map<String, Object>> fields;
        fields = new ArrayList<>();

        Map<String, Object> aField =
                MapBuilder
                        .<String, Object>newMapBuilder()
                        .put("field", "city")
                        .put("value", "South")
                        .put("comparator", MapBuilder.<String, Object>newMapBuilder()
                                .put("name", "no.priv.garshol.duke.comparators.JaroWinkler")
                                .map())
                        .put("low", 0.1)
                        .put("high", 0.95)
                        .put("cleaners", new Map[]{
                                MapBuilder.<String, Object>newMapBuilder()
                                        .put("name", "no.priv.garshol.duke.cleaners.TrimCleaner")
                                        .map(),
                                MapBuilder.<String, Object>newMapBuilder()
                                        .put("name", "no.priv.garshol.duke.cleaners.LowerCaseNormalizeCleaner")
                                        .map()})
                        .map();

        fields.add(aField);

        aField =
                MapBuilder
                        .<String, Object>newMapBuilder()
                        .put("field", "state")
                        .put("value", "ME")
                        .put("comparator", MapBuilder.<String, Object>newMapBuilder()
                                .put("name", "no.priv.garshol.duke.comparators.JaroWinkler")
                                .map())
                        .put("low", 0.1)
                        .put("high", 0.95)
                        .put("cleaners", new Map[]{
                                MapBuilder.<String, Object>newMapBuilder()
                                        .put("name", "no.priv.garshol.duke.cleaners.LowerCaseNormalizeCleaner")
                                        .map()})
                        .map();

        fields.add(aField);

        aField =
                MapBuilder
                        .<String, Object>newMapBuilder()
                        .put("field", "population")
                        .put("value", "26000")
                        .put("comparator", MapBuilder.<String, Object>newMapBuilder()
                                .put("name", "no.priv.garshol.duke.comparators.NumericComparator")
                                .map())
                        .put("low", 0.1)
                        .put("high", 0.95)
                        .put("cleaners", new Map[]{
                                MapBuilder.<String, Object>newMapBuilder()
                                        .put("name", "no.priv.garshol.duke.cleaners.DigitsOnlyCleaner")
                                        .map()})
                        .map();

        fields.add(aField);

        aField =
                MapBuilder
                        .<String, Object>newMapBuilder()
                        .put("field", "position")
                        .put("value", "43,70")
                        .put("comparator", MapBuilder.<String, Object>newMapBuilder()
                                .put("name", "no.priv.garshol.duke.comparators.GeopositionComparator")
                                .put("params", MapBuilder.<String, Object>newMapBuilder()
                                        .put("max-distance", "100").map()
                                ).map())
                        .put("low", 0.1)
                        .put("high", 0.95)
                        .put("cleaners", new Map[]{
                                MapBuilder.<String, Object>newMapBuilder()
                                        .put("name", "no.priv.garshol.duke.cleaners.LowerCaseNormalizeCleaner")
                                        .map()})
                        .map();

        fields.add(aField);

        params.put(
                "entity",
                new MapBuilder<String, ArrayList<Map<String, Object>>>().put(
                        "fields", fields).map());

        // Find all objects
        SearchRequestBuilder request =
                client()
                        .prepareSearch("test")
                        .setTypes("city")
                        .setQuery(
                                functionScoreQuery(
                                        (matchAllQuery()))
                                        .boostMode(CombineFunction.REPLACE)
                                        .add(ScoreFunctionBuilders.scriptFunction("entity-resolution", "native", params)))
                        .setSize(4);

        logger.info(request.toString());

        SearchResponse searchResponse = request.execute().actionGet();

        assertThat(Arrays.toString(searchResponse.getShardFailures()),
                searchResponse.getFailedShards(), equalTo(0));

        logger.info(searchResponse.toString());

        assertThat(searchResponse.getHits().getAt(0).getSource().get("city")
                .toString(), equalTo("South Portland"));
        assertThat(searchResponse.getHits().getAt(0).getScore(), equalTo(
                valueOf("0.97579086")));

        assertThat(searchResponse.getHits().getAt(1).getSource().get("city")
                .toString(), equalTo("Portland"));
        assertThat(searchResponse.getHits().getAt(1).getScore(), equalTo(
                valueOf("0.29081574")));

        assertThat(searchResponse.getHits().getAt(2).getSource().get("city")
                .toString(), equalTo("Boston"));
        assertThat(searchResponse.getHits().getAt(2).getScore(), equalTo(
                valueOf("0.057230186")));

        assertThat(searchResponse.getHits().getAt(3).getSource().get("city")
                .toString(), equalTo("South Burlington"));
        assertThat(searchResponse.getHits().getAt(3).getScore(), equalTo(
                valueOf("0.049316783")));
    }
}
