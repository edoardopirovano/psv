#! /bin/sh

mkdir -p results
for i in 3 4 5
do
	for j in 5 10 15 20 25
	do
		./jamming.sh $i $j | tee results/$i-$j
		echo ""
	done
done
