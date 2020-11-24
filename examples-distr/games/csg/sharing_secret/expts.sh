#!/bin/bash
for i in `seq 1 12`;
do
        ../../../../../bin/prism bar_csg.prism -const rmax=$i -pf "<<s>>R{\"r0\"}max=?[C]"  >> "./expts.txt"
done

