package com.beautyinventory;

import org.junit.jupiter.api.Test;
import java.sql.Connection;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DatabaseConnectionTest {

    @Test  
    void testDatabaseConnection() {
        Connection conn = DatabaseConnection.getConnection();
        assertNotNull(conn, "Database connection should not be null");
        System.out.println("Successfully connected to AWS RDS!");
    }
}