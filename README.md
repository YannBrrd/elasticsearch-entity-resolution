entity-resolution
===================

This project aims to build an interactive entity resolution service based on both [Duke](http://code.google.com/p/duke) and [Elasticsearch](http://www.elasticsearch.org)... 

## Request
 ```
{
  "query": {
    "custom_score": {
      "query": {
        "match": {
          "name": "foo"
        }
      },
      "script": "entity-resolution",
      "lang": "native",
      "params": {
        "entity": [
            {
                "field" : "name",
                "value" : "Arthur",
                "cleaners" : ["asciifolding","lowercase"],
                "comparator" : "no.priv.garshol.duke.comparators.PersonNameComparator",
                "low" : 0.5,
                "high" : 0.95
            },
            {
                "field" : "surname",
                "value" : "Raimbault",
                "cleaners" : ["asciifolding"],
                "comparator" : "no.priv.garshol.duke.comparators.Levenshtein",
                "low" : 0.5,
                "high" : 0.95
            }            
        ]
      }
    }
  }
}
```

## Licence 

This project is licended under LGPLv3
