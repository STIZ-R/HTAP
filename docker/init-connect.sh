#!/bin/bash

until curl -s http://htap-connect:8083/connectors >/dev/null 2>&1; do
    echo "En attente de Kafka Connect..."
    sleep 5
done

if curl -s http://htap-connect:8083/connectors | grep -q "debezium-connector"; then
    echo "Connecteur déjà créé"
else
    echo "Création du connecteur..."
    curl -X POST -H "Content-Type: application/json" \
         --data @/kafka/connectors/debezium-connector.json \
         http://htap-connect:8083/connectors
fi
