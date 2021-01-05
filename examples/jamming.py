import sys

n = int(sys.argv[1])
k = int(sys.argv[2])

print("agent")
print("\ttransmitted : bool init false;")
print("\t")
for x in range(0,n):
	print("\t[transmit" + str(x) + "] (transmitted=false);")
print("\t")
print("\t[transmit] (transmitted=true);")
print("\t")
for x in range(0,n):
	print("\t[block"+ str(x) + "] true;")
print("\t")
print("\tupdate")
for x in range(0,n):
	print("\t\t(transmit" + str(x) + ", true, !{block" + str(x) + "}) -> 0.4:(transmitted'=true);")
	print("\t\t(transmit" + str(x) + ", true, {block" + str(x) + "}) -> 0.1:(transmitted'=true);")
print("\t\t")
print("\t\t(transmit, true, {}) -> 1.0:(transmitted'=false);")
print("\tendupdate")
print("endagent")
print("")
print("environment")
print("\treceived : [0.." + str(k) + "] init 0;")
print("\t")
print("\t[receive] true;")
print("\t")
print("\tupdate")
print("\t\t(receive, received<" + str(k) + ", {transmit}) -> 1.0:(received'=received+1);")
print("\tendupdate")
print("endenvironment")
print("")
print("label \"receivedAll\" = received_E=" + str(k) + ";")