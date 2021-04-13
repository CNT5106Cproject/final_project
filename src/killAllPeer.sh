#!/bin/bash
kill -9 $(ps aux | grep java | grep peer.PeerProces | awk '{print $2}')