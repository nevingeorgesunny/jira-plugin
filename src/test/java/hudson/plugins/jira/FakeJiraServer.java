package hudson.plugins.jira;

/**
 * Author: Nevin Sunny
 */

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import net.sf.json.JSONObject;

public class FakeJiraServer {

	private HttpServer server;
	private URI serverUri;
	private String pwdCollected;

	public FakeJiraServer(InetSocketAddress address) throws IOException {
		server = HttpServer.create(address, 0);
		server.createContext("/", this::jiraServerHandler);
	}

	public void start() {
		server.start();
		serverUri =
				URI.create("http://" + server.getAddress().getHostString() + ":" + server.getAddress().getPort() + "/");
	}

	public void stop() {
		server.stop(0);
	}

	public URI getServerUri() {
		return serverUri;
	}

	public String getPasswordAndReset() {
		String result = pwdCollected;
		this.pwdCollected = null;
		return result;
	}

	private  void jiraServerHandler(HttpExchange he) throws IOException {
		String relativePath = he.getRequestURI().getPath();

		String authBasicBase64 = he.getRequestHeaders().getFirst("Authorization");
		String authBase64 = authBasicBase64.substring("Basic ".length());
		String auth = new String(Base64.getDecoder().decode(authBase64), StandardCharsets.UTF_8);
		String[] authArray = auth.split(":");
		String user = authArray[0];
		String pwd = authArray[1];

		pwdCollected = pwd;

		if ("/rest/api/latest/mypermissions".equals(relativePath)) {
			myPermissions(he);
		}
	}

	private void myPermissions(HttpExchange exchange) throws IOException {
		Map<String, Object> permissions = new HashMap<>();
		Map<String, Object> perm1 = new HashMap<>();
		perm1.put("id", 1);
		perm1.put("key", "perm_key");
		perm1.put("name", "perm_name");
		perm1.put("description", null);
		perm1.put("havePermission", "true");

		permissions.put("perm_1", perm1);

		Map<String, Object> body = new HashMap<>();
		body.put("permissions", permissions);

		String response = JSONObject.fromObject(body).toString();
		exchange.sendResponseHeaders(200, response.getBytes().length);
		try (OutputStream os = exchange.getResponseBody()) {
			os.write(response.getBytes());
		}
	}
}
