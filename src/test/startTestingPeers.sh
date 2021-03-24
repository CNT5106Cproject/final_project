#!/bin/bash
echo "${PWD}"
java -Duser.language=en -cp ../../lib peer.PeerProcess 1001 &
java -Duser.language=en -cp ../../lib peer.PeerProcess 1002 &
echo "Deployed all peers succesfully."