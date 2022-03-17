sig A {}

lone sig A1, A2, A3 extends A {}

pred inst1 {
  one A1
  one A2
  A = A1 + A2
}

run {not inst1} for 3
