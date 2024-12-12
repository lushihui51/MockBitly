#!/bin/bash

# Define the directory where the log files are stored
log_dir=~/a1group10/logs/status

# Check if the directory exists
if [ -d "$log_dir" ]; then
  for log_file in "$log_dir"/*.log; do
    if [ -f "$log_file" ]; then
      echo ""
      echo "---------- $(basename "$log_file" .log) ----------"
      cat "$log_file"
      echo "--------------------------------"
      echo ""
    else
      echo "No log files found in $log_dir"
    fi
  done
else
  echo "Directory $log_dir does not exist."
fi
