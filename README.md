entity-resolution
===================

This project is an interactive entity resolution plugin for [Elasticsearch](http://www.elasticsearch.org) based on [Duke](http://code.google.com/p/duke). Basically, it uses [Bayesian probabilities] (http://en.wikipedia.org/wiki/Bayesian_probability) to compute probability. You can pretty much use it an interactive deduplication engine.

It is usable as is, though ```cleaners``` are not yet implemented.

To understand basics, go to [Duke project documentation](http://code.google.com/p/duke/wiki/XMLConfig).

A list of [available comparators] (http://code.google.com/p/duke/wiki/Comparator) is available [here](http://code.google.com/p/duke/wiki/Comparator).

## Install

``` 
$ plugin -i entity-resolution -url 	http://goo.gl/LMF3wK
```

## Request
 ```javascript
{
  "query": {
    "custom_score": {
      "query": {
        "match_all": {}
      },
      "script": "entity-resolution",
      "lang": "native",
      "params": {
        "entity": {
          "fields": [
            {
              "field": "city",
              "value": "South",
              "cleaners": [
                "no.priv.garshol.duke.cleaners.TrimCleaner",
                "no.priv.garshol.duke.cleaners.LowerCaseNormalizeCleaner"
              ],
              "comparator": "no.priv.garshol.duke.comparators.Levenshtein",
              "low": 0.1,
              "high": 0.95
            },
            {
              "field": "state",
              "value": "ME",
              "cleaners": [
                "no.priv.garshol.duke.cleaners.LowerCaseNormalizeCleaner"
              ],
              "comparator": "no.priv.garshol.duke.comparators.Levenshtein",
              "low": 0.1,
              "high": 0.95
            },
            {
              "field": "population",
              "value": "25002",
              "cleaners": ["no.priv.garshol.duke.cleaners.DigitsOnlyCleaner"],
              "comparator": "no.priv.garshol.duke.comparators.NumericComparator",
              "low": 0.1,
              "high": 0.95
            }
          ]
        }
      }
    }
  }
}
```

## Parametrization
### fields

List of fields to compare, and parametrization. Should always be an array.
* ```field``` is the name of the field to compare to.
* ```value``` is the value of the field to compare.
* ```cleaners``` is the list of data cleaners to apply. Should always be an array. Should always be full qualified class name.
* ```comparator``` is the full qualified class name of the comparator to use. Note : you can implement your own, and put it in the claspath. It should work (not tested yet).
* ```low``` is the lowest probability for this field (if the probability is inferior, this one will be used).
* ```high``` is the highest probability for this field (if the probability is superior, this one will be used).

## Run example

### Request

```javascript
{
  "size": 4,
  "query": {
    "custom_score": {
      "query": {
        "match_all": {}
      },
      "script": "entity-resolution",
      "lang": "native",
      "params": {
        "entity": {
          "fields": [
            {
              "field": "city",
              "value": "South",
              "cleaners": [
                "no.priv.garshol.duke.cleaners.TrimCleaner",
                "no.priv.garshol.duke.cleaners.LowerCaseNormalizeCleaner"
              ],
              "comparator": "no.priv.garshol.duke.comparators.Levenshtein",
              "low": 0.1,
              "high": 0.95
            },
            {
              "field": "state",
              "value": "ME",
              "cleaners": [
                "no.priv.garshol.duke.cleaners.LowerCaseNormalizeCleaner"
              ],
              "comparator": "no.priv.garshol.duke.comparators.Levenshtein",
              "low": 0.1,
              "high": 0.95
            },
            {
              "field": "population",
              "value": "26000",
              "cleaners": [
                "no.priv.garshol.duke.cleaners.DigitsOnlyCleaner"
              ],
              "comparator": "no.priv.garshol.duke.comparators.NumericComparator",
              "low": 0.1,
              "high": 0.95
            }
          ]
        }
      }
    }
  }
}
```

### Response

```javascript
{
  "took" : 29,
  "timed_out" : false,
  "_shards" : {
    "total" : 1,
    "successful" : 1,
    "failed" : 0
  },
  "hits" : {
    "total" : 16,
    "max_score" : 0.95843065,
    "hits" : [ {
      "_index" : "test",
      "_type" : "city",
      "_id" : "3",
      "_score" : 0.95843065, "_source" : {"city": "South Portland", "state": "ME", "population": 25002}
    }, {
      "_index" : "test",
      "_type" : "city",
      "_id" : "5",
      "_score" : 0.19, "_source" : {"city": "Portland", "state": "ME", "population": 66194}
    }, {
      "_index" : "test",
      "_type" : "city",
      "_id" : "4",
      "_score" : 0.03672479, "_source" : {"city": "Essex", "state": "VT", "population": 19587}
    }, {
      "_index" : "test",
      "_type" : "city",
      "_id" : "2",
      "_score" : 0.029812464, "_source" : {"city": "South Burlington", "state": "VT", "population": 17904}
    } ]
  }
}
```
## Licence 

This project is licended under LGPLv3

Copyright (c) 2013 Yann Barraud