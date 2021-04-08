#!/bin/bash
echo "${PWD}"

rm ../config/1002/thefile
rm ../config/1003/thefile
rm ../config/1004/thefile
rm ../config/1005/thefile
rm ../config/1007/thefile
rm ../config/1008/thefile
rm ../config/1009/thefile
java -Duser.language=en -cp ../../lib peer.PeerProcess 1001 &
java -Duser.language=en -cp ../../lib peer.PeerProcess 1002 &
java -Duser.language=en -cp ../../lib peer.PeerProcess 1003 &
java -Duser.language=en -cp ../../lib peer.PeerProcess 1004 &
java -Duser.language=en -cp ../../lib peer.PeerProcess 1005 &
java -Duser.language=en -cp ../../lib peer.PeerProcess 1006 &
java -Duser.language=en -cp ../../lib peer.PeerProcess 1007 &
java -Duser.language=en -cp ../../lib peer.PeerProcess 1008 &
java -Duser.language=en -cp ../../lib peer.PeerProcess 1009 &
echo "Deployed all peers succesfully."