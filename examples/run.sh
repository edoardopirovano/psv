#! /bin/sh

python3 jamming.py $1 $2 > jamming-temp.sf
export PRISM_JAVAMAXMEM=24g
timeout 600 ../../bin/psv-s -s jamming-temp.sf -p jamming.pctl > result
cat result | grep "Time for model construction" | sed s/"Time for model construction: "//  | sed s/" seconds."/" sec"/
cat result | grep "Time for model checking" | sed s/"Time for model checking: "//  | sed s/" seconds."/" sec"/
cat result | grep "States:" | sed s/"States:      "//  | sed s/" (1 initial)"//
cat result | grep "Transitions:" | sed s/"Transitions: "//
rm jamming-temp.sf
rm result