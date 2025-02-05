package org.wanaku.server.quarkus;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.Dependent;

import io.smallrye.mutiny.Multi;
import io.vertx.core.json.JsonObject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.jboss.logging.Logger;
import org.wanaku.server.quarkus.types.McpMessage;
import org.wanaku.server.quarkus.types.Messages;

@Dependent
public class McpController {
    private static final Logger LOG = Logger.getLogger(McpController.class);

    @ConfigProperty(name = "quarkus.http.host")
    String host;

    @ConfigProperty(name = "quarkus.http.port")
    int port;

    @PostConstruct
    void initChannel() {
        LOG.info("McpController instantiated");
    }

    @Incoming("mcpNewConnections")
    @Outgoing("mcpEvents")
    public McpMessage handle(String request) {
        return McpMessage.newConnectionMessage(host, port);
    }

    @Incoming("mcpRequests")
    @Outgoing("mcpEvents")
    public Multi<McpMessage> requests(String str) {
        LOG.debugf("Received %s", str);
        JsonObject request = new JsonObject(str);
        JsonObject response;

        String method = request.getString("method");
        switch (method) {
            case "initialize": {
                response = Messages.newForInitialization(request.getInteger("id"));
                break;
            }
            case "notifications/initialized": {
                return Multi.createFrom().empty();
            }
            default: {
                response = null;
                break;
            }
        }

        McpMessage message = new McpMessage();
        message.event = "message";
        message.payload = response.toString();

        LOG.debugf("Replying with %s", message.payload);

        return Multi.createFrom().item(message);
    }
}
