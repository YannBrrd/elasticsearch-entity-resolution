package org.yaba.entity.script;

import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.collect.MapBuilder;
import org.elasticsearch.common.lucene.search.function.CombineFunction;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static java.lang.Float.valueOf;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.elasticsearch.index.query.QueryBuilders.functionScoreQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.elasticsearch.test.hamcrest.ElasticsearchAssertions.assertAcked;
import static org.hamcrest.Matchers.equalTo;

public class NeutralFieldsTests extends AbstractSearchScriptTests {
    @Test
    public final void testEntity() throws IOException, ExecutionException, InterruptedException {


        // Create a new test index
        String testMapping =
                jsonBuilder()
                        .startObject()
                        .startObject("user")
                        .startObject("_timestamp")
                        .field("enabled", false)
                        .endObject()
                        .startObject("properties")
                        .startObject("gender")
                        .field("type", "string")
                        .field("index", "not_analyzed")
                        .endObject()
                        .startObject("name")
                        .field("type", "string")
                        .field("index", "not_analyzed")
                        .endObject()
                        .endObject()
                        .endObject()
                        .string();

        assertAcked(prepareCreate("test").addMapping("user", testMapping));

        List<IndexRequestBuilder> indexBuilders = new ArrayList<>();

        // Index main records
        indexBuilders.add(client()
                .prepareIndex("test", "user", "1")
                .setSource("name", "dale", "gender", "m"));
        indexBuilders.add(client()
                .prepareIndex("test", "user", "2")
                .setSource("name", "david"));
        indexBuilders.add(client()
                .prepareIndex("test", "user", "3")
                .setSource("name", "dale"));

        indexRandom(true, indexBuilders);

        // Script parameters
        Map<String, Object> params =
                MapBuilder.<String, Object>newMapBuilder().map();

        ArrayList<Map<String, Object>> fields;
        fields = new ArrayList<>();

        Map<String, Object> aField =
                MapBuilder
                        .<String, Object>newMapBuilder()
                        .put("field", "name")
                        .put("value", "dale")
                        .put("comparator", MapBuilder.<String, Object>newMapBuilder()
                                .put("name", "no.priv.garshol.duke.comparators.ExactComparator")
                                .map())
                        .put("low", 0.1)
                        .put("high", 0.9)
                        .put("cleaners", new Map[]{
                                MapBuilder.<String, Object>newMapBuilder()
                                        .put("name", "no.priv.garshol.duke.cleaners.LowerCaseNormalizeCleaner")
                                        .map()})
                        .map();

        fields.add(aField);

        aField =
                MapBuilder
                        .<String, Object>newMapBuilder()
                        .put("field", "gender")
                        .put("value", "m")
                        .put("comparator", MapBuilder.<String, Object>newMapBuilder()
                                .put("name", "no.priv.garshol.duke.comparators.ExactComparator")
                                .map())
                        .put("low", 0.0)
                        .put("high", 0.9)
                        .put("cleaners", new Map[]{
                                MapBuilder.<String, Object>newMapBuilder()
                                        .put("name", "no.priv.garshol.duke.cleaners.TrimCleaner")
                                        .map(),
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
                        .setTypes("user")
                        .setQuery(
                                functionScoreQuery(
                                        (matchAllQuery()))
                                        .boostMode(CombineFunction.REPLACE)
                                        .scoreMode("max")
                                        .add(ScoreFunctionBuilders.scriptFunction("entity-resolution", "native", params)).add(ScoreFunctionBuilders.scriptFunction("entity-resolution", "native", params)));

        logger.info(request.toString());

        SearchResponse searchResponse = request.execute().actionGet();

        assertThat(Arrays.toString(searchResponse.getShardFailures()),
                searchResponse.getFailedShards(), equalTo(0));

        logger.info(searchResponse.toString());

        assertThat(searchResponse.getHits().getAt(0).getSource().get("name")
                .toString(), equalTo("dale"));
        assertThat(searchResponse.getHits().getAt(0).getScore(), equalTo(
                valueOf("0.9878049")));

        assertThat(searchResponse.getHits().getAt(1).getSource().get("name")
                .toString(), equalTo("dale"));
        assertThat(searchResponse.getHits().getAt(1).getScore(), equalTo(
                valueOf("0.9")));

        assertThat(searchResponse.getHits().getAt(2).getSource().get("name")
                .toString(), equalTo("david"));
        assertThat(searchResponse.getHits().getAt(2).getScore(), equalTo(
                valueOf("0.1")));


    }

}
