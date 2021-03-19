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

## 7. LogHandler
LogHandler is a singleton, just define in the class initial variable.
And you are able to call it's functions shown as below.
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
