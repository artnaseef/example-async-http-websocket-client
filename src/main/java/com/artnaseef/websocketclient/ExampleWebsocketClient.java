package com.artnaseef.websocketclient;

import com.ning.http.client.*;

import com.ning.http.client.providers.apache.ApacheAsyncHttpProvider;
import com.ning.http.client.providers.grizzly.GrizzlyAsyncHttpProvider;
import com.ning.http.client.websocket.WebSocket;
import com.ning.http.client.websocket.WebSocketTextListener;
import com.ning.http.client.websocket.WebSocketUpgradeHandler;
import com.sun.org.apache.xpath.internal.operations.Bool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Created by art on 5/9/14.
 */
public class ExampleWebsocketClient {
    private static final Logger LOG = LoggerFactory.getLogger(ExampleWebsocketClient.class);

    private AsyncHttpClient client;
    private boolean         withProxy = false;
    private boolean         grizzlyProvider = false;
    private String          username = "proxyuser";
    private String          password = "proxypassword";
    private String          wsUrl = "ws://localhost:8080/ws/agent";

    public static void  main (String[] args) {
        new ExampleWebsocketClient().instanceMain(args);
    }

    public void instanceMain (String[] args) {
        this.parseCmdline(args, 0);

        if ( grizzlyProvider ) {
            this.client = new AsyncHttpClient(new GrizzlyAsyncHttpProvider(new AsyncHttpClientConfig.Builder().build()));
        } else {
            this.client = new AsyncHttpClient();
        }

        try {

            Future<WebSocket> futureWebsocket;
            if ( ! withProxy ) {
                LOG.info("without proxy");
                futureWebsocket =
                        this.client.prepareGet(this.wsUrl)
                                .execute(new WebSocketUpgradeHandler.Builder().build());
            } else {
                LOG.info("with proxy user {}", username);
                futureWebsocket =
                        this.client.prepareGet(this.wsUrl)
                                .setProxyServer(new ProxyServer(ProxyServer.Protocol.HTTP,
                                                "192.168.56.101", 808, this.username, this.password))
                                .execute(new WebSocketUpgradeHandler.Builder().build());
            }

            LOG.info("waiting for websocket connection");
            WebSocket websocket = futureWebsocket.get();

            if ( websocket != null ) {
                LOG.info("received websocket connection");

                websocket.addWebSocketListener(new WebSocketTextListener() {
                    @Override
                    public void onMessage(String s) {
                        LOG.info("received response {}", s);
                    }

                    @Override
                    public void onFragment(String s, boolean b) {

                    }

                    @Override
                    public void onOpen(WebSocket webSocket) {
                        LOG.info("websocket connection open");
                    }

                    @Override
                    public void onClose(WebSocket webSocket) {
                        LOG.info("websocket connection closed");
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        LOG.error("websocket error", throwable);
                    }
                });

                LOG.info("sending websocket message");
                websocket.sendTextMessage("Mark");
                LOG.info("sent websocket message");
            } else {
                LOG.error("failed to create websocket connection");
                // TBD: get more information here
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    protected void  parseCmdline (String[] args, int start) {
        int    cur;

        cur = start;

        while ( cur < args.length ) {
            String oneArg = args[cur];

            if ( oneArg.startsWith("user=") ) {
                this.username = oneArg.substring(5);
            } else if ( oneArg.startsWith("password=") ) {
                this.password = oneArg.substring(9);
            } else if ( oneArg.startsWith("url=") ) {
                this.wsUrl = oneArg.substring(4);
            } else if ( oneArg.startsWith("useProxy=") ) {
                this.withProxy = Boolean.parseBoolean(oneArg.substring(9));
            } else if ( oneArg.startsWith("provider=") ) {
                this.parseProvider(oneArg.substring(9));
            } else {
                System.err.println("Unrecognized command-line argument " + oneArg);
                System.exit(1);
            }

            cur++;
        }
    }

    protected void  parseProvider (String provider) {
        if ( provider.equals("grizzly") ) {
            this.grizzlyProvider = true;
        } else if  ( provider.equals("netty") ) {
            this.grizzlyProvider = false;
        } else {
            System.err.println("Unrecognized provider \"" + provider + "\"; try \"grizzly\" or \"netty\"");
            System.exit(1);
        }
    }
}
