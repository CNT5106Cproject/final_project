#!/bin/bash
echo "${PWD}"

rm ./project/1002/thefile
rm ./project/1003/thefile
rm ./project/1004/thefile
rm ./project/1005/thefile
rm ./project/1007/thefile
rm ./project/1008/thefile
rm ./project/1009/thefile

java -Duser.language=en -cp ./project peer.PeerProcess 1001 &
java -Duser.language=en -cp ./project peer.PeerProcess 1002 &
java -Duser.language=en -cp ./project peer.PeerProcess 1003 &
java -Duser.language=en -cp ./project peer.PeerProcess 1004 &
java -Duser.language=en -cp ./project peer.PeerProcess 1005 &
java -Duser.language=en -cp ./project peer.PeerProcess 1006 &
java -Duser.language=en -cp ./project peer.PeerProcess 1007 &
java -Duser.language=en -cp ./project peer.PeerProcess 1008 &
java -Duser.language=en -cp ./project peer.PeerProcess 1009 &
echo "Deployed all peers succesfully."

# Use for remote server demo
# java -Duser.language=en -cp ~/final_project/project peer.PeerProcess 1001 &
