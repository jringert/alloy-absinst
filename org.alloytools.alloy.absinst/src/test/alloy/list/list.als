module list
one sig List {header: lone Node}
sig Node {elem: Int, link: lone Node}

pred Acyclic {
  all n: List.header.*link | n !in n.^link}
pred NoRepetition {
  all disj m, n : List.header.*link | m.elem != n.elem}

fact Reachability {List.header.*link = Node}

// original run command
run {Acyclic and NoRepetition}

// partial instance such that all extensions satisfy the property, but removing any atom or tuple would not
one sig Node1 extends Node{}{no link}
pred xxx {List.header = Node1}

// should not have a solution, but does once one line is missing above
run {xxx and !(Acyclic and NoRepetition)}

