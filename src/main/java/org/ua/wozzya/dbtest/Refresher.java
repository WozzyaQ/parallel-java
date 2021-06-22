package org.ua.wozzya.dbtest;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Refresher {
    private static final String RECREATE_TABLE = "drop table if exists colorful_object;" +
            "create table if not exists colorful_object(id int, color int, version int);" +
            "insert into colorful_object (id, color, version) values (1,0,0);";

    public static void refresh(String url) {
        try(Connection con = DriverManager.getConnection(url);
            Statement st = con.createStatement();){
            st.executeUpdate(RECREATE_TABLE);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
