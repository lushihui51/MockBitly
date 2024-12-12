#!/bin/bash

ROOT="$HOME/a1group10"
SHORTENER_DIR="$HOME/a1group10/URLShortenerServer"
PROXY_DIR="$HOME/a1group10/proxyServer"
DATABASE_DIR="/virtual/$USER"

cd $ROOT
nohup java MonitoringSystem $(cat nodes.txt) &

cd $SHORTENER_DIR
mkdir -p $DATABASE_DIR
rm -f $DATABASE_DIR/urlshortener.db
sqlite3 $DATABASE_DIR/urlshortener.db <schema.sql
nohup java -cp ".:$ROOT:sqlite-jdbc-3.39.3.0.jar" URLShortener &

cd $PROXY_DIR
nohup java -cp ".:$ROOT" proxyServer $(cat ~/a1group10/nodes.txt) &
