#!/bin/bash

# cumulative expected damage (const K not used)

../../bin/prism ids_scenario1.prism ids.props -javamaxmem 8g -prop 1 -const rounds=25,K=1
../../bin/prism ids_scenario2.prism ids.props -javamaxmem 8g -prop 1 -const rounds=25,K=1
#../../bin/prism ids_simple.prism ids.props -javamaxmem 8g -prop 1 -const rounds=25,K=1

../../bin/prism ids_scenario1.prism ids.props -javamaxmem 8g -prop 1 -const rounds=50,K=1
../../bin/prism ids_scenario2.prism ids.props -javamaxmem 8g -prop 1 -const rounds=50,K=1
#../../bin/prism ids_simple.prism ids.props -javamaxmem 8g -prop 1 -const rounds=50,K=1

../../bin/prism ids_scenario1.prism ids.props -javamaxmem 8g -prop 1 -const rounds=100,K=1
../../bin/prism ids_scenario2.prism ids.props -javamaxmem 8g -prop 1 -const rounds=100,K=1
#../../bin/prism ids_simple.prism ids.props -javamaxmem 8g -prop 1 -const rounds=100,K=1

../../bin/prism ids_scenario1.prism ids.props -javamaxmem 8g -prop 1 -const rounds=200,K=1
../../bin/prism ids_scenario2.prism ids.props -javamaxmem 8g -prop 1 -const rounds=200,K=1
#../../bin/prism ids_simple.prism ids.props -javamaxmem 8g -prop 1 -const rounds=200,K=1


# expected damage in a specific round

../../bin/prism ids_scenario1.prism ids.props -javamaxmem 8g -prop 2 -const rounds=25,K=1:1:25
../../bin/prism ids_scenario2.prism ids.props -javamaxmem 8g -prop 2 -const rounds=25,K=1:1:25
#../../bin/prism ids_simple.prism ids.props -javamaxmem 8g -prop 2 -const rounds=25,K=1:1:25
