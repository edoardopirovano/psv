This repository contains the Probabilistic Swarm Verifier (PSV) package presented in my thesis, which is based on PRISM-games.

To install on Linux, ensure you have recent versions of Make, GCC, G++ and JDK intalled and run `make` followed by install `./install.sh` in this directory.
   
To run, execute `bin/psv`, which will give further instructions. In particular, it specifies the
instructions that the program accepts as:

 -a,--async                  Check an asynchronous system (default is synchronous).
 -c,--concrete <arg>         Pass a comma separated list of integers to check a concrete system of that size.
 -e,--export <arg>           Path to export DOT file of model to.
 -f,--faultsFile <arg>       Path to a file specifying faults to inject.
 -p,--propertiesFile <arg>   Path to properties file.
 -s,--swarmFile <arg>        Path to the swarm model file.

The scenarios considered in my thesis can be found in the `examples` directory. For instance, to reproduce the results in the graph showing the results for the channel jamming scenario, run the command:

  bin/psv -s examples/jamming-graph.ssf -p examples/jamming-graph.prop -c n

replacing `n` with the desired number of agents (1,2,3,...). To obatin the lower threshold given by the abstract model, run the command:

  bin/psv -s examples/jamming-graph.ssf -p examples/jamming-graph.prop