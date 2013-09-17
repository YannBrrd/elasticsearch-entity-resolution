curl -XDELETE "http://localhost:9200/test"
echo
curl -XPUT "http://localhost:9200/test/" -d '{
    "settings": {
        "index.number_of_shards": 1,
        "index.number_of_replicas": 0
    },
    "mappings": {
        "city": {
            "properties": {
                "city": {
                    "type": "string"
                },
                "state": {
                    "type": "string",
                    "index": "not_analyzed"
                },
                "population": {
                    "type": "integer"
                }
            }
        }
    }
}'
echo

curl -XPUT "localhost:9200/test/city/1" -d '{"city": "Cambridge", "state": "MA", "population": 105162}'
curl -XPUT "localhost:9200/test/city/2" -d '{"city": "South Burlington", "state": "VT", "population": 17904}'
curl -XPUT "localhost:9200/test/city/3" -d '{"city": "South Portland", "state": "ME", "population": 25002}'
curl -XPUT "localhost:9200/test/city/4" -d '{"city": "Essex", "state": "VT", "population": 19587}'
curl -XPUT "localhost:9200/test/city/5" -d '{"city": "Portland", "state": "ME", "population": 66194}'
curl -XPUT "localhost:9200/test/city/6" -d '{"city": "Burlington", "state": "VT", "population": 42417}'
curl -XPUT "localhost:9200/test/city/7" -d '{"city": "Stamford", "state": "CT", "population": 122643}'
curl -XPUT "localhost:9200/test/city/8" -d '{"city": "Colchester", "state": "VT", "population": 17067}'
curl -XPUT "localhost:9200/test/city/9" -d '{"city": "Concord", "state": "NH", "population": 42695}'
curl -XPUT "localhost:9200/test/city/10" -d '{"city": "Boston", "state": "MA", "population": 617594}'

curl -XPOST "http://localhost:9200/test/_refresh"
echo
curl -s "localhost:9200/test/_search?pretty=true" -d '{
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
}'

