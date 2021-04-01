#!/bin/bash
echo "${PWD}"
java -Duser.language=en -cp ../../lib peer.PeerProcess 1001 &
java -Duser.language=en -cp ../../lib peer.PeerProcess 1002 &
java -Duser.language=en -cp ../../lib peer.PeerProcess 1003 &
echo "Deployed all peers succesfully."