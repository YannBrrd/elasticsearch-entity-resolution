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
}'
echo
curl -s "localhost:9200/test/city/_search?pretty=true" -d '{
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
}'