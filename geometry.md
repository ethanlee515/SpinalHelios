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
Edges along each dimension are stored in two-dimensional maps.
To distinguish the indices with those of input and output qubits, we index these maps using the labels $$(s, t)$$.
For Pauli correction output purposes, we focus on the north-south and east-west directions.
The north-south edges are stored as follows:
![north-south](nw-st.png)
Specifically, the north-south edges are further partitioned into four cases:
* First column:
* Middle odd column
* Middle even column
* Last column
TODO describe

TODO what about east-west?

## TODO math
