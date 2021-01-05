import sys

n = int(sys.argv[1])

print("agent")
print("\txCoord : [0.." + str(n - 1) + "] init 0;")
print("\tyCoord : [0.." + str(n - 1) + "] init 0;")
print("\t")
print("\tobservedLeft : [0..1] init 0; ")
print("\tobservedRight : [0..1] init 0;")
print("\tobservedUp : [0..1] init 0;")
print("\tobservedDown : [0..1] init 0;")
print("\t")
print("\tstate : [0..3] init 0;")
print("\t")
print("\tdirection : [0..3] init 0;")
print("\t")

for x in range(0, n):
    for y in range(0, n):
        print("\t[visit" + str(x) + str(y) + "] (xCoord=" + str(x) + " & yCoord=" + str(y) + " & state=0);")

print("\t")
print("\t[observe] (state=1);")
print("\t[chooseMove] (state=2);")
print("\t[moveLeft] (state=3 & direction=0);")
print("\t[moveUp] (state=3 & direction=1);")
print("\t[moveRight] (state=3 & direction=2);")
print("\t[moveDown] (state=3 & direction=3);")
print("\t")
print("\tupdate")
print("\t\t(chooseMove, true, {}) -> 1.0:(state'=3);")
print("\t\t(chooseMove, observedLeft=observedRight & observedRight=observedUp & observedUp=observedDown, {}) -> " +
      "(1/4):(direction'=0) + (1/4):(direction'=1) + (1/4):(direction'=2) + (1/4):(direction'=3);")
print("\t\t(chooseMove, observedLeft<observedRight & observedLeft<observedUp & observedLeft<observedDown, {}) -> " +
      "(1):(direction'=0);")
print("\t\t(chooseMove, observedUp<observedLeft & observedUp<observedRight & observedUp<observedDown, {}) -> " +
      "(1):(direction'=1);")
print("\t\t(chooseMove, observedRight<observedLeft & observedRight<observedUp & observedRight<observedDown, {}) -> " +
      "(1):(direction'=2);")
print("\t\t(chooseMove, observedDown<observedLeft & observedDown<observedRight & observedDown<observedUp, {}) -> " +
      "(1):(direction'=3);")
print("\t\t(chooseMove, observedUp=observedDown & observedUp<observedLeft & observedUp<observedRight, {}) -> " +
      "(1/2):(direction'=1) + (1/2):(direction'=3);")
print("\t\t(chooseMove, observedUp=observedRight & observedUp<observedLeft & observedUp<observedDown, {}) -> " +
      "(1/2):(direction'=1) + (1/2):(direction'=2);")
print("\t\t(chooseMove, observedUp=observedLeft & observedUp<observedRight & observedUp<observedDown, {}) -> " +
      "(1/2):(direction'=1) + (1/2):(direction'=0);")
print("\t\t(chooseMove, observedRight=observedDown & observedDown<observedLeft & observedDown<observedUp, {}) -> " +
      "(1/2):(direction'=2) + (1/2):(direction'=3);")
print("\t\t(chooseMove, observedLeft=observedDown & observedDown<observedRight & observedDown<observedUp, {}) -> " +
      "(1/2):(direction'=0) + (1/2):(direction'=3);")
print("\t\t(chooseMove, observedLeft=observedRight & observedLeft<observedDown & observedLeft<observedUp, {}) -> " +
      "(1/2):(direction'=2) + (1/2):(direction'=3);")
print("\t\t(chooseMove, observedDown=observedLeft & observedLeft=observedRight & observedLeft<observedUp, {}) -> " +
      "(1/3):(direction'=2) + (1/3):(direction'=3) + (1/3):(direction'=0);")
print("\t\t(chooseMove, observedUp=observedLeft & observedLeft=observedRight & observedLeft<observedDown, {}) -> " +
      "(1/3):(direction'=2) + (1/3):(direction'=1) + (1/3):(direction'=0);")
print("\t\t(chooseMove, observedUp=observedLeft & observedLeft=observedDown & observedLeft<observedRight, {}) -> " +
      "(1/3):(direction'=3) + (1/3):(direction'=0) + (1/3):(direction'=1);")
print("\t\t(chooseMove, observedUp=observedRight & observedRight=observedDown & observedRight<observedLeft, {}) -> " +
      "(1/3):(direction'=3) + (1/3):(direction'=2) + (1/3):(direction'=1);")
print("\t\t")
print("\t\t(moveRight, xCoord<" + str(n - 1) + ", {}) -> (xCoord'=xCoord+1) & (state'=0);")
print("\t\t(moveRight, xCoord=" + str(n - 1) + ", {}) -> (xCoord'=0) & (state'=0);")
print("\t\t(moveLeft, xCoord>0, {}) -> (xCoord'=xCoord-1) & (state'=0);")
print("\t\t(moveLeft, xCoord=0, {}) -> (xCoord'=" + str(n - 1) + ") & (state'=0);")
print("\t\t(moveDown, yCoord<" + str(n - 1) + ", {}) -> (yCoord'=yCoord+1) & (state'=0);")
print("\t\t(moveDown, yCoord=" + str(n - 1) + ", {}) -> (yCoord'=0) & (state'=0);")
print("\t\t(moveUp, yCoord>0, {}) -> (yCoord'=yCoord-1) & (state'=0);")
print("\t\t(moveUp, yCoord=0, {}) -> (yCoord'=" + str(n - 1) + ") & (state'=0);")
print("\t\t")

for x in range(0, n):
    for y in range(0, n):
        print("\t\t(visit" + str(x) + str(y) + ", true, {}) -> (state'=1);")

print("\t\t")

for x in range(0, n):
    for y in range(0, n):
        print("\t\t(observe, xCoord=" + str(x) + " & yCoord=" + str(y) + ", {}) -> "
                                                                         "(observedLeft'=uValue" + str(
            (x - 1) % n) + str(y) + "_E) "
                                    "& (observedRight'=uValue" + str((x + 1) % n) + str(y) + "_E) " +
              "& (observedUp'=uValue" + str(x) + str((y - 1) % n) + "_E) " +
              "& (observedDown'=uValue" + str(x) + str((y + 1) % n) + "_E) & (state'=2);")

print("\tendupdate")
print("endagent")
print()
print("environment")

print("\t")
for x in range(0, n):
    for y in range(0, n):
        print("\tuValue" + str(x) + str(y) + " : int init 0;")

print("\t")
print("\t[none] true;")
print("\t")
print("\tupdate")

for x in range(0, n):
    for y in range(0, n):
        if (sys.argv[2] == "nc"):
            print("\t\t(none, true, {visit" + str(x) + str(y) + "}) -> (uValue" + str(x) + str(y) + "'=uValue" + str(x) + str(
                y) + "+1);")
        if (sys.argv[2] == "lrta"):
            print("\t\t(none, true, {visit" + str(x) + str(y) + "}) -> (uValue" + str(x) + str(y) + "'=floor(min(" +
                  "uValue" + str((x + 1) % n) + str(y) +
                  ", uValue" + str((x - 1) % n) + str(y) +
                  ", uValue" + str(x) + str((y + 1) % n) +
                  ", uValue" + str(x) + str((y - 1) % n) + "))+1);")

print("\tendupdate")
print("endenvironment")
print()

label = ""
for x in range(0, n):
    for y in range(0, n):
        label += "uValue" + str(x) + str(y) + "_E>0 & "

print("label \"allVisited\" = " + label[:-3] + ";")
