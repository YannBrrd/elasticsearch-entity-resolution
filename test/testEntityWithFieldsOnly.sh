sh testInit.sh
echo
curl -s "localhost:9200/test/city/_search?pretty=true" -d '{
  "size": 4,
  "query": {
    "function_score": {
      "query": {
        "match_all": {}
      },
      "script_score" :{
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
  }                           }
}'

