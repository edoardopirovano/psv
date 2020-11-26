#! /bin/sh

mkdir -p results
for i in 20 25 30 35 40 45
do
	for j in 6 8 10 12 14
	do
		./jamming.sh $i $j | tee results/$i-$j
		echo ""
	done
done
