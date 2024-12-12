#!/bin/bash

javac GetParent.java
javac Birth.java
sleep 1

# do the below in java
while IFS=$(read -r line); do
    if [[ -z "$line" ]]; then
        continue
    fi
    parent=$(java GetParent "$line" $(cat nodes.txt))
    echo $parent
    cp logs/writes/"$parent".log logs/writes/"$line".log
done <new.txt

cat new.txt >>nodes.txt
truncate -s 0 new.txt
./end.sh -r
./start.sh
java Birth $(cat new.txt)
