# Computer Networks Project
---
- Building a P2P file-sharing software like BitTorrent.
- About 800 line for midpoint check on March 10
- Final Submission 4/20

---
## Functions

1.HandShakeMsg (Mayank)
2.ActualMsg (Jim)
3.Server (YiMing)
4.Client (Mayank)
5.PeerProcess (YiMing)
6.FileManager (Jim)
7.LogHandler (YiMing)

## 6. FileManager
- FileManager is a singleton,  new it as the class's default variables.
  Then you are able to call it's functions shown in the function list.

### Block & Bit field functions
| Function Names | input |  return | description  |
| ------------- | ------------- | ----------- | ------- | 
| insertBitfield  | (int peerId, byte[] b, int len) | None | record what peices does **others** have |
| updateHave | (String peerId, int blockIdx) | None | update **others'** have peices |
| buildOwnBitfield | int remainderBits | None | Build own bit field record in memory |
| updateOwnBitfield | int blockIdx | None | update own bit field |
| getOwnBitfield | | None | get own bit field |
| pickInterestedFileBlock | (String peerId) | int blockIdx | use for request msg, random pick a interested block which the other peer (peerId) have |
| isInterested | String peerId | boolean | interested in target Peer's blocks |

### File functions
| Function Names | input |  return | description  |
| read | (int blockIdx, byte[] b, int len) | int byteRead | read the block, return the length of bytes which just be read |
| write | (int blockIdx, byte[] b, int len) | int byteWrite | write the block, return the lenght of bytes which just be written |
| close | | None | close the file |

### Other functions
| Function Names | input |  return | description  |
| isComplete | | None | is peer finishing download the file |
| printByteArray | byte[] bytes | None | print bytes to string |


## 7. LogHandler
- LogHandler is a singleton,  new it as the class's default variables.
  Then you are able to call it's functions shown in the function list.
  
  ```
    LogHandler logging = new LogHandler();
    ...
    logging.writeLog("This is a test log");
  ```

- Please naming the functions with this rule
  ```
    log{Actions}
  ```
 
### Project description log functions
---
| Function Names | input |  return | description  |
| ------------- | ------------- | ----------- | ------- | 
| logStartConn  | (Peer client, Peer targetHost) | None | log start connection |
| logChangePrefersPeers  | | None | log change of preferred neighbors |
| logChangeUnchokedPeer  | | None | change of unchoke neighbors |
| logUnchoking  | | None | log unchoke targe peer |
| logChoking    | | None | log choke target peer  |
| logSendHaveMsg  | | None | log server send have msg |
| logSendInterestMsg  | | None | log cleint send interest msg |
| logSendNotInterestMsg  | | None | log cleint send not interest msg |
| logDownload  | | None | log download block |
| logCompleteFile  | | None | log complete downloading the file |
| logCloseConn  | | None | log close connection |
| logSendHandShakeMsg | | None | log send hand shake msg |

### Error log functions
---
| Function Names | input |  return | description  |
| ------------- | ------------- | ----------- | ------- | 
| logConnError  | (Peer client, Peer targetHost) | None | Connection Error |

### Other log functions
---
| Function Names | input |  return | description  |
| ------------- | ------------- | ----------- | ------- | 
| writeLog      | (String msg) | None | Write log to log file handler |
| writeLog      | (String msg, Level lvl) | None | Write log to log file handler with log level |
| logEstablishPeer | | | |
| logStartServer | | | |
| logStartClient | | | |
