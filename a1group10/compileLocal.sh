#!/bin/bash

ROOT="$HOME/a1group10"
SHORTENER_DIR="$HOME/a1group10/URLShortenerServer"
PROXY_DIR="$HOME/a1group10/proxyServer"
DATABASE_DIR="/virtual/$USER"

cd $ROOT
javac MonitoringSystem.java

cd $SHORTENER_DIR
javac -cp ".:$ROOT" URLShortener.java

cd $PROXY_DIR
javac -cp ".:$ROOT" proxyServer.java
