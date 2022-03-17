sig A {}

pred inst1 {
  some disj A1, A2 : univ |
    A = A1 + A2  
}

run {not inst1} for 3
