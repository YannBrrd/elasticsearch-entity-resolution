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

curl -XPUT "localhost:9200/test/city/1" -d '{"city": "Cambridge", "state": "MA", "population": 105162, "position": "42.373746,71.110554"}'
curl -XPUT "localhost:9200/test/city/2" -d '{"city": "South Burlington", "state": "VT", "population": 17904, "position": "44.451846,73.181710"}'
curl -XPUT "localhost:9200/test/city/3" -d '{"city": "South Portland", "state": "ME", "population": 25002, "position": "43.631549,70.272724"}'
curl -XPUT "localhost:9200/test/city/4" -d '{"city": "Essex", "state": "VT", "population": 19587, "position": "44.492905,73.108601"}'
curl -XPUT "localhost:9200/test/city/5" -d '{"city": "Portland", "state": "ME", "population": 66194, "position": "43.665116,70.269086"}'
curl -XPUT "localhost:9200/test/city/6" -d '{"city": "Burlington", "state": "VT", "population": 42417, "position": "44.484748,73.223157"}'
curl -XPUT "localhost:9200/test/city/7" -d '{"city": "Stamford", "state": "CT", "population": 122643, "position":"41.074448,73.541316"}'
curl -XPUT "localhost:9200/test/city/8" -d '{"city": "Colchester", "state": "VT", "population": 17067, "position": "44.3231,73.148"}'
curl -XPUT "localhost:9200/test/city/9" -d '{"city": "Concord", "state": "NH", "population": 42695, "position": "43.220093,71.549127"}'
curl -XPUT "localhost:9200/test/city/10" -d '{"city": "Boston", "state": "MA", "population": 617594, "position": "42.321597,71.089115"}'

curl -XPOST "http://localhost:9200/test/_refresh"
echo