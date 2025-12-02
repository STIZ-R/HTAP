#!/bin/sh
set -e

echo "Waiting for PostgreSQL at $POSTGRES_HOST:$POSTGRES_PORT..."

while ! nc -z $POSTGRES_HOST $POSTGRES_PORT; do
  echo "PostgreSQL not ready, sleeping 1s..."
  sleep 1
done

echo "PostgreSQL is up! Starting proxy..."
exec java -jar /app/proxy.jar
