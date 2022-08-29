sig A {}
sig B extends A {}

run {one B} for 3

// LB = B$1
// UB = B in B$1
