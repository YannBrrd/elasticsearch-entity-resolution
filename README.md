[![Build Status](https://travis-ci.org/YannBrrd/elasticsearch-entity-resolution.png)](http://travis-ci.org/YannBrrd/elasticsearch-entity-resolution)



elasticsearch-entity-resolution
===================

This project is an interactive entity resolution plugin for [Elasticsearch](http://www.elasticsearch.org) based on [Duke](http://code.google.com/p/duke). Basically, it uses [Bayesian probabilities] (http://en.wikipedia.org/wiki/Bayesian_probability) to compute probability. You can pretty much use it an interactive deduplication engine.

To understand basics, go to [Duke project documentation](http://code.google.com/p/duke/wiki/XMLConfig).

A list of [available comparators] (http://code.google.com/p/duke/wiki/Comparator) is available [here](http://code.google.com/p/duke/wiki/Comparator).

## Install

Download at [Bintray](http://dl.bintray.com/yann-barraud/elasticsearch-entity-resolution)

``` 
$ plugin -i entity-resolution -url http://dl.bintray.com/yann-barraud/elasticsearch-entity-resolution/org/yaba/elasticsearch-entity-resolution-plugin/0.3/#elasticsearch-entity-resolution-plugin-0.3.zip
```

## Request

### Tuning mode 

This mode allows you to parametrise the plugin for each request you fire. It is comfortable to tune your comparison parameters. Once tuning is done, you can switch (if you wish) to indus-mode.

#### Request
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

### indus-mode

Once you are certain of your script parametrization, it is quite comfortable to store it in Elasticsearch.

#### Parametrization store exemple

##### Mapping 
```javascript
{
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
}
```

##### Parameters
```javascript
{
  "entity": {
    "fields": [
      {
        "field": "city",
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
        "cleaners": [
          "no.priv.garshol.duke.cleaners.LowerCaseNormalizeCleaner"
        ],
        "comparator": "no.priv.garshol.duke.comparators.Levenshtein",
        "low": 0.1,
        "high": 0.95
      },
      {
        "field": "population",
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
```

##### Request
```javascript
{
  "size": 4,
  "query": {
    "custom_score": {
      "query": {
        "match_all": {
          
        }
      },
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
            }
          ]
        }
      }
    }
  }
}
```

## Parametrisation
### fields

List of fields to compare, and parametrisation. Should always be an array.
* ```field``` is the name of the field to compare to.
* ```value``` is the value of the field to compare.
* ```cleaners``` is the list of data cleaners to apply. Should always be an array. Should always be full qualified class name.
* ```comparator``` is the full qualified class name of the comparator to use. Note : you can implement your own, and put it in the claspath. It should work (not tested yet).
* ```low``` is the lowest probability for this field (if the probability is inferior, this one will be used).
* ```high``` is the highest probability for this field (if the probability is superior, this one will be used).

### Threshold
Threshold can be set using ```min_score``` as described in [Elasticsearch documentation](http://www.elasticsearch.org/guide/reference/api/search/min-score/).

## Run examples

### Without threshold

#### Request

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

#### Response

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

#### With threshold && using stored configuration

#### Request

```javascript
{
  "size": 4,
  "min_score" : 0.7,
  "query": {
    "custom_score": {
      "query": {
        "match_all": {
          
        }
      },
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
            }
          ]
        }
      }
    }
  }
}
```

#### Response
```javascript
{
  "took" : 2,
  "timed_out" : false,
  "_shards" : {
    "total" : 5,
    "successful" : 5,
    "failed" : 0
  },
  "hits" : {
    "total" : 1,
    "max_score" : 0.95843065,
    "hits" : [ {
      "_index" : "test",
      "_type" : "city",
      "_id" : "3",
      "_score" : 0.95843065, "_source" : {"city":"South Portland","state":"ME","population":25002}
    } ]
  }
}
```


## Licence 

This project is licended under LGPLv3

Copyright (c) 2013 Yann Barraud
