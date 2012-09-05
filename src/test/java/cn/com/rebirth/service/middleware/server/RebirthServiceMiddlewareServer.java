package cn.com.rebirth.service.middleware.server;

import java.io.IOException;

public class RebirthServiceMiddlewareServer {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		System.setProperty("search.core.discovery.initial_state_timeout", "5m");
		System.setProperty("rebirth.search.server.cluster", "rebirth-search-server-cluster");
		System.setProperty("rebirth.service.middleware.bootstrap.mlockall", "true");
		System.setProperty("zk.zkConnect", "192.168.2.179:2181");
		Bootstrap.main(args);
		if (System.in.read() != 0) {
			Bootstrap.close(args);
		}
	}

}
