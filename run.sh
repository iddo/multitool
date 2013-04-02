#!/bin/bash
i=0 args=()
for arg in "$@"; do
	args[$i]="$arg"
	((++i))
done
java -cp target/multitool-0.0.1-SNAPSHOT.jar org.iddo.multitool.HTMLTransformer "${args[@]}"
