#!/bin/bash
set -e

DATABASES="userdb accountdb transactiondb budgetdb categorizationdb notificationdb insightsdb"

for db in $DATABASES; do
  echo "Creating database: $db"
  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
    CREATE DATABASE $db;
EOSQL
done