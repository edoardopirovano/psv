#!/bin/bash

# normal market
../../bin/prism investor_csg.prism investor.props -javamaxmem 8g -prop 1 -const months=1:1:9
../../bin/prism investor_tsg.prism investor.props -javamaxmem 8g -prop 1 -const months=1:1:9

# later cash-ins market
../../bin/prism investor_csg.prism investor.props -javamaxmem 8g -prop 2 -const months=1:1:9
../../bin/prism investor_tsg.prism investor.props -javamaxmem 8g -prop 2 -const months=1:1:9

# later cash-ins with fluctuations market
../../bin/prism investor_csg.prism investor.props -javamaxmem 8g -prop 3 -const months=1:1:9
../../bin/prism investor_tsg.prism investor.props -javamaxmem 8g -prop 3 -const months=1:1:9

# early cash-ins market
../../bin/prism investor_csg.prism investor.props -javamaxmem 8g -prop 4 -const months=1:1:9
../../bin/prism investor_tsg.prism investor.props -javamaxmem 8g -prop 4 -const months=1:1:9

