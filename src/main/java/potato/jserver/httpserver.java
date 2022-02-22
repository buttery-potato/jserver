package potato.jserver;


import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
public class httpserver {
    public static DateTimeFormatter localTimeFormat;
    public static LocalDateTime currentTime;
    public static String databaseUsername;
    public static String databasePassword;
    public static String databaseAddress;
    public static String databaseName;
    public static void main(String args[]) throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        Properties config = new Properties();
        config.load(new FileInputStream("config/config"));
        databaseUsername = config.getProperty("database_username");
        databasePassword = config.getProperty("database_password");
        databaseAddress = config.getProperty("database_address");
        databaseName = config.getProperty("database_name");
        System.out.println(databaseUsername);
        System.out.println(databasePassword);
        System.out.println(databaseAddress);
        System.out.println(databaseName);
        InetSocketAddress serverAddress = new InetSocketAddress(80);
        HttpServer httpserver = HttpServer.create(serverAddress, 0);
        httpserver.createContext("/time/time", new TimeHttpHandler());
        httpserver.start();
        System.out.println("press enter to stop");
        BufferedReader inputreader = new BufferedReader(new InputStreamReader(System.in));
        while (inputreader.readLine().trim().equals("")) {
            System.out.println("server stopping!");
            httpserver.stop(1);
            System.out.println("server stopped");
            Runtime.getRuntime().exit(0);
        }
    }

    private static class TimeHttpHandler implements HttpHandler {
        Connection dbconnection = null;
        Statement sqlstmt = null;
        public TimeHttpHandler() {
            try {
                dbconnection = DriverManager.getConnection("jdbc:mysql://" + databaseUsername + ":" + databasePassword + "@" + databaseAddress + "/" + databaseName);
            } catch (SQLException exception) {
                System.out.println(exception.getMessage());
            }
        }
        @Override
        public void handle(HttpExchange exchange) { 
            try {
                localTimeFormat = DateTimeFormatter.ofPattern("h:m:s a, MMMM d, u");
                currentTime = LocalDateTime.now();
                byte[] formattedCurrentTime = currentTime.format(localTimeFormat).getBytes();
                exchange.sendResponseHeaders(200, 0);
                OutputStream os = exchange.getResponseBody();
                os.write(formattedCurrentTime);
                os.close();
                sqlstmt = dbconnection.createStatement();
                sqlstmt.execute("insert into connections values (\"" + exchange.getRemoteAddress().getAddress().getHostAddress() + "\", \"" + LocalDateTime.now().format(localTimeFormat) + "\");");
            } catch (IOException|SQLException ex) {
                System.out.println(ex.getMessage());
            }
        }
    }
}
