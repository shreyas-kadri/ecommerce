#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
    CREATE DATABASE "userDB";
    CREATE DATABASE "inventoryDB";
    CREATE DATABASE "cartDB";
    CREATE DATABASE "orderDB";
EOSQL
