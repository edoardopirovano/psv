import sys

n = int(sys.argv[1])

print("agent")
print("\txCoord : [0.." + str(n-1) + "] init 0;")
print("\tyCoord : [0.." + str(n-1) + "] init 0;")
print("\t")
print("\tobservedLeft : [0..1] init 0; ")
print("\tobservedRight : [0..1] init 0;")
print("\tobservedUp : [0..1] init 0;")
print("\tobservedDown : [0..1] init 0;")
print("\t")
print("\tstate : [0..2] init 0;")
print("\t")

for x in range(0,n):
	for y in range(0,n):
		print("\t[visit" + str(x) + str(y) + "] (xCoord=" + str(x) + " & yCoord=" + str(y) + " & state=0) : 1;")

print("\t")
print("\t[observe] (state=1) : 1;")
print("\t")
print("\t[moveLeft] (observedLeft=observedRight & observedRight=observedUp & observedUp=observedDown & state=2) : (1/4);")
print("\t[moveRight] (observedLeft=observedRight & observedRight=observedUp & observedUp=observedDown & state=2) : (1/4);")
print("\t[moveUp] (observedLeft=observedRight & observedRight=observedUp & observedUp=observedDown & state=2) : (1/4);")
print("\t[moveDown] (observedLeft=observedRight & observedRight=observedUp & observedUp=observedDown & state=2) : (1/4);")
print("\t")
print("\t[moveLeft] (observedLeft<observedRight & observedLeft<observedUp & observedLeft<observedDown & state=2) : 1;")
print("\t")
print("\t[moveRight] (observedRight<observedLeft & observedRight<observedUp & observedRight<observedDown & state=2) : 1;")
print("\t")
print("\t[moveUp] (observedUp<observedLeft & observedUp<observedRight & observedUp<observedDown & state=2) : 1;")
print("\t")
print("\t[moveDown] (observedDown<observedLeft & observedDown<observedRight & observedDown<observedUp & state=2) : 1;")
print("\t")
print("\t[moveUp] (observedUp=observedDown & observedUp<observedLeft & observedUp<observedRight & state=2) : (1/2);")
print("\t[moveDown] (observedUp=observedDown & observedUp<observedLeft & observedUp<observedRight & state=2) : (1/2);")
print("\t")
print("\t[moveUp] (observedUp=observedRight & observedUp<observedLeft & observedUp<observedDown & state=2) : (1/2);")
print("\t[moveRight] (observedUp=observedRight & observedUp<observedLeft & observedUp<observedDown & state=2) : (1/2);")
print("\t")
print("\t[moveUp] (observedUp=observedLeft & observedUp<observedRight & observedUp<observedDown & state=2) : (1/2);")
print("\t[moveLeft] (observedUp=observedLeft & observedUp<observedRight & observedUp<observedDown & state=2) : (1/2);")
print("\t")
print("\t[moveDown] (observedRight=observedDown & observedDown<observedLeft & observedDown<observedUp & state=2) : (1/2);")
print("\t[moveRight] (observedRight=observedDown & observedDown<observedLeft & observedDown<observedUp & state=2) : (1/2);")
print("\t")
print("\t[moveDown] (observedLeft=observedDown & observedDown<observedRight & observedDown<observedUp & state=2) : (1/2);")
print("\t[moveLeft] (observedLeft=observedDown & observedDown<observedRight & observedDown<observedUp & state=2) : (1/2);")
print("\t")
print("\t[moveRight] (observedLeft=observedRight & observedLeft<observedDown & observedLeft<observedUp & state=2) : (1/2);")
print("\t[moveLeft] (observedLeft=observedRight & observedLeft<observedDown & observedLeft<observedUp & state=2) : (1/2);")
print("\t")
print("\t[moveRight] (observedDown=observedLeft & observedLeft=observedRight & observedLeft<observedUp & state=2) : (1/3);")
print("\t[moveDown] (observedDown=observedLeft & observedLeft=observedRight & observedLeft<observedUp & state=2) : (1/3);")
print("\t[moveLeft] (observedDown=observedLeft & observedLeft=observedRight & observedLeft<observedUp & state=2) : (1/3);")
print("\t")
print("\t[moveRight] (observedUp=observedLeft & observedLeft=observedRight & observedLeft<observedDown & state=2) : (1/3);")
print("\t[moveUp] (observedUp=observedLeft & observedLeft=observedRight & observedLeft<observedDown & state=2) : (1/3);")
print("\t[moveLeft] (observedUp=observedLeft & observedLeft=observedRight & observedLeft<observedDown & state=2) : (1/3);")
print("\t")
print("\t[moveDown] (observedUp=observedLeft & observedLeft=observedDown & observedLeft<observedRight & state=2) : (1/3);")
print("\t[moveUp] (observedUp=observedLeft & observedLeft=observedDown & observedLeft<observedRight & state=2) : (1/3);")
print("\t[moveLeft] (observedUp=observedLeft & observedLeft=observedDown & observedLeft<observedRight & state=2) : (1/3);")
print("\t")
print("\t[moveDown] (observedUp=observedRight & observedRight=observedDown & observedRight<observedLeft & state=2) : (1/3);")
print("\t[moveUp] (observedUp=observedRight & observedRight=observedDown & observedRight<observedLeft & state=2) : (1/3);")
print("\t[moveRight] (observedUp=observedRight & observedRight=observedDown & observedRight<observedLeft & state=2) : (1/3);")
print("\t")
print("\tupdate")
print("\t\t(moveRight, xCoord<" + str(n-1) + ", {}) -> (xCoord'=xCoord+1) & (state'=0);")
print("\t\t(moveRight, xCoord=" + str(n-1) + ", {}) -> (xCoord'=0) & (state'=0);")
print("\t\t(moveLeft, xCoord>0, {}) -> (xCoord'=xCoord-1) & (state'=0);")
print("\t\t(moveLeft, xCoord=0, {}) -> (xCoord'=" + str(n-1) + ") & (state'=0);")
print("\t\t(moveDown, yCoord<" + str(n-1) + ", {}) -> (yCoord'=yCoord+1) & (state'=0);")
print("\t\t(moveDown, yCoord=" + str(n-1) + ", {}) -> (yCoord'=0) & (state'=0);")
print("\t\t(moveUp, yCoord>0, {}) -> (yCoord'=yCoord-1) & (state'=0);")
print("\t\t(moveUp, yCoord=0, {}) -> (yCoord'=" + str(n-1) + ") & (state'=0);")
print("\t\t")

for x in range(0,n):
	for y in range(0,n):
		print("\t\t(visit" + str(x) + str(y) + ", true, {}) -> (state'=1);")
		
print("\t\t")

for x in range(0,n):
	for y in range(0,n):
		print("\t\t(observe, xCoord=" + str(x) + " & yCoord=" + str(y) + ", {}) -> "
			"(observedLeft'=uValue" + str((x - 1) % n) + str(y) + "_E) "
			"& (observedRight'=uValue" + str((x + 1) % n) + str(y) + "_E) " +
			"& (observedUp'=uValue" + str(x) + str((y - 1) % n) + "_E) " +
			"& (observedDown'=uValue" + str(x) + str((y + 1) % n) + "_E) & (state'=2);")

print("\tendupdate")
print("endagent")
print()
print("environment")

for x in range(0,n):
	for y in range(0,n):
		print("\tuValue" + str(x) + str(y) + " : int init 0;")

print("\t")
print("\tupdate")

for x in range(0,n):
	for y in range(0,n):
		if (sys.argv[2] == "nc"):
			print("\t\t(true, {visit" + str(x) + str(y) + "}) -> (uValue" + str(x) + str(y) + "'=uValue" + str(x) + str(y) + "+1);")
		if (sys.argv[2] == "lrta"):
			print("\t\t(true, {visit" + str(x) + str(y) + "}) -> (uValue" + str(x) + str(y) + "'=floor(min(" + 
				"uValue" + str((x + 1) % n) + str(y) +
				", uValue" + str((x - 1) % n) + str(y) +
				", uValue" + str(x) + str((y + 1) % n) +
				", uValue" + str(x) + str((y - 1) % n) + "))+1);")


print("\tendupdate")
print("endenvironment")
print()

label = ""
for x in range(0,n):
	for y in range(0,n):
		label += "uValue" + str(x) + str(y) + "_E>0 & "

print("label \"allVisited\" = " + label[:-3] + ";")