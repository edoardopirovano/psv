#! /bin/sh

mkdir -p results
for i in 3 4 5
do
	for j in 5 10 25 50 75
	do
		./run.sh $i $j | tee results/$i-$j
		echo ""
	done
done
