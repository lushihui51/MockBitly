#!/bin/bash
ENDING_LOGS_DIR="$HOME/a1group10/logs/ending"

./compileLocal.sh
sleep 1
for host in $(cat nodes.txt); do
    echo "Starting session on $host"
    ssh $host "~/a1group10/runLocal.sh" >"./logs/starting/$host.log" 2>&1 &
done

rm -f $ENDING_LOGS_DIR/*

echo "Started sessions"
