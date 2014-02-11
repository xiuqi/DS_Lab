#!/bin/sh

./logger.sh config.txt logger vector & 
./run.sh config.txt alice vector &
./run.sh config.txt bob vector &
./run.sh config.txt charlie vector & 
./run.sh config.txt daphnie vector &
