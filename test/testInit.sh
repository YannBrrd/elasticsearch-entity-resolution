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