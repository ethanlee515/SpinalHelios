# Geometry of Decoding Graph

We describe how the input and output are indexed.
Recall that Helios is based on surface code, and computes Pauli X corrections using measured Z-syndromes.

## Input indexing

The data qubits are organized in a grid, and syndromes are measured using ancillas located inside each face of the grid.
Here is an example for code distance $$d=5$$:
![vertices](./img/vertices.png)

Now, the Z-measurements are themselves stored in a grid of size $$(w_x, w_z)=(d + 1, \lfloor\frac{d}{2}\rfloor)$$.
![syndrome coordinates](./img/syndrome-ij.png)

In other words, the vertical axis is in a zig-zag pattern:
![syndrome axis](./img/syndrome-axis.png)

## Edges indexing

In the decoding graph, syndrome qubits correspond to vertices, and data qubits correspond to edges.
The edges are partitioned based on their directions: north-south, east-west, and up-down.
(North here actually means north-west.)
Edges along each dimension are stored in two-dimensional maps;
to distinguish the indices with those of input and output qubits, the edges use the labels $$(s, t)$$.
For Pauli correction output purposes, we focus on the north-south and east-west directions.
Notice that there are only a total of $$d^2-d+1$$ corrections, meaning that some physical qubits will never report an error.

Concretely, the north-south edges are further partitioned into four cases:
* First column (cyan): The boundary conditions are set up as `nexist_edge` and will never report an error.
* Middle odd column (teal): These edges go between vertices $$(s-1, t-1)$$ and $$(s, t-1)$$.
* Middle even column (purple): These edges go between vertices $$(s-1, t-1)$$ and $$(s, t)$$.
* Last column (green): These edges extends southward from vertices $$(s-1, t-1)$$.
These are summarized in the following figure:
![north-south](ns-st.png)

On the other hand, the east-west edges are partitioned into five cases:
* First column (green): Extends westwards from vertices $$(s-1, t)$$
* Middle odd column (teal): Between vertices $$(s, t)$$ and $$(s-1, t)$$
* Middle even column (purple): Between vertices $$(s, t-1)$$ and $$(s-1, t)$$.
* Last column except last row (cyan): Will never report an error
* Last column and final row (brown): Extends eastwards from $$(s, t-1)$$.
These are summarized in the following figure:
![east-west](ew-st.png)

## Output coordinates

Now we work out the final coordinate transform case-by-case.
This connects the output grid to the edge indexing, then back to the input indexing.
We will spell out all the details to check that everything matches in the implementation.
We label data qubits using $$(x, y)$$ pairs.
We choose $$x$$ as the downward direction to match the $$i$$-direction for the syndromes.
![output axis](xy-axis.png)

### North-South

* First column has no edges.
* For middle odd columns, the data at $$(x, y)$$ is between syndromes $$(x, \lfloor\frac{y}{2}\rfloor)$$ to $$(x + 1, \lfloor\frac{y}{2}\rfloor)$$.
  We have $$(s-1, t-1)=(x, \lfloor\frac{y}{2}\rfloor)$$; solving it gives $$(s, t)=(x + 1, \lfloor\frac{y}{2}\rfloor+1)$$.
* For middle even columns, the data at $$(x, y)$$ is between syndromes $$(x, \frac{y}{2} - 1)$$ and $$(x + 1,\frac{y}{2})$$.
  This gives $$(s, t)=(x+1, \frac{y}{2})$$
* Last column extends from the syndromes at $$(x, w_z-1)$$. This gives $$(s, t)=(x+1, w_z)$$.

### South-West

* First column extends from the syndromes at $$(x, 0)$$. This gives $$(s, t)=(x+1, 0)$$.
* Middle odd column is between $$(x, \lfloor\frac{y}{2}\rfloor)$$ and $$(x+1, \lfloor\frac{y}{2}\rfloor)$$.
  This gives $$(s, t)=(x+1, \lfloor\frac{y}{2}\rfloor)$$
* Middle even column is between $$(x, \frac{y}{2})$$ and $$(x+1, \frac{y}{2}-1)$$.
  This gives $$(s, t)=(x+1, \frac{y}{2})$$.
* Last column except last row will never report an error.
* Final row in last column extends from the final syndrome $$(w_x - 1, w_z - 1)$$.
  This gives $$(s, t)=(w_x - 1, w_z)$$.