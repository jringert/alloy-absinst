var lone sig A, B {}

fact {
  always (one A implies after one B)
  always (one B implies after one A)
}
run {always #(A + B) = 1}
