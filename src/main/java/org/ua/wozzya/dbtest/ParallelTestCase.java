package org.ua.wozzya.dbtest;


import java.net.Proxy;
import java.sql.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class ParallelTestCase {
    private static final String QUERY_FOR_ROW = "select color from colorful_object as co where co.id = 1";
    private static final String QUERY_FOR_ROW_AND_VERSION = "select color, version from colorful_object as co where co.id = 1";

    private static final String UPDATE_ROW_VALUE = "update colorful_object set color=? where id=1";
    private static final String UPDATE_VALUE_AND_VERSION = "update colorful_object set color =?, version=? where id = 1 and version=?";

    private static void runExecutors(int workers, int queries, Runnable action) {
        ExecutorService ex = Executors.newFixedThreadPool(workers);
        for (int i = 0; i < queries; ++i) {
            ex.execute(action);
        }

        ex.shutdown();
        try {
            ex.awaitTermination(10, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static Runnable noLocking(Supplier<Connection> connectionSupplier) {
        return () -> {
            try (Connection con = connectionSupplier.get();
                 Statement selectStatement = con.createStatement()) {
                ResultSet rs = selectStatement.executeQuery(QUERY_FOR_ROW);
                if (rs.next()) {
                    int colorNumber = rs.getInt(1);
                    try (PreparedStatement st = con.prepareStatement(UPDATE_ROW_VALUE)) {
                        st.setInt(1, colorNumber + 1);
                        st.execute();
                    }
                }
                rs.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        };
    }

    private static Runnable noLockingTransactional(Supplier<Connection> connectionSupplier) {
        return () -> {
            while (true) {
                try (Connection con = connectionSupplier.get();
                     Statement selectStatement = con.createStatement()) {
                    ResultSet rs = selectStatement.executeQuery(QUERY_FOR_ROW);
                    if (rs.next()) {
                        int colorNumber = rs.getInt(1);
                        try (PreparedStatement st = con.prepareStatement(UPDATE_ROW_VALUE)) {
                            st.setInt(1, colorNumber + 1);
                            st.execute();
                        }
                    }
                    rs.close();
                    con.commit();
                } catch (SQLException e) {
                    e.printStackTrace();
                    continue;
                }
                break;
            }
        };
    }

    public static Runnable optimisticLockingWithVersioning(Supplier<Connection> connectionSupplier) {
        return () -> {
            onFail:
            while (true) {
                try (Connection con = connectionSupplier.get();
                     Statement st = con.createStatement();
                     ResultSet rs = st.executeQuery(QUERY_FOR_ROW_AND_VERSION)) {

                    if (rs.next()) {
                        int colorNumber = rs.getInt(1);
                        int versionNumber = rs.getInt(2);
                        try (PreparedStatement ps = con.prepareStatement(UPDATE_VALUE_AND_VERSION)) {
                            int k = 0;
                            ps.setInt(++k, colorNumber + 1);
                            ps.setInt(++k, versionNumber + 1);
                            ps.setInt(++k, versionNumber);
                            if (ps.executeUpdate() > 0) {
                                break;
                            } else {
                                continue onFail;
                            }
                        }
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        };
    }


    public static Connection getConnection(String url) {
        try {
            return DriverManager.getConnection(url);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

        // TOO BAD!
        return null;
    }
    public static Supplier<Connection> driverSupplier(String url) {
        return () -> getConnection(url);
    }

    public static Supplier<Connection> transactionalSupplier(String url, int transactionIsolationLevel) {
        return () -> {
            Connection con = driverSupplier(url).get();
            try {
                con.setAutoCommit(false);
                con.setTransactionIsolation(transactionIsolationLevel);
                return con;
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }

            // TOO BAD!!!
            return null;
        };
    }

    private static Supplier<Connection> singleConnection(Connection connection) {
        return staticSingleConnection(connection, false);
    }

    private static Supplier<Connection> staticSingleConnection(Connection con, boolean transactional) {
        return () -> new ProxyConnection(con, transactional);
    }



    public static void main(String[] args) throws SQLException {

//        System.out.println("starting no locking + transactional");
//        Refresher.refresh(URLS.POSTGRES_URL);
//        runExecutors(
//                16,10,
//                noLockingTransactional(transactionalSupplier(URLS.POSTGRES_URL, Connection.TRANSACTION_READ_COMMITTED))
//        );


//        System.out.println("starting no locking");
//        Refresher.refresh(URLS.POSTGRES_URL);
//        runExecutors(
//                16,10,
//                noLocking(driverSupplier(URLS.POSTGRES_URL))
//        );


        System.out.println("starting optimistic");
        Refresher.refresh(URLS.POSTGRES_URL);
        runExecutors(
                16,1000,
                optimisticLockingWithVersioning(driverSupplier(URLS.POSTGRES_URL))
        );


//        Connection con = DriverManager.getConnection(URLS.POSTGRES_URL);
//
//        System.out.println("Running noLocking w/ single connection");
//        Refresher.refresh(URLS.POSTGRES_URL);
//        runExecutors(
//                16, 100,
//                noLocking(singleConnection(con))
//        );
//
//        System.out.println("Running optimistic w/ single connection");
//        Refresher.refresh(URLS.POSTGRES_URL);
//        runExecutors(
//                16, 100,
//                optimisticLockingWithVersioning(singleConnection(con))
//        );
//
//        con.close();
    }
}
