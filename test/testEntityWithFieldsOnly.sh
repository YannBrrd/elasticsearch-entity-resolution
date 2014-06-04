#!/bin/sh
sh testInit.sh
echo
curl -s "localhost:9200/test/city/_search?pretty=true" -d '{
 "size" : 4,
 "query" : {
   "custom_score" : {
     "query" : {
       "match_all" : { }
     },
     "script" : "entity-resolution",
     "lang" : "native",
     "params" : {
       "entity" : {
         "fields" : [ {
           "field" : "city",
           "value" : "South",
           "cleaners" : [ "no.priv.garshol.duke.cleaners.TrimCleaner", "no.priv.garshol.duke.cleaners.LowerCaseNormalizeCleaner" ],
           "high" : 0.95,
           "comparator" : "no.priv.garshol.duke.comparators.JaroWinkler",
           "low" : 0.1
         }, {
           "field" : "state",
           "value" : "ME",
           "cleaners" : [ "no.priv.garshol.duke.cleaners.LowerCaseNormalizeCleaner" ],
           "high" : 0.95,
           "comparator" : "no.priv.garshol.duke.comparators.JaroWinkler",
           "low" : 0.1
         }, {
           "field" : "population",
           "value" : "26000",
           "cleaners" : [ "no.priv.garshol.duke.cleaners.DigitsOnlyCleaner" ],
           "high" : 0.95,
           "comparator" : "no.priv.garshol.duke.comparators.NumericComparator",
           "low" : 0.1
         }, {
           "field" : "position",
           "value" : "43,70",
           "cleaners" : [ "no.priv.garshol.duke.cleaners.LowerCaseNormalizeCleaner" ],
           "high" : 0.95,
           "comparator" : "no.priv.garshol.duke.comparators.GeopositionComparator",
           "low" : 0.1
         } ]
       }
     }
   }
 }
}'

