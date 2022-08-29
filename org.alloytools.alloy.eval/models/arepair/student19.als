sig List {
    header: set Node
}

sig Node {
    link: set Node,
    elem: set Int
}

// Correct
fact CardinalityConstraints {
    all l: List | lone l.header
    all n: Node | lone n.link
    all n: Node | one n.elem
}

// Correct
pred Loop(This: List) {
    no This.header || one n: This.header.*link | n in n.^link
}

// Overconstraint.  Should allow no n.link
pred Sorted(This: List) {
    // Fix: replace "n.elem <= n.link.elem" with "some n.link => n.elem <= n.link.elem".
    all n:This.header.*link | n.elem <= n.link.elem
}

pred RepOk(This: List) {
     Loop[This]
     Sorted[This]
}

run {all l : List | RepOk[l] }

// Underconstraint.  result should be number of n
pred Count(This: List, x: Int, result: Int) {
    RepOk[This]
    // Fix: delete "all n: This.header.*link.elem | x = n".
    all n: This.header.*link.elem | x = n
    // Fix: replace "result = #x" with "result = #{n: This.header.*link | n.elem = x}".
    result = #x
}

run { all l : List | all x : Int | all r: Int | Count[l,x,r]} for 3 but 2 Int

abstract sig Boolean {}
one sig True, False extends Boolean {}

// Correct.
pred Contains(This: List, x: Int, result: Boolean) {
    RepOk[This]
    (some n: This.header.*link | x in n.elem && result = True) || (all n: This.header.*link | x !in n.elem && result = False)
}

fact IGNORE {
  one List
  List.header.*link = Node
}