sig A {
  next : lone A
}


// at least 1 at most 3
// at most A1->A2->A3
pred inst1 {
  some disj A1 : univ |
    A1 in A 
  #A <=3
  
  
}

// upper bound for a signature can be based on counting
---- easy
// upper bound for tuples???




run {inst1} for 4
