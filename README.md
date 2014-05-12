*Execute Without Proxy*
```
mvn exec:java -Dexec.mainClass=com.artnaseef.websocketclient.ExampleWebsocketClient -Dexec.arguments="useProxy=false,url=wss://localhost:8443/ws/agent,provider=grizzly"
```

*Execute With Proxy*
```
mvn exec:java -Dexec.mainClass=com.artnaseef.websocketclient.ExampleWebsocketClient -Dexec.arguments="useProxy=true,user=proxyUser,password=proxyPassword,proxyServer=localhost,proxyPort=8888,url=wss://localhost:8443/ws/agent,provider=grizzly"
```

*Successful Handshake*
```
2014-05-12 09:59:00,295 [AsyncHttpClient-Callback] INFO  com.artnaseef.websocketclient.ExampleWebsocketClient  - received response hello Mark; we are a little slow
```

