package org.ua.wozzya.dbtest;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class TestConnection {
    private static final String URL = "jdbc:postgresql://localhost:12345/test?user=vlad&password=vlad";

    public static void main(String[] args) throws SQLException {
        Connection con = DriverManager.getConnection(URL);
        Statement st = con.createStatement();
        st.executeUpdate("insert into color (color) values (43)");
        con.close();
    }
}
