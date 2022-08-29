module list
one sig List {header: lone Node}
sig Node {elem: Int, link: lone Node}

pred Acyclic {
  all n: List.header.*link | n !in n.^link}
pred NoRepetition {
  all disj m, n : List.header.*link | m.elem != n.elem}

fact Reachability {List.header.*link = Node}

// original run command
run {Acyclic and NoRepetition} for exactly 5 Node

