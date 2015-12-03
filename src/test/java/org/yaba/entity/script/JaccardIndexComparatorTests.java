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

import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.collect.MapBuilder;
import org.elasticsearch.common.lucene.search.function.CombineFunction;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptService;
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

public class JaccardIndexComparatorTests extends AbstractSearchScriptTests {


    public static final String CITY = "city";
    public static final String PROPERTIES = "properties";
    public static final String TYPE = "type";
    public static final String STRING = "string";
    public static final String STATE = "state";
    public static final String INDEX = "index";
    public static final String NOT_ANALYZED = "not_analyzed";
    public static final String POPULATION = "population";
    public static final String INTEGER = "integer";
    public static final String POSITION = "position";
    public static final String GEO_POINT = "geo_point";
    public static final String TEST = "test";
    public static final String FIELD = "field";
    public static final String VALUE = "value";
    public static final String COMPARATOR = "comparator";
    public static final String NAME = "name";
    public static final String LOW = "low";
    public static final String HIGH = "high";
    public static final String CLEANERS = "cleaners";

    @Test
    public final void testEntity() throws IOException, ExecutionException, InterruptedException {


        // Create a new test index
        String testMapping =
                jsonBuilder()
                        .startObject()
                        .startObject(CITY)
                        .startObject(PROPERTIES)
                        .startObject(CITY)
                        .field(TYPE, STRING)
                        .endObject()
                        .startObject(STATE)
                        .field(TYPE, STRING)
                        .field(INDEX, NOT_ANALYZED)
                        .endObject()
                        .startObject(POPULATION)
                        .field(TYPE, INTEGER)
                        .endObject()
                        .startObject(POSITION)
                        .field(TYPE, GEO_POINT)
                        .endObject()
                        .endObject()
                        .endObject()
                        .endObject()
                        .string();

        assertAcked(prepareCreate(TEST).addMapping(CITY, testMapping));

        List<IndexRequestBuilder> indexBuilders = new ArrayList<>();

        // Index main records
        indexBuilders.add(client()
                .prepareIndex(TEST, CITY, "1")
                .setSource(CITY, "Cambridge", STATE, "MA", POPULATION,
                        105162, POSITION, "42.373746,71.110554"));
        indexBuilders.add(client()
                .prepareIndex(TEST, CITY, "2")
                .setSource(CITY, "South Burlington", STATE, "VT",
                        POPULATION, 17904, POSITION, "44.451846,73.181710"));
        indexBuilders.add(client()
                .prepareIndex(TEST, CITY, "3")
                .setSource(CITY, "South Portland", STATE, "ME",
                        POPULATION, 25002, POSITION, "43.631549,70.272724"));
        indexBuilders.add(client().prepareIndex(TEST, CITY, "4")
                        .setSource(CITY, "Essex", STATE, "VT", POPULATION, 19587, POSITION, "44.492905,73.108601")
        );
        indexBuilders.add(client()
                .prepareIndex(TEST, CITY, "5")
                .setSource(CITY, "Portland", STATE, "ME", POPULATION,
                        66194, POSITION, "43.665116,70.269086"));
        indexBuilders.add(client()
                .prepareIndex(TEST, CITY, "6")
                .setSource(CITY, "Burlington", STATE, "VT", POPULATION,
                        42417, POSITION, "44.484748,73.223157"));
        indexBuilders.add(client()
                .prepareIndex(TEST, CITY, "7")
                .setSource(CITY, "Stamford", STATE, "CT", POPULATION,
                        122643, POSITION, "41.074448,73.541316"));
        indexBuilders.add(client()
                .prepareIndex(TEST, CITY, "8")
                .setSource(CITY, "Colchester", STATE, "VT", POPULATION,
                        17067, POSITION, "44.3231,73.148"));
        indexBuilders.add(client()
                .prepareIndex(TEST, CITY, "9")
                .setSource(CITY, "Concord", STATE, "NH", POPULATION,
                        42695, POSITION, "43.220093,71.549127"));
        indexBuilders.add(client()
                .prepareIndex(TEST, CITY, "10")
                .setSource(CITY, "Boston", STATE, "MA", POPULATION,
                        617594, POSITION, "42.321597,71.089115"));

        indexRandom(true, indexBuilders);

        // Script parameters
        Map<String, Object> params =
                MapBuilder.<String, Object>newMapBuilder().map();

        ArrayList<Map<String, Object>> fields;
        fields = new ArrayList<>();

        Map<String, Object> aField =
                MapBuilder
                        .<String, Object>newMapBuilder()
                        .put(FIELD, CITY)
                        .put(VALUE, "South")
                        .put(COMPARATOR, MapBuilder.<String, Object>newMapBuilder()
                                .put(NAME, "no.priv.garshol.duke.comparators.JaccardIndexComparator")
                                .put("objects", MapBuilder.<String, Object>newMapBuilder()
                                        .put("object", MapBuilder.<String, Object>newMapBuilder()
                                                .put("class", "no.priv.garshol.duke.comparators.Levenshtein")
                                                .put("name", "comparator")
                                                .map())
                                        .map())
                                .map())
                        .put(LOW, 0.1)
                        .put(HIGH, 0.95)
                        .put(CLEANERS, new Map[]{
                                MapBuilder.<String, Object>newMapBuilder()
                                        .put(NAME, "no.priv.garshol.duke.cleaners.TrimCleaner")
                                        .map(),
                                MapBuilder.<String, Object>newMapBuilder()
                                        .put(NAME, "no.priv.garshol.duke.cleaners.LowerCaseNormalizeCleaner")
                                        .map()})
                        .map();

        fields.add(aField);

        aField =
                MapBuilder
                        .<String, Object>newMapBuilder()
                        .put(FIELD, STATE)
                        .put(VALUE, "ME")
                        .put(COMPARATOR, MapBuilder.<String, Object>newMapBuilder()
                                .put(NAME, "no.priv.garshol.duke.comparators.JaroWinkler")
                                .map())

                        .put(LOW, 0.1)
                        .put(HIGH, 0.95)
                        .put(CLEANERS, new Map[]{
                                MapBuilder.<String, Object>newMapBuilder()
                                        .put(NAME, "no.priv.garshol.duke.cleaners.LowerCaseNormalizeCleaner")
                                        .map()})
                        .map();

        fields.add(aField);

        aField =
                MapBuilder
                        .<String, Object>newMapBuilder()
                        .put(FIELD, POPULATION)
                        .put(VALUE, "26000")
                        .put(COMPARATOR, MapBuilder.<String, Object>newMapBuilder()
                                .put(NAME, "no.priv.garshol.duke.comparators.NumericComparator")
                                .map())
                        .put(LOW, 0.1)
                        .put(HIGH, 0.95)
                        .put(CLEANERS, new Map[]{
                                MapBuilder.<String, Object>newMapBuilder()
                                        .put(NAME, "no.priv.garshol.duke.cleaners.DigitsOnlyCleaner")
                                        .map()})
                        .map();

        fields.add(aField);

        aField =
                MapBuilder
                        .<String, Object>newMapBuilder()
                        .put(FIELD, POSITION)
                        .put(VALUE, "43,70")
                        .put(COMPARATOR, MapBuilder.<String, Object>newMapBuilder()
                                .put(NAME, "no.priv.garshol.duke.comparators.GeopositionComparator")
                                .put("params", MapBuilder.<String, Object>newMapBuilder()
                                                .put("max-distance", "100").map()
                                ).map())
                        .put(LOW, 0.1)
                        .put(HIGH, 0.95)
                        .put(CLEANERS, new Map[]{
                                MapBuilder.<String, Object>newMapBuilder()
                                        .put(NAME, "no.priv.garshol.duke.cleaners.LowerCaseNormalizeCleaner")
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
                        .prepareSearch(TEST)
                        .setTypes(CITY)
                        .setQuery(
                                functionScoreQuery(
                                        (matchAllQuery()))
                                        .boostMode(CombineFunction.REPLACE)
                                        .scoreMode("max")
                                        .add(ScoreFunctionBuilders.scriptFunction(new Script(EntityResolutionScript.SCRIPT_NAME, ScriptService.ScriptType.INLINE, "native", params))))
                        .setSize(4);

        logger.info(request.toString());

        SearchResponse searchResponse = request.execute().actionGet();

        assertThat(Arrays.toString(searchResponse.getShardFailures()),
                searchResponse.getFailedShards(), equalTo(0));

        logger.info(searchResponse.toString());

        assertThat(searchResponse.getHits().getAt(0).getSource().get(CITY)
                .toString(), equalTo("South Portland"));
        assertThat(searchResponse.getHits().getAt(0).getScore(), equalTo(
                valueOf("0.7192429")));

        assertThat(searchResponse.getHits().getAt(1).getSource().get(CITY)
                .toString(), equalTo("Portland"));
        assertThat(searchResponse.getHits().getAt(1).getScore(), equalTo(
                valueOf("0.025401069")));

        assertThat(searchResponse.getHits().getAt(2).getSource().get(CITY)
                .toString(), equalTo("Essex"));
        assertThat(searchResponse.getHits().getAt(2).getScore(), equalTo(
                valueOf("0.0042182333")));


        /*
        TODO : Need to check and fix this test

        assertThat(searchResponse.getHits().getAt(3).getSource().get(CITY)

                .toString(), equalTo("Boston"));
        assertThat(searchResponse.getHits().getAt(3).getScore(), equalTo(
                valueOf("0.0035236408"))); */

    }

}
