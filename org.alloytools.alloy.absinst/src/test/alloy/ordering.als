open util/ordering[State] as so

sig State {
    id : Int    
}

/*
lone sig State0, State1 extends State {}

fact bounds {
    one State0
    one State1
    State0 -> -1 in (this/State <: id)
    State1 -> 0 in (this/State <: id)
    this/State in State0 + State1
    (this/State <: id) in State0 -> -1 + State1 -> 0

    // we cannot access signature Ord nor its relations and thus have to constrain the functions
    so/first = State0 
    so/next = {State0->State1}
}
// negation of predicate
run {!(so/first.id < so/last.id) } for 2 but 1 Int
*/

run {so/first.id < so/last.id } for 2 but 1 Int
