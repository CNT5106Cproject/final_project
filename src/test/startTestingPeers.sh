#!/bin/bash
echo "${PWD}"
java -cp ../../lib peer.PeerProcess 1001 localhost 5566 1 > 1001.log &
java -cp ../../lib peer.PeerProcess 1002 localhost 5567 0 > 1002.log &
echo "Deployed all peers succesfully."