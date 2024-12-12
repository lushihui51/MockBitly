#!/bin/bash

PROXY_PORT=8080
URLSHORTENER_PORT=3000
MONITORING_PORT=3001
STARTING_LOGS_DIR="$HOME/a1group10/logs/starting"
WRITES_LOGS_DIR="$HOME/a1group10/logs/writes"
ENDING_LOGS_DIR="$HOME/a1group10/logs/ending"

restart='false'
while getopts 'r' flag; do
    case "${flag}" in
    r) restart='true' ;;
    esac
done

for host in $(cat nodes.txt); do
    echo "Ending session on $host"
    ssh $host "kill \$(lsof -i :$PROXY_PORT | awk 'NR==2 {print \$2}'); 
    kill \$(lsof -i :$URLSHORTENER_PORT | awk 'NR==2 {print \$2}'); 
    kill \$(lsof -i :$MONITORING_PORT | awk 'NR==2 {print \$2}');" >"$ENDING_LOGS_DIR/$host.log" 2>&1 &
done

wait $(jobs -p)

rm -f $STARTING_LOGS_DIR/*
if [ $restart = 'false' ]; then
    rm -f $WRITES_LOGS_DIR/*
fi
