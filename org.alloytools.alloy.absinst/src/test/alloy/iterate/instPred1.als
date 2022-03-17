sig A {
  bs : set B
}

sig B {}

sig C extends B {
  bs : one B
}

pred inst1 {

some disj _B0,_C0,_B2,_B1,_A0 : univ {
  
  this/A = _A0
  this/B = _B0 + _B1 + _B2 + _C0
  this/C = _C0
  this/C <: bs = _C0 -> _B0
  no this/A <: bs
  }
}

run {not inst1} for 4

pred inst2 {

some disj _B0,_C0,_C1 : univ {
  
  no this/A
  this/B = _B0 + _C0 + _C1
  this/C = _C0 + _C1
  this/C <: bs = _C0 -> _B0 + _C1 -> _B0
  }
}

run {not inst2} for 4

run {not inst1 and not inst2} for 4
