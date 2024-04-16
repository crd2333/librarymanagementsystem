import utils.ConnectConfig;
import utils.DatabaseConnector;

import java.util.logging.Logger;
// add
import java.net.InetSocketAddress;
import com.sun.net.httpserver.HttpServer;

public class Main {
    private static final Logger log = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        DatabaseConnector connector = null;
        HttpServer server = null;

        try {
            // parse connection config from "resources/application.yaml"
            ConnectConfig conf = new ConnectConfig();
            log.info("Success to parse connect config. " + conf.toString());
            // connect to database
            connector = new DatabaseConnector(conf);
            boolean connStatus = connector.connect();
            if (!connStatus) {
                log.severe("Failed to connect database.");
                System.exit(1);
            }
            LibraryManagementSystem library = new LibraryManagementSystemImpl(connector);
            System.out.println("Successfully init class library.");

            // 创建 HTTP 服务器，监听指定端口
            server = HttpServer.create(new InetSocketAddress(8000), 0);
            // 添加 handler
            server.createContext("/card", new CardHandler(library));
            server.createContext("/book", new BookHandler(library));
            server.createContext("/borrow", new BorrowHandler(library));
            // 启动服务器
            server.start();
            System.out.println("Server is listening on port 8000");

        } catch (Exception e) {
            e.printStackTrace();
        }

        // 关闭钩子
        final DatabaseConnector finalConnector = connector;
        final HttpServer finalServer = server;
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                // shutdown server
                finalServer.stop(0);
                System.out.println("Server stopped.");
                // release database connection handler
                if (finalConnector != null && finalConnector.release()) {
                    log.info("Success to release connection.");
                    System.out.println("Success to release connection.");
                } else {
                    log.severe("Failed to release connection.");
                    System.out.println("Failed to release connection.");
                }
                System.out.println("Bye.");
            }
        });
    }
}


