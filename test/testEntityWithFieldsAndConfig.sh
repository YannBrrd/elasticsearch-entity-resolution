#!/bin/sh
./testInit.sh
echo
curl -XPUT "http://localhost:9200/entity/" -d '{
  "settings": {
    "index.number_of_shards": 1,
    "index.number_of_replicas": 0
  },
  "mappings": {
    "entity-configuration": {
      "properties": {
        "name": {
          "type": "string",
          "index": "not_analyzed"
        },
        "comparator": {
          "type": "string",
          "index": "not_analyzed"
        },
        "low": {
          "type": "double"
        },
        "high": {
          "type": "double"
        },
        "cleaners": {
            "type": "string",
            "index" : "not_analyzed"
        }
      }
    }
  }
}'
echo
curl -XPUT "localhost:9200/entity/entity-configuration/test" -d '{
   "entity" : {
     "fields" : [ {
       "field" : "city",
       "cleaners" : [ {
         "name" : "no.priv.garshol.duke.cleaners.TrimCleaner"
       }, {
         "name" : "no.priv.garshol.duke.cleaners.LowerCaseNormalizeCleaner"
       } ],
       "high" : 0.95,
       "comparator" : {
         "name" : "no.priv.garshol.duke.comparators.JaroWinkler"
       },
       "low" : 0.1
     }, {
       "field" : "state",
       "cleaners" : [ {
         "name" : "no.priv.garshol.duke.cleaners.LowerCaseNormalizeCleaner"
       } ],
       "high" : 0.95,
       "comparator" : {
         "name" : "no.priv.garshol.duke.comparators.JaroWinkler"
       },
       "low" : 0.1
     }, {
       "field" : "population",
       "cleaners" : [ {
         "name" : "no.priv.garshol.duke.cleaners.DigitsOnlyCleaner"
       } ],
       "high" : 0.95,
       "comparator" : {
         "name" : "no.priv.garshol.duke.comparators.NumericComparator"
       },
       "low" : 0.1
     }, {
       "field" : "position",
       "cleaners" : [ {
         "name" : "no.priv.garshol.duke.cleaners.LowerCaseNormalizeCleaner"
       } ],
       "high" : 0.95,
       "comparator" : {
         "name" : "no.priv.garshol.duke.comparators.GeopositionComparator",
         "params" : {
           "max-distance" : "100"
         }
       },
       "low" : 0.1
     } ]
   }
 }'
echo
curl -s "localhost:9200/test/city/_search?pretty=true" -d '{
  "size": 4,
  "min_score" : 0.7,
  "query": {
    "function_score": {
      "boost_mode": "replace",
      "query": {
        "match_all": {
          
        }
      },
      "script_score": {
        "script": "entity-resolution",
        "lang": "native",
        "params": {
          "entity": {
            "configuration": {
              "index": "entity",
              "type": "entity-configuration",
              "name": "test"
            },
            "fields": [
              {
                "field": "city",
                "value": "South"
              },
              {
                "field": "state",
                "value": "ME"
              },
              {
                "field": "population",
                "value": "26000"
              },
              {
                "field": "position",
                "value": "43,70"
              }
            ]
          }
        }
      }    
    }
  }
}'
