#!/bin/bash
echo "${PWD}"
java -Duser.language=en -cp ../../lib peer.PeerProcess 1001 localhost 5566 1 &
java -Duser.language=en -cp ../../lib peer.PeerProcess 1002 localhost 5567 0 &
echo "Deployed all peers succesfully."