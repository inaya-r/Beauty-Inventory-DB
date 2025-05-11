// BrandDAO class - stores all APIs relating to Brands/Brand Tiers in our Beauty Inventory Database

package com.beautyinventory.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import com.beautyinventory.DatabaseConnection;

public class BrandDAO {

    private Connection conn;
    private PreparedStatement getBrandTierPerformanceStmt;

    public BrandDAO() {
        this.conn = DatabaseConnection.getConnection();
        prepareStatements(); 
    }

    /** 
     * Initializes necessary prepared statement (only 1 right now, can increase as Database is expanded
     * and more APIs are created)
     */
    private void prepareStatements() {
        try {
            String getBrandTierPerformanceQuery = "SELECT \n" + 
                "\tbrandTier,\n" +
                "\tCOALESCE(SUM(totalRevenue), 0) AS \"Total Revenue\",\n" +
                "\tCOALESCE(SUM(totalUnitsSold), 0) AS \"Total Units Sold\",\n" +
                "\tROUND(COALESCE(AVG(avgPricePerUnit), 0), 2) AS \"Average Price Per Unit\",\n" +
                "\tCOALESCE(SUM(totalRestocks), 0) AS \"Total Restocks\"\n" +
                "FROM (\n" +
                "\tSELECT \n" +
                "\t\tbt.type AS brandTier,\n" +
                "\t\tSUM(CASE WHEN st.transactionTypeID = 'S' THEN st.quantity * st.price ELSE 0 END) AS totalRevenue,\n" +
                "\t\tSUM(CASE WHEN st.transactionTypeID = 'S' THEN st.quantity ELSE 0 END) AS totalUnitsSold,\n" +
                "\t\tAVG(CASE WHEN st.transactionTypeID = 'S' THEN st.price ELSE NULL END) AS avgPricePerUnit,\n" +
                "\t\tCOUNT(DISTINCT CASE WHEN st.transactionTypeID = 'R' THEN st.id ELSE NULL END) AS totalRestocks\n" +
                "\tFROM Products p\n" +
                "\tJOIN Brand b ON p.brandID = b.ID\n" +
                "\tJOIN BrandTier bt ON b.brandTierID = bt.ID\n" +
                "\tLEFT JOIN StockTransactions st ON p.ID = st.productID\n" +
                "\tWHERE st.timestamp BETWEEN ? AND ?\n" +
                "\tGROUP BY bt.type\n" +
                ") AS BrandPerformance\n" +
                "GROUP BY brandTier;";

            // Prepare statement ONLY once
            getBrandTierPerformanceStmt = conn.prepareStatement(getBrandTierPerformanceQuery);

        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Error preparing SQL statements.");
        }
    }

/* ---------------------------------------------------------------------------------------------------------
 * INAYA'S 2ND API (other in ProductDAO.java)
 * ---------------------------------------------------------------------------------------------------------
*/
    /** getBrandTierPerformance
     * Fetches brand tier performance data within a given date range.
     * - param: DATE startDate (start date of analysis), DATE endDate (end date of analysis)
     * - returns: list of formatted performance records for both brand tiers for easy comparison
     */
    public List<String> getBrandTierPerformance(Date startDate, Date endDate) {
        List<String> performanceResults = new ArrayList<>();

        try {
            // Set parameters
            getBrandTierPerformanceStmt.setDate(1, startDate);
            getBrandTierPerformanceStmt.setDate(2, endDate);

            // Execute query
            ResultSet rs = getBrandTierPerformanceStmt.executeQuery();

            // Column Widths
            int brandTierWidth = 12;
            int revenueWidth = 18;
            int unitsSoldWidth = 20;
            int avgPriceWidth = 25;
            int restocksWidth = 18;

            // Calculate separator length dynamically
            int SEPARATOR_LENGTH = brandTierWidth + revenueWidth + unitsSoldWidth + avgPriceWidth + 
                                    restocksWidth + 10; 

            // Print table header
            System.out.printf("%-" + brandTierWidth + "s %" + revenueWidth + "s %" + unitsSoldWidth + 
                                "s %" + avgPriceWidth + "s %" + restocksWidth + "s%n",
                                    "Brand Tier", "Total Revenue", "Units Sold", "Avg Price Per Unit", 
                                        "Total Restocks");

            // Print separator line
            for (int i = 0; i < SEPARATOR_LENGTH; i++) { System.out.print("-"); }
            System.out.println();

            // Process query results
            while (rs.next()) {
                String row = String.format("%-" + brandTierWidth + "s %" + revenueWidth + ".2f %" + 
                                unitsSoldWidth + "d %" + avgPriceWidth + ".2f %" + restocksWidth + "d",
                        rs.getString("brandTier"),
                        rs.getDouble("Total Revenue"),
                        rs.getInt("Total Units Sold"),
                        rs.getDouble("Average Price Per Unit"),
                        rs.getInt("Total Restocks")
                );
                System.out.println(row);
                performanceResults.add(row);
            }

            rs.close(); // Close ResultSet

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return performanceResults;
    }

//---------------------------------------------------------------------------------------------------------------------
    
    /**
     * Closes all prepared statements and releases database resources.
     */
    public void close() {
        try {
            if (getBrandTierPerformanceStmt != null) {
                getBrandTierPerformanceStmt.close();
            }
            if (conn != null) {
                conn.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Main method for testing
     */
    public static void main(String[] args) {
        System.out.println("Testing BrandDAO...");

        // Instantiate DAO
        BrandDAO brandDAO = new BrandDAO();

        // Example input parameters
        Date startDate = Date.valueOf("2025-01-01");
        Date endDate = Date.valueOf("2025-03-13");

        // Execute query
        List<String> performanceData = brandDAO.getBrandTierPerformance(startDate, endDate);

        // Display results
        if (performanceData.isEmpty()) {
            System.out.println("No data retrieved.");
        } else {
            System.out.println("\nRetrieved Performance Data.");
        }

        // Close resources
        brandDAO.close();
    }
}
