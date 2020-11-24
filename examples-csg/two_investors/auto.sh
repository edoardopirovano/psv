#!/bin/bash

# normal market (single investor)
../../bin/prism two_investors_csg.prism two_investors.props -javamaxmem 8g -prop 1 -const months=1:1:9
../../bin/prism two_investors_tsg.prism two_investors.props -javamaxmem 8g -prop 1 -const months=1:1:9

# normal market (coalition of investors)
../../bin/prism two_investors_csg.prism two_investors.props -javamaxmem 8g -prop 2 -const months=1:1:9
../../bin/prism two_investors_tsg.prism two_investors.props -javamaxmem 8g -prop 2 -const months=1:1:9

# later cash-ins market (single investor)
../../bin/prism two_investors_csg.prism two_investors.props -javamaxmem 8g -prop 3 -const months=1:1:9
../../bin/prism two_investors_tsg.prism two_investors.props -javamaxmem 8g -prop 3 -const months=1:1:9

# later cash-ins market (coalition of investors)
../../bin/prism two_investors_csg.prism two_investors.props -javamaxmem 8g -prop 4 -const months=1:1:9
../../bin/prism two_investors_tsg.prism two_investors.props -javamaxmem 8g -prop 4 -const months=1:1:9

# later cash-ins with fluctuations market (single investor)
../../bin/prism two_investors_csg.prism two_investors.props -javamaxmem 8g -prop 5 -const months=1:1:9
../../bin/prism two_investors_tsg.prism two_investors.props -javamaxmem 8g -prop 5 -const months=1:1:9

# later cash-ins with fluctuations market (coalition of investors)
../../bin/prism two_investors_csg.prism two_investors.props -javamaxmem 8g -prop 6 -const months=1:1:9
../../bin/prism two_investors_tsg.prism two_investors.props -javamaxmem 8g -prop 6 -const months=1:1:9

# early cash-ins market (single investor)
../../bin/prism two_investors_csg.prism two_investors.props -javamaxmem 8g -prop 7 -const months=1:1:9
../../bin/prism two_investors_tsg.prism two_investors.props -javamaxmem 8g -prop 7 -const months=1:1:9

# early cash-ins market (coalition of investors)
../../bin/prism two_investors_csg.prism two_investors.props -javamaxmem 8g -prop 8 -const months=1:1:9
../../bin/prism two_investors_tsg.prism two_investors.props -javamaxmem 8g -prop 8 -const months=1:1:9

