package potato.potato.jserver;


import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.time.*;
import java.time.format.*;
import java.sql.*;
import java.util.*;
public class httpserver {
    public static DateTimeFormatter localTimeFormat;
    public static LocalDateTime currentTime;
    public static String databaseUsername;
    public static String databasePassword;
    public static String databaseAddress;
    public static String databaseName;
    /*
    @author potato
    @param databaseUsername username to connect to database with
    @param databasePassword password to go along with username
    @param databaseAddress ip address of database, or localhost
    @param databaseName name of database to connect with
    @text
    */
    public static void main(String args[]) throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        /*
        param format: databaseUsername databasePassword databaseAddress databaseName
        */
        Class.forName("com.mysql.cj.jdbc.Driver");
        Properties config = new Properties();
        config.load(new FileInputStream("config/config"));
        databaseUsername = config.getProperty("database_username", "none");
        databasePassword = config.getProperty("database_password", "none");
        databaseAddress = config.getProperty("database_address", "none");
        databaseName = config.getProperty("database_name", "none");
        if (databaseUsername.equals("none")) {
            databaseUsername = args[0];
        }
        if (databasePassword.equals("none")) {
            databasePassword = args[1];
        }
        if (databaseAddress.equals("none")) {
            databaseAddress = args[2];
        }
        if (databaseName.equals("none")) {
            databaseName = args[3];
        }
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
