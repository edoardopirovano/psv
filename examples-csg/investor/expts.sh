#!/bin/bash
for i in `seq 1 12`;
do
        ../../../../../bin/prism $1 -const nrounds=$i  >> "$2/build_$3.txt"
done

