#!/bin/bash

TOPIC=$1
SCHEMA_ID=$2
DATA_FILE_PATH=$3

kafka-avro-console-producer --broker-list broker:9092 --topic "${TOPIC}" --property schema.registry.url=http://schemaregistry:8081 --property value.schema.id="${SCHEMA_ID}" < "${DATA_FILE_PATH}"
