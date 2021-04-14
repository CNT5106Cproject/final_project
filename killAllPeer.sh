#!/bin/bash
kill -9 $(ps aux | grep java | grep PeerProces | awk '{print $2}')

# Use for remote server demo
# ps aux | grep java | grep PeerProces | grep yiming | awk '{print $2}' | xargs /bin/kill
