package com.artnaseef.websocketclient;

import com.ning.http.client.*;

import com.ning.http.client.providers.apache.ApacheAsyncHttpProvider;
import com.ning.http.client.providers.grizzly.GrizzlyAsyncHttpProvider;
import com.ning.http.client.websocket.WebSocket;
import com.ning.http.client.websocket.WebSocketTextListener;
import com.ning.http.client.websocket.WebSocketUpgradeHandler;
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
    private String          proxyServer = "localhost";
    private int             proxyServerPort = 808;
    private ProxyServer.Protocol	proxyProtocol = ProxyServer.Protocol.HTTP;

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
                                .setProxyServer(new ProxyServer(this.proxyProtocol, this.proxyServer, this.proxyServerPort, this.username, this.password))
                                .execute(new WebSocketUpgradeHandler.Builder().build());
            }

            LOG.info("waiting for websocket connection");
            WebSocket websocket = futureWebsocket.get();

            if ( websocket != null ) {
                LOG.info("received websocket connection");

                final WebsocketSender sender = new WebsocketSender(websocket);
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
                        sender.shutdown();
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        LOG.error("websocket error", throwable);
                        sender.shutdown();
                    }
                });

                LOG.info("sending websocket message");
                websocket.sendTextMessage("Mark");
                LOG.info("sent websocket message");

                // Start a sender to continually send messages across the socket.
                sender.start();
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
            } else if ( oneArg.startsWith("provider=") ) {
                this.parseProvider(oneArg.substring(9));
            } else if ( oneArg.startsWith("proxyProtocol=") ) {
                this.parseProxyProtocol(oneArg.substring(14));
            } else if ( oneArg.startsWith("proxyServer=") ) {
                this.proxyServer = oneArg.substring(12);
            } else if ( oneArg.startsWith("proxyServerPort=") ) {
                this.proxyServerPort = Integer.valueOf(oneArg.substring(16));
            } else if ( oneArg.startsWith("url=") ) {
                this.wsUrl = oneArg.substring(4);
            } else if ( oneArg.startsWith("useProxy=") ) {
                this.withProxy = Boolean.parseBoolean(oneArg.substring(9));
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

    protected void  parseProxyProtocol (String proto) {
        String  upper = proto.toUpperCase();

        if ( upper.equals("HTTP") ) {
            this.proxyProtocol = ProxyServer.Protocol.HTTP;
        } else if ( upper.equals("HTTPS") ) {
            this.proxyProtocol = ProxyServer.Protocol.HTTPS;
        } else if ( upper.equals("NTLM") ) {
            this.proxyProtocol = ProxyServer.Protocol.NTLM;
        } else {
            System.err.println("Unrecognized protocol \"" + proto + "\"; try \"HTTP\", \"HTTPS\", or \"NTLM\"");
            System.exit(1);
        }
    }

    protected class WebsocketSender extends Thread {
        protected boolean   runningInd = true;
        protected WebSocket socket;

        public WebsocketSender (WebSocket sock) {
            this.socket = sock;
        }

        public void run () {
            int iter = 0;

            while ( this.runningInd ) {
                try {
                    this.sleep(1000);
                    LOG.info("Sending Tom " + iter);

                    // TBD: no exceptions on failure to send to closed websocket?
                    this.socket.sendTextMessage("Tom " + iter);
                    iter++;
                } catch ( InterruptedException intExc ) {
                    if ( runningInd ) {
                        LOG.warn("websocket sender interrupted in running state");
                    }
                }
            }
        }

        public void shutdown () {
            this.runningInd = false;
            this.interrupt();
        }

        public void waitForShutdown () throws InterruptedException {
            this.join();
        }
    }
}
