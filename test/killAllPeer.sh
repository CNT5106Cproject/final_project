#!/bin/bash mac local
# kill -9 $(ps aux | grep java | grep PeerProces | awk '{print $2}')

# Use for remote server demo
ssh lin114-00 ps aux | grep java | grep PeerProces | grep yiming | awk '{print $2}' | xargs /bin/kill;
ssh lin114-01 ps aux | grep java | grep PeerProces | grep yiming | awk '{print $2}' | xargs /bin/kill;
ssh lin114-02 ps aux | grep java | grep PeerProces | grep yiming | awk '{print $2}' | xargs /bin/kill;
ssh lin114-03 ps aux | grep java | grep PeerProces | grep yiming | awk '{print $2}' | xargs /bin/kill;
ssh lin114-04 ps aux | grep java | grep PeerProces | grep yiming | awk '{print $2}' | xargs /bin/kill;
ssh lin114-05 ps aux | grep java | grep PeerProces | grep yiming | awk '{print $2}' | xargs /bin/kill;
ssh lin114-06 ps aux | grep java | grep PeerProces | grep yiming | awk '{print $2}' | xargs /bin/kill;
ssh lin114-07 ps aux | grep java | grep PeerProces | grep yiming | awk '{print $2}' | xargs /bin/kill;
ssh lin114-08 ps aux | grep java | grep PeerProces | grep yiming | awk '{print $2}' | xargs /bin/kill;
