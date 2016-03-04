package database.operation;

import database.definition.Pampini_file;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import request_handler.JSON_fields;
import util.config;

import java.sql.*;

import static java.lang.System.exit;
import static main.Main.log;


public class Querry {
    //Validate login credentials
    public static boolean login(int uid, String phash) {
        try {
            Connection c = DriverManager.getConnection(config.jdbc, config.jdbc_username, config.jdbc_password);
            Statement stmt = c.createStatement();

            String sql = "set search_path to file;\n" +
                    "select phash from users where uid = " + uid + ")";

            ResultSet rs = stmt.executeQuery(sql);

            if (rs.getString("phash").matches(phash))
                return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    //Fetch file details from database
    public static Pampini_file get_file_details(int fid) {
        ResultSet rc;

        try {
            Connection c = DriverManager.getConnection(config.jdbc, config.jdbc_username, config.jdbc_password);
            Statement stmt = c.createStatement();

            String sql = String.format("set search_path to file;\nselect * from file where fif = %d;", fid);

            rc = stmt.executeQuery(sql);

            return new Pampini_file(rc.getInt("fid"), rc.getString("name"), rc.getInt("uploaderid"), rc.getDate("udate"), rc.getTime("utime"), rc.getInt("nsharer"), rc.getInt("ndloader"), rc.getInt("type"), rc.getLong("file_size"), rc.getLong("packet_size"), rc.getInt("no_packets"));

        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }

    }

    //Get JSON array of files available for downloading
    public static JSONArray get_downloadable_files(int page_number, int sort_type) {
        return null;
    }

    //Get best host to download one of the packets from JSON array 'arr'
    public static JSONObject get_host_for_packets(int fid, JSONArray arr) {
        try {
            Connection c = DriverManager.getConnection(config.jdbc, config.jdbc_username, config.jdbc_password);
            Statement stmt = c.createStatement();

            String sql = "set search_path to files;\n" +
                    "select * from (select partno, ip from ip where fid = " + fid + " and ip in (";

            for (int i = 0; i < arr.length(); i++)
                try {
                    sql = sql + arr.getJSONObject(i).toString();
                    if (i != arr.length() - 1)
                        sql = sql + ",";
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            sql = sql + ") ) as a natural join active_users order by no_conn limit 1;";

            ResultSet rs = stmt.executeQuery(sql);

            JSONObject ret = new JSONObject();

            ret.put(JSON_fields.To_send_data.uid, rs.getInt("uid"));
            ret.put(JSON_fields.To_send_data.IP, rs.getString("ip"));
            ret.put(JSON_fields.To_send_data.no_conn, rs.getInt("no_conn"));
            ret.put(JSON_fields.To_send_data.partno, rs.getInt("partno"));
            ret.put(JSON_fields.To_send_data.fid, fid);

            return ret;

        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }

    }

    //Get JSON array of files matching search_parameter
    public static JSONArray search(String search_parameter, int sort_type) {
        return null;
    }

    //Check for integrity of database and modify as needed
    public static void check_databases() {

        //Connecting to database
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            log("Database Check : Problems with JDBC, exiting.");
            e.printStackTrace();
            exit(1);
        }
        log("Database Check : JDBC Found.");

        Connection c = null;
        try {
            c = DriverManager.getConnection("jdbc:postgresql://localhost:5432/pampini", "pampini", "pampini");
        } catch (SQLException e) {
            log("Database Check : Unable to connect to local database, exiting.");
            e.printStackTrace();
            exit(1);
        }
        log("Database Check : Connected to local database.");

        //Creating statements
        Statement stmt = null;
        try {
            stmt = c.createStatement();
        } catch (SQLException e) {
            log("Database Check : Unable to create statement, exiting.");
            e.printStackTrace();
            exit(1);
        }

        //Setting search_path
        String sql = "set search_path to file;\n";
        try {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
            exit(1);
        }

        //For table availfid
        sql = "create table if not exists availfid\n" +
                "(fid integer primary key not null);";

        try {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
            exit(1);
        }

        for (int j = 0; j < 10; j++) {
            sql = "";
            for (int i = 0; i < 1000; i++)
                sql = sql + "insert into table availfid values(" + String.valueOf(j) + String.valueOf(i) + ");\n";
            try {
                stmt.executeUpdate(sql);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        //For table users
        try {
            sql = "create table if not exists users(\n" +
                    "uid            integer         primary key not null," +
                    "fname          varchar(30)     not null," +
                    "lname          varchar(30)     not null," +
                    "sex            char(1)         not null," +
                    "phash          VARCHAR(225)    not null," +
                    "jbatch         char(4)         not null," +
                    "branchcode     char(2)         not null," +
                    "dpiccode       integer         unique not NULL" +
                    ");";

            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        //For table files
        try {
            sql = "create table if not EXISTS files(\n" +
                    "fid            INTEGER         primary key not null," +
                    "uploaderid     integer         not null FOREIGN KEY REFERENCES(users.uid)," +
                    "udate          DATE            not null," +
                    "utime          TIME            not null," +
                    "nsharer        integer         not null," +
                    "ndloader       integer         not null," +
                    "ftype          SMALLINT        not null," +
                    "filesize       biginteger      not null," +
                    "pacetsize      biginteger      not null," +
                    "nopackets      INTEGER         not null," +
                    "fname          varchar(50)     not NULL" +
                    ");";

            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        //Not complete yet

    }

}