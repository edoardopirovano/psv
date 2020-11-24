This repository contains the Probabilistic Swarm Verifier for Strategic Properties (PSV-S), which is based on PRISM-games.

To install on Linux, ensure you have recent versions of Make, GCC, G++ and JDK 8 intalled and run `make` followed by install `./install.sh` in this directory.
   
To run, execute `bin/psv-s`, which will give further instructions.

Example swarm systems can be found in `examples/swarms`. Once in this folder, to reproduce the results in Figure 1, run:

  ../../bin/psv-s -s jamming-fig1.sf -p jamming-fig1.pctl -c n

replacing `n` with the desired number of agents (1,2,3,...).

To reproduce the timing results in Table 1, run `./runAll.sh`. This will output one cell of the table at a time, moving down columns then across rows.