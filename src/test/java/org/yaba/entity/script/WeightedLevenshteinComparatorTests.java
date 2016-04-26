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

public class WeightedLevenshteinComparatorTests extends AbstractSearchScriptTestCase {

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
    public static final String ADDRESS = "address";
    public static final String ZIP = "zip";
    public static final String EMAIL = "email";
    public static final String PERSON = "person";
    public static final String PARAMS = "params";
    public static final String OBJECT = "object";
    

    @Test
    public final void testEntity() throws IOException, ExecutionException, InterruptedException {
        
        String testMapping = 
                jsonBuilder()
                    .startObject()
                    .startObject(PERSON)
                    .startObject(PROPERTIES)
                    .startObject(NAME)
                    .field(TYPE,STRING)
                    .field(INDEX,NOT_ANALYZED)
                    .endObject()
                    .startObject(ADDRESS)
                    .field(TYPE,STRING)
                    .field(INDEX,NOT_ANALYZED)
                    .endObject()
                    .startObject(ZIP)
                    .field(TYPE,INTEGER)
                    .field(INDEX,NOT_ANALYZED)
                    .endObject()
                    .startObject(EMAIL)
                    .field(TYPE,STRING)
                    .endObject()
                    .endObject()
                    .endObject()
                    .endObject()
                    .string();
 System.out.println(testMapping);
        assertAcked(prepareCreate(TEST).addMapping(PERSON, testMapping));
        
        List<IndexRequestBuilder> indexBuilders = new ArrayList<>();
        
        indexBuilders.add(client()
                .prepareIndex(TEST,PERSON,"1")
                .setSource(NAME,"J. Random Hacker",ADDRESS,"Main St 101", ZIP,"21231"));
        indexBuilders.add(client()
                .prepareIndex(TEST,PERSON,"2")
                .setSource(NAME,"John Random Hacker",ADDRESS,"Mian Street 101",ZIP,"21231",EMAIL,"hack@gmail.com"));
        indexBuilders.add(client()
                .prepareIndex(TEST,PERSON,"3")
                .setSource(NAME,"Jacob Hacker",ADDRESS,"Main Street 201",ZIP,"38122",EMAIL,"jacob@hotmail.com"));
        indexBuilders.add(client()
                .prepareIndex(TEST,PERSON,"4")
                .setSource(NAME,"J Random Hacker",ADDRESS,"Main St 101",ZIP,"21231"));
        indexRandom(true, indexBuilders);

        // Script parameters
        Map<String, Object> params =
                MapBuilder.<String, Object>newMapBuilder().map();

        ArrayList<Map<String, Object>> fields;
        fields = new ArrayList<>();
        
        Map<String, Object> aField =
                MapBuilder
                        .<String, Object>newMapBuilder()
                        .put(FIELD, ADDRESS)
                        .put(VALUE, "Main Street 101")
                        .put(COMPARATOR, MapBuilder.<String, Object>newMapBuilder()
                                .put(NAME, "no.priv.garshol.duke.comparators.WeightedLevenshtein")
                                .put(PARAMS, MapBuilder.<String,Object>newMapBuilder()
                                        .put("digit-weight", "4.0")
                                        .put("letter-weight", "1.3")
                                        .put("punctuation-weight", "10.3")
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
        
//        aField =
//                MapBuilder
//                        .<String, Object>newMapBuilder()
//                        .put(FIELD, NAME)
//                        .put(VALUE, "Random")
//                        .put(COMPARATOR, MapBuilder.<String, Object>newMapBuilder()
//                                .put(NAME, "no.priv.garshol.duke.comparators.QGramComparator")
//                                .map())
//
//                        .put(LOW, 0.35)
//                        .put(HIGH, 0.88)
//                        .put(CLEANERS, new Map[]{
//                                MapBuilder.<String, Object>newMapBuilder()
//                                        .put(NAME, "no.priv.garshol.duke.cleaners.LowerCaseNormalizeCleaner")
//                                        .map()})
//                        .map();
//
//        fields.add(aField);
//        
//        aField = 
//                MapBuilder
//                        .<String,Object>newMapBuilder()
//                        .put(FIELD, ADDRESS)
//                        .put(COMPARATOR, MapBuilder.<String,Object>newMapBuilder()
//                                .put(NAME,"no.priv.garshol.duke.comparators.ExactComparator")
//                                .map())
//                        .put(LOW, 0.4)
//                        .put(HIGH, 0.8)
//                        .map();
//        fields.add(aField);
        
        params.put(
                "entity",
                new MapBuilder<String, ArrayList<Map<String, Object>>>().put(
                        "fields", fields).map());

        // Find all objects
        SearchRequestBuilder request =
                client()
                        .prepareSearch(TEST)
                        .setTypes(PERSON)
                        .setQuery(
                                functionScoreQuery(
                                        (matchAllQuery()))
                                        .boostMode(CombineFunction.REPLACE.getName())
                                        .scoreMode("max")
                                        .add(ScoreFunctionBuilders.scriptFunction(new Script(EntityResolutionScript.SCRIPT_NAME, ScriptService.ScriptType.INLINE, "native", params))))
                        .setSize(4);



        logger.info("\n" + request.toString());


        SearchResponse searchResponse = request.execute().actionGet();

        assertThat(Arrays.toString(searchResponse.getShardFailures()),
                searchResponse.getFailedShards(), equalTo(0));

        logger.info(searchResponse.toString());
        
        assertThat(searchResponse.getHits().getAt(0).getSource().get(ADDRESS).toString(), equalTo("Mian Street 101"));
        assertThat(searchResponse.getHits().getAt(0).getScore(),equalTo(valueOf("0.80752")));

        assertThat(searchResponse.getHits().getAt(1).getSource().get(NAME).toString(),
                equalTo("Jacob Hacker"));
        assertThat(searchResponse.getHits().getAt(1).getScore(),
                equalTo(valueOf("0.742")));
        
        assertThat(searchResponse.getHits().getAt(2).getScore(),
                equalTo(valueOf("0.62510747")));
        assertThat(searchResponse.getHits().getAt(3).getScore(),
                equalTo(valueOf("0.62510747")));
    }
}