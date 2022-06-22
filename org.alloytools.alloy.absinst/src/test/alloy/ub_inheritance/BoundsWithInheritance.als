sig A {}
sig B extends A {}
sig C extends A {}

run {} for 3 but 6 A, 3 B, 3 C

run {no C} for 3 but 3 A, 6 B
