/*
 * DatabaseConnection.java - Establishes a connection to our AWS Relational Database.
 * 
 * Note: This project is built using Maven for dependency management.
 * 
 * To connect to the database and run the application, use the following command:
 * 
 * psql -h beautyinventorydb.chmaqmi0s40e.us-east-2.rds.amazonaws.com -U postgres -d postgres
 */

 package com.beautyinventory;

 import java.sql.Connection;
 import java.sql.DriverManager;
 import java.sql.SQLException;
 
 public class DatabaseConnection {
     private static final String URL = "jdbc:postgresql://beautyinventorydb.chmaqmi0s40e.us-east-2.rds.amazonaws.com:5432/postgres";
     private static final String USER = "postgres";  // AWS RDS username
     private static final String PASSWORD = "ADIrules1025!$";  // AWS RDS password
 
     private static Connection conn; // a single Connection instance
 
     public static Connection getConnection() {
         if (conn == null) {
             try {
                 conn = DriverManager.getConnection(URL, USER, PASSWORD);
                 System.out.println("Database connected successfully.");
                 if (conn.isClosed()) {
                     conn = DriverManager.getConnection(URL, USER, PASSWORD);
                 } 
                 return conn; 
             } catch (SQLException e) {
                 e.printStackTrace();
                 throw new RuntimeException("Error connecting to the database");
             }
         }
         return conn;
     }
 
     // to close the connection
     public static void closeConnection() {
         try {
             if (conn != null && !conn.isClosed()) {
                 conn.close();
                 System.out.println("\nDatabase connection closed.");
             }
         } catch (SQLException e) {
             e.printStackTrace();
         }
     }
 }