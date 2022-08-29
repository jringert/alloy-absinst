sig A {
  nx : set A
}

pred show {
  no a : A | a in a.nx
  some a : A | a in a.^nx
}

run show for 2
