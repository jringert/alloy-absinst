// this shows that introducing one sigs might make a satisfiable model unsatisfiable

sig A {}
sig B extends A {}

one sig A1 extends A {}
one sig A2 extends A {}
one sig A3 extends A {}

run {one B} for 3

// LB = B$1
// UB = B in B$1
