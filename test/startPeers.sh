#!/bin/bash
echo "${PWD}"

rm ../demo/1002/thefile
rm ../demo/1003/thefile
rm ../demo/1004/thefile
rm ../demo/1005/thefile
rm ../demo/1007/thefile
rm ../demo/1008/thefile
rm ../demo/1009/thefile

java -Duser.language=en -cp ../demo/ PeerProcess 1001 &
java -Duser.language=en -cp ../demo/ PeerProcess 1002 &
java -Duser.language=en -cp ../demo/ PeerProcess 1003 &
java -Duser.language=en -cp ../demo/ PeerProcess 1004 &
java -Duser.language=en -cp ../demo/ PeerProcess 1005 &
java -Duser.language=en -cp ../demo/ PeerProcess 1006 &
java -Duser.language=en -cp ../demo/ PeerProcess 1007 &
java -Duser.language=en -cp ../demo/ PeerProcess 1008 &
java -Duser.language=en -cp ../demo/ PeerProcess 1009 &
echo "Deployed all peers succesfully."

# Use for remote server demo
# java PeerProcess 1001
