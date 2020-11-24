#!/bin/csh

../../bin/prism medium_access.prism medium_access.props -prop 1 -javamaxmem 16g -const q1=0.95,q2=0.75,emax=5,k=10
../../bin/prism medium_access.prism medium_access.props -prop 1 -javamaxmem 16g -const q1=0.95,q2=0.75,emax=10,k=20
../../bin/prism medium_access.prism medium_access.props -prop 1 -javamaxmem 16g -const q1=0.95,q2=0.75,emax=20,k=40
../../bin/prism medium_access.prism medium_access.props -prop 1 -javamaxmem 16g -const q1=0.95,q2=0.75,emax=40,k=80
../../bin/prism medium_access.prism medium_access.props -prop 1 -javamaxmem 16g -const q1=0.95,q2=0.75,emax=80,k=160

../../bin/prism power_control.prism power_control.props -prop 1 -javamaxmem 16g -const powmax=2,emax=20,k=1,fail=0.1
../../bin/prism power_control.prism power_control.props -prop 1 -javamaxmem 16g -const powmax=2,emax=40,k=1,fail=0.1
../../bin/prism power_control.prism power_control.props -prop 1 -javamaxmem 16g -const powmax=4,emax=20,k=1,fail=0.1
../../bin/prism power_control.prism power_control.props -prop 1 -javamaxmem 16g -const powmax=4,emax=40,k=1,fail=0.1

../../bin/prism aloha_backoff3.prism aloha_backoff3.props -prop 2 -javamaxmem 16g -const bcmax=1,tmax=8,q=0.9
../../bin/prism aloha_backoff3.prism aloha_backoff3.props -prop 2 -javamaxmem 16g -const bcmax=2,tmax=8,q=0.9
../../bin/prism aloha_backoff3.prism aloha_backoff3.props -prop 2 -javamaxmem 16g -const bcmax=3,tmax=8,q=0.9
../../bin/prism aloha_backoff3.prism aloha_backoff3.props -prop 2 -javamaxmem 16g -const bcmax=4,tmax=8,q=0.9
../../bin/prism aloha_backoff3.prism aloha_backoff3.props -prop 2 -javamaxmem 16g -const bcmax=5,tmax=8,q=0.9

../../bin/prism robot_coordination.prism robot_coordination.props -prop 2 -javamaxmem 16g -const q=0.1,l=5,k=5
../../bin/prism robot_coordination.prism robot_coordination.props -prop 2 -javamaxmem 16g -const q=0.1,l=10,k=10
../../bin/prism robot_coordination.prism robot_coordination.props -prop 2 -javamaxmem 16g -const q=0.1,l=15,k=15g
../../bin/prism robot_coordination.prism robot_coordination.props -prop 2 -javamaxmem 16g -const q=0.1,l=20,k=20


