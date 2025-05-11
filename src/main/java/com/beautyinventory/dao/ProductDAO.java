//product 
package com.beautyinventory.dao;

import com.beautyinventory.DatabaseConnection;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProductDAO {
    private Connection conn;

    // Constructor that takes in Connection param
    public ProductDAO() {
        this.conn = DatabaseConnection.getConnection();

    }

    public static void main(String[] args) {
        System.out.println("Testing Product DAO methods...");

        // Instantiate DAO
        ProductDAO product = new ProductDAO();
        System.out.println("Testing Product DAO methods...");

        // Testing the getProductInventoryByProduct
        product.getProductInventoryByProductType("SK");


        // // Test the getExampleData() method
        List<String> test_product = product.getAllProductInventory();

        // Display results
        if (test_product.isEmpty()) {
            System.out.println("Finished.");
        } else {
            System.out.println("Retrieved data:");
            for (String item : test_product) {
                System.out.println("- " + item);
            }
        }

        List<String> test_productsByBrandTierA = product.getProductsByBrandTier('A');
        if (test_productsByBrandTierA.isEmpty()) {
            System.out.println("No products found for affordable brand tier.");
        }

        List<String> test_productsByBrandTierL = product.getProductsByBrandTier('L');
        if (test_productsByBrandTierL.isEmpty()) {
            System.out.println("No products found for luxury brand tier.");
        }

        // testing suggestRestockLevels

        List<String> restockLevels = product.suggestRestockLevels(); // Get restocks for Luxury brands in last 60 days

        // Display results
        if (restockLevels.isEmpty()) {
            System.out.println("Finished.");
        } else {
            System.out.println("Retrieved data:");
        }

        // Testing update prices API
        product.updateCostAndSellingPrice(0, "HSC");
        product.updateCostAndSellingPrice(0, "LBS");
        product.updateCostAndSellingPrice(0, "EFB");
        product.updateCostAndSellingPrice(0, "NGP");
        product.updateCostAndSellingPrice(0, "ABL");
        product.updateCostAndSellingPrice(0, "RSS");
        product.updateCostAndSellingPrice(0, "BCI");


    }

//-----------------------------------------------------------------------------------------------------------------

    // CRUP DATE API - Ayesha
    // Function with query that adds/inserts transaction(sale, restock, and
    // adjustment) and also does update on current inventory based on what
    // transaction occured
    public void insertTransactionAndShowUpdatedInventory(int sku, int quantity, String transactionType) {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        int productId = -1;
        Double sellingPrice = null; 

        try {
            conn.setAutoCommit(false);

            // Get the product ID
            String productIdQuery = "SELECT id, currentSellingPrice FROM Products WHERE sku = ?";
            stmt = conn.prepareStatement(productIdQuery);
            stmt.setInt(1, sku);
            rs = stmt.executeQuery();
            if (rs.next()) {
                productId = rs.getInt("id");
                if ("S".equals(transactionType)) {
                    sellingPrice = rs.getDouble("currentSellingPrice"); // Fetch selling price for sales
                }
            } else {
                System.out.println("Error: Product with SKU " + sku + " not found.");
                return;
            }
            rs.close();
            stmt.close();

            // Insert transaction & get inserted row
            String insertQuery = "INSERT INTO StockTransactions (productID, quantity, timestamp, price, transactionTypeID) "
                            + "VALUES (?, ?, NOW(), ?, ?) RETURNING id, quantity, timestamp, price, transactionTypeID";

            stmt = conn.prepareStatement(insertQuery);
            stmt.setInt(1, productId);
            stmt.setInt(2, quantity);
            
            // Set price for sales, NULL for restocks/adjustments
            if ("S".equals(transactionType)) {
                stmt.setDouble(3, sellingPrice);
            } else {
                stmt.setNull(3, Types.DOUBLE);
                stmt.setNull(3, Types.DOUBLE);
            }

            stmt.setString(4, transactionType);

            // Execute & get inserted row
            rs = stmt.executeQuery();
            if (rs.next()) {
                // Get transaction details
                int insertedQuantity = rs.getInt("quantity");
                Timestamp timestamp = rs.getTimestamp("timestamp");
                String formattedTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(timestamp);
                
                // Determine transaction type label (Java 8/11 compatible switch statement)
                String transactionLabel;
                String additionalMessage = ""; 
                switch (transactionType) {
                    case "S":
                        transactionLabel = "sale";
                        additionalMessage = String.format(" for $%.2f each", sellingPrice);
                        break;
                    case "R":
                        transactionLabel = "restock";
                        break;
                    case "A":
                        transactionLabel = "adjustment";
                        break;
                    default:
                        transactionLabel = "transaction";
                }

                // Print formatted message
                if (transactionLabel == "restock" || transactionLabel == "adjustment") {
                    System.out.println("\nNew " + transactionLabel + " of " + insertedQuantity + " inserted at " + formattedTime);
                } else {
                    System.out.println("\nNew " + transactionLabel + " of " + insertedQuantity + " inserted at " + formattedTime + additionalMessage); 
                }
            }

            conn.commit(); // Commit only if successful

        } catch (SQLException e) {
            e.printStackTrace();
            try {
                conn.rollback();
                System.out.println("Transaction rolled back.");
            } catch (SQLException rollbackEx) {
                rollbackEx.printStackTrace();
            }
        } finally {
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                conn.setAutoCommit(true);
            } catch (SQLException closeEx) {
                closeEx.printStackTrace();
            }
        }
    }

    
    /* public void insertTransactionAndShowUpdatedInventory(int sku, int quantity, double price,String transactionType) {
        Statement stmt = null;
        ResultSet rs = null;
        int productId = -1;

        try {
            conn.setAutoCommit(false);
            String productIdQuery = "SELECT id FROM Products WHERE sku = " + sku;
            stmt = conn.createStatement();
            rs = stmt.executeQuery(productIdQuery);
            if (rs.next()) {
                productId = rs.getInt("id");
            } else {
                System.out.println("Error: Product with SKU " + sku + " not found.");
                return;
            }
            rs.close();

            String selectQuery = "SELECT Products.sku, Products.name, Brand.name AS brand_name, BrandTier.type, " +
                    "ProductType.name AS product_type, " +
                    "COALESCE(SUM(CASE " +
                    "WHEN st.transactiontypeID = 'R' THEN st.quantity " +
                    "WHEN st.transactiontypeID = 'S' THEN -st.quantity " +
                    "WHEN st.transactiontypeID = 'A' THEN st.quantity " +
                    "ELSE 0 END), 0) AS currentInventory, " +
                    "Products.reorderThreshold, Products.maxStockCapacity, Products.isActive " +
                    "FROM Products " +
                    "JOIN Brand ON Brand.id = Products.brandID " +
                    "JOIN BrandTier ON BrandTier.id = Brand.brandtierID " +
                    "JOIN ProductType ON ProductType.id = Products.productTypeID " +
                    "JOIN StockTransactions st ON Products.id = st.productID " +
                    "WHERE Products.sku = '" + sku + "' " +
                    "GROUP BY Products.sku, Products.name, Brand.name, BrandTier.type, ProductType.name, " +
                    "Products.reorderThreshold, Products.maxStockCapacity, Products.isActive " +
                    "ORDER BY Products.sku";

            stmt = conn.createStatement();
            rs = stmt.executeQuery(selectQuery);

            // Display the current product details
            if (rs.next()) {
                System.out.println("\n-------------------------------------------------------");
                System.out.println("Current Product Inventory:\n");
                System.out.println("SKU: " + rs.getString("sku"));
                System.out.println("Name: " + rs.getString("name"));
                System.out.println("Brand: " + rs.getString("brand_name"));
                System.out.println("Brand Tier: " + rs.getString("type"));
                System.out.println("Product Type: " + rs.getString("product_type"));
                System.out.println("Current Inventory: " + rs.getInt("currentInventory"));
                System.out.println("Reorder Threshold: " + rs.getInt("reorderThreshold"));
                System.out.println("Max Stock Capacity: " + rs.getInt("maxStockCapacity"));
                System.out.println("Active: " + rs.getBoolean("isActive"));
                System.out.println("\n-------------------------------------------------------");
            }

            // Insert transaction using the retrieved product ID
            String insertTransactionQuery = "INSERT INTO StockTransactions (productID, quantity, timestamp, price, transactionTypeID) "
                    + "VALUES (" + productId + ", " + quantity + ", NOW(), "
                    + (price >= 0 ? price : "NULL") + ", '" + transactionType + "')";

            stmt.executeUpdate(insertTransactionQuery);
            System.out.println("Stock transaction inserted successfully.");
            System.out.println("\n-------------------------------------------------------");

            // Get the updated product details after the transaction
            rs = stmt.executeQuery(selectQuery);

            // Display the updated product details
            if (rs.next()) {
                System.out.println("\n-------------------------------------------------------");
                System.out.println("\nUpdated Product Inventory:\n");
                System.out.println("SKU: " + rs.getString("sku"));
                System.out.println("Name: " + rs.getString("name"));
                System.out.println("Brand: " + rs.getString("brand_name"));
                System.out.println("Brand Tier: " + rs.getString("type"));
                System.out.println("Product Type: " + rs.getString("product_type"));
                System.out.println("Current Inventory: " + rs.getInt("currentInventory"));
                System.out.println("Reorder Threshold: " + rs.getInt("reorderThreshold"));
                System.out.println("Max Stock Capacity: " + rs.getInt("maxStockCapacity"));
                System.out.println("Active: " + rs.getBoolean("isActive"));
                System.out.println("\n-------------------------------------------------------");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                // Roll back to ensure no changes are committed
                conn.rollback();
                System.out.println("Transaction rolled back.");
            } catch (SQLException rollbackEx) {
                rollbackEx.printStackTrace();
            }

            // Cleaning up resources
            try {
                if (rs != null) {
                    rs.close();
                }
                if (stmt != null) {
                    stmt.close();
                }
                // Enabling auto-commit again after rollback
                conn.setAutoCommit(true);
            } catch (SQLException closeEx) {
                closeEx.printStackTrace();
            }
        }
    } */
//-----------------------------------------------------------------------------------------------------------------

    // LIST API - Ayesha
    // Method to get product total inventory and other product information filtered
    // by product type
    public void getProductInventoryByProductType(String productTypeID) {
        // Simplified query with user input for productTypeID
        String query = "SELECT " +
                "Products.sku, " +
                "Products.name, " +
                "Products.description, " +
                "Products.currentSellingPrice, " +
                "Brand.name AS brand_name, " +
                "BrandTier.type, " +
                "Products.maxStockCapacity, " +
                "Products.reorderThreshold, " +
                "Products.isActive, " +
                "ProductType.name AS product_type, " +
                "COALESCE(SUM(CASE " +
                "WHEN StockTransactions.transactiontypeID = 'R' THEN StockTransactions.quantity " +
                "WHEN StockTransactions.transactiontypeID = 'S' THEN -StockTransactions.quantity " +
                "WHEN StockTransactions.transactiontypeID = 'A' THEN StockTransactions.quantity " +
                "ELSE 0 END), 0) AS currentInventory " +
                "FROM Products " +
                "JOIN Brand ON Brand.id = Products.brandID " +
                "JOIN BrandTier ON BrandTier.id = Brand.brandtierID " +
                "JOIN ProductType ON ProductType.id = Products.productTypeID " +
                "LEFT JOIN StockTransactions ON Products.id = StockTransactions.productID " +
                "WHERE Products.productTypeID = '" + productTypeID + "' " +
                "GROUP BY Products.sku, Products.name, Products.description, " +
                "Products.currentSellingPrice, Brand.name, BrandTier.type, " +
                "ProductType.name, Products.reorderThreshold, " +
                "Products.maxStockCapacity, Products.isActive " +
                "ORDER BY Products.sku;";

        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);

            // Column Widths
            int skuWidth = 6;
            int nameWidth = 25;
            int priceWidth = 15;
            int brandWidth = 22;
            int brandTierWidth = 10;
            int maxStockWidth = 18;
            int reorderWidth = 18;
            int isActiveWidth = 10;
            int inventoryWidth = 18;

            // Calculating separator length dynamically
            int SEPARATOR_LENGTH = skuWidth + nameWidth + priceWidth + brandWidth +
                    brandTierWidth + maxStockWidth + reorderWidth + isActiveWidth + inventoryWidth
                    + 10;

            // Printing table header
            System.out.printf(
                    "%-" + skuWidth + "s %-" + nameWidth + "s %-" + priceWidth + "s %-" +
                            brandWidth + "s %-" + brandTierWidth + "s %" + maxStockWidth + "s %" +
                            reorderWidth + "s %-" + isActiveWidth + "s %-" + inventoryWidth
                            + "s%n",
                    "SKU", "Name", "Price", "Brand", "Brand Tier",
                    "Max Stock", "Reorder Thresh", "Active", "Inventory");

            // Printing separator line
            for (int i = 0; i < SEPARATOR_LENGTH; i++) {
                System.out.print("-");
            }
            System.out.println();

            // Printing each row
            while (rs.next()) {
                String row = String.format(
                        "%-" + skuWidth + "s %-" + nameWidth + "s %-" + priceWidth + ".2f %-" +
                                brandWidth + "s %-" + brandTierWidth + "s %" + maxStockWidth + "d %" +
                                reorderWidth + "d %-" + isActiveWidth + "s %-"
                                + inventoryWidth + "d%n",
                        rs.getString("sku"),
                        rs.getString("name").length() > nameWidth
                                ? rs.getString("name").substring(0, nameWidth - 3) + "..."
                                : rs.getString("name"),
                        rs.getDouble("currentSellingPrice"),
                        rs.getString("brand_name"),
                        rs.getString("type"),
                        rs.getInt("maxStockCapacity"),
                        rs.getInt("reorderThreshold"),
                        rs.getBoolean("isActive") ? "Yes" : "No",
                        rs.getInt("currentInventory"));

                System.out.print(row);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

//-----------------------------------------------------------------------------------------------------------------

    /* LIST API - getAllProductInventory()
     * Author: Inaya Rizvi 
     * - params: none.
     * - fetches all product inventory data from database and returns it in table format.
     * - returns: SKU, name, brand, current inventory, reorder threshold, max stock capacity, 
     *            and active status (if it's currently being sold or not) for all products in system. 
     */
    public List<String> getAllProductInventory() {
        List<String> inventory = new ArrayList<>();
        
        // Query to retrieve all product inventory data from database, now including Price
        String query = "SELECT \n" +
                "    p.sku AS \"SKU\", \n" +
                "    p.name AS \"Product\", \n" +
                "    p.currentSellingPrice AS \"Price\", \n" +  // Added price column
                "    b.name AS \"Brand\", \n" +
                "    COALESCE( \n" +
                "        SUM( \n" +
                "            CASE \n" +
                "                WHEN st.transactiontypeID = 'R' THEN st.quantity  -- Restock adds \n" +
                "                WHEN st.transactiontypeID = 'S' THEN -st.quantity -- Sale subtracts \n" +
                "                WHEN st.transactiontypeID = 'A' THEN st.quantity  -- Adjustment either way \n" +
                "                ELSE 0 \n" +
                "            END \n" +
                "        ), 0) AS \"Current Inventory\",  -- Default to 0 if no transactions exist \n" +
                "    p.reorderThreshold AS \"Reorder Threshold\", \n" +
                "    p.maxStockCapacity AS \"Max Capacity\", \n" +
                "    p.isActive AS \"Is Active\" " +
                "FROM Products p \n" +
                "    JOIN Brand b ON (b.id = p.brandid) \n" +
                "    JOIN BrandTier bt ON (bt.id = b.brandtierID) \n" +
                "    JOIN ProductType pt ON (pt.id = p.producttypeID) \n" +
                "    LEFT JOIN StockTransactions st ON (p.id = st.productID)  -- Allow products with no transactions \n" +
                "GROUP BY p.id, p.sku, p.name, p.currentSellingPrice, b.name, p.reorderThreshold, p.maxStockCapacity, p.isActive \n" +  // Added p.price to GROUP BY
                "ORDER BY p.sku;";
    
        try (
            PreparedStatement stmt = conn.prepareStatement(query);
            ResultSet rs = stmt.executeQuery()) {
    
            // Column Widths
            int skuWidth = 6;
            int productWidth = 30;
            int priceWidth = 7; 
            int brandWidth = 23;
            int inventoryWidth = 16;
            int reorderWidth = 18;
            int stockCapacityWidth = 18;
            int isActiveWidth = 10;
    
            // Calculate separator length dynamically
            int SEPARATOR_LENGTH = skuWidth + productWidth + priceWidth + brandWidth + inventoryWidth + reorderWidth + stockCapacityWidth + isActiveWidth + 12; // Extra padding
    
            // Print table header
            System.out.printf(
                    "\n%-" + skuWidth + "s %-" + productWidth + "s %-" + priceWidth + "s %-" + brandWidth + "s %-" + inventoryWidth + "s %" + reorderWidth + "s %"
                            + stockCapacityWidth + "s %-" + isActiveWidth + "s%n",
                    "SKU", "Product", "Price", "Brand", "Current Inventory", "Reorder Threshold", "Max Stock Capacity", "Is Active");
    
            // Print separator line
            for (int i = 0; i < SEPARATOR_LENGTH; i++) {
                System.out.print("-");
            }
            
            System.out.println();
    
            // Print each row
            while (rs.next()) {
                System.out.printf(
                        "%-" + skuWidth + "s %-" + productWidth + "s %" + priceWidth + ".2f %-" + brandWidth + "s %" + inventoryWidth + "d %" + reorderWidth + "d %"
                                + stockCapacityWidth + "d %" + isActiveWidth + "s%n",
                        rs.getString("SKU"),
                        rs.getString("Product").length() > productWidth
                                ? rs.getString("Product").substring(0, productWidth - 3) + "..."
                                : rs.getString("Product"),
                        rs.getDouble("Price"),  // Display price with 2 decimal places
                        rs.getString("Brand"),
                        rs.getInt("Current Inventory"),
                        rs.getInt("Reorder Threshold"),
                        rs.getInt("Max Capacity"),
                        rs.getBoolean("Is Active") ? "Yes" : "No");
            }
            System.out.println();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return inventory;
    }
    

//-----------------------------------------------------------------------------------------------------------------
    
    /**
     * LIST API
     * Author : Dulguun Delgerbat
     * Retrieves and displays products based on brand tier (Affordable 'A' or Luxury
     * 'L'),
     * calculating the current inventory level for each product.
     *
     * <p>
     * The results are formatted into a table and returned as a list of product
     * details,
     * including SKU, name, brand, type, price, inventory, reorder threshold,
     * max stock capacity, and active status.
     * </p>
     *
     * @param brandTierID Brand tier identifier ('A' for Affordable, 'L' for
     *                    Luxury).
     * @return List of formatted strings containing product details.
     */
    public List<String> getProductsByBrandTier(char brandTierID) {
        // displays the selected brand tier at the top of the output
        
        String output = ""; 
        if (brandTierID == 'L') {output = "Luxury";}
        else if (brandTierID == 'A') {output = "Affordable";}
        else output = "NA - invalid entry."; 
        String title = String.format("\n\n\t\t\t PRODUCTS WITH BRAND TIER: %s", output);
        
        System.out.println(title);

        // list to hold product details
        List<String> productsByBrandTier = new ArrayList<>();

        // SQL query to retrieve product information along with the calculated current inventory
        String query = "SELECT P.SKU AS \"SKU\", P.name AS \"Product\", B.name as \"Brand\", Pt.id as \"Product Type\", "
                + "P.currentSellingPrice AS \"Selling Price\", " +
                "\tCOALESCE(\n" + //
                "\t\tSUM(\n" + //
                "\t\t\tCASE\n" + //
                "\t\t\t\tWHEN ST.transactiontypeID = 'R' THEN ST.quantity -- restock adds\n" + //
                "\t\t\t\tWHEN ST.transactiontypeID = 'S' THEN -ST.quantity -- sale subtracts\n" + //
                "\t\t\t\tWHEN ST.transactiontypeID = 'A' THEN ST.quantity -- adjustment either\n" + //
                "\t\t\t\tELSE 0\n" + //
                "\t\t\tEND\n" + //
                "\t\t), 0) as currentInventory,\n"
                + "P.reorderThreshold AS \"Reorder Threshold\", "
                + "P.maxStockCapacity AS \"Max Stock Capacity\", "
                + "P.isActive AS \"Is Active\" "
                + "FROM Products P "
                + "JOIN Brand AS B ON (B.ID = P.brandID) "
                + "JOIN BrandTier AS BT ON (BT.ID = B.brandTierID) "
                + "JOIN ProductType AS PT ON (PT.ID = P.productTypeID) "
                + "LEFT JOIN StockTransactions ST ON (P.id = ST.productID) "
                + "WHERE BT.ID = ? "
                + "GROUP BY P.SKU, P.name, B.name, Pt.id, P.currentSellingPrice, P.reorderThreshold, P.maxStockCapacity, P.isActive "
                + "ORDER BY P.SKU;";

        try (
                // prepare the SQL statement
                PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, String.valueOf(brandTierID));
                ResultSet rs = stmt.executeQuery();

                // column width defs for formatting
                int skuWidth = 6;
                int productWidth = 30;
                int brandWidth = 20;
                int productTypeWidth = 10;
                int priceWidth = 12;
                int currentInventoryWidth = 18;
                int reorderWidth = 18;
                int stockCapacityWidth = 18;
                int isActiveWidth = 10;

                // calculate separator length dynamically
                int SEPARATOR_LENGTH = skuWidth + productWidth + brandWidth +
                        productTypeWidth + priceWidth + currentInventoryWidth +
                        reorderWidth + stockCapacityWidth + isActiveWidth + 10;

                // print table header
                System.out.printf(
                        "\n%-" + skuWidth + "s %-" + productWidth + "s %-" + brandWidth + "s %-" +
                                productTypeWidth + "s %" + priceWidth + "s %" + currentInventoryWidth + "s %" +
                                reorderWidth + "s %" + stockCapacityWidth + "s %-" + isActiveWidth + "s%n",
                        "SKU", "Product", "Brand", "Product Type", "Price", "Current Inventory",
                        "Reorder Threshold", "Max Capacity", "Is Active");

                // print separator line for readability
                for (int i = 0; i < SEPARATOR_LENGTH; i++) {
                    System.out.print("-");
                }
                System.out.println();

                // process each product result and format the output row
                while (rs.next()) {
                    String row = String.format(
                            "%-" + skuWidth + "s %-" + productWidth + "s %-" + brandWidth + "s %-" +
                                    productTypeWidth + "s %" + priceWidth + ".2f %" + currentInventoryWidth + "d %" +
                                    reorderWidth + "d %" + stockCapacityWidth + "d %" + isActiveWidth + "s%n",
                            rs.getString("SKU"),
                            rs.getString("Product").length() > productWidth
                                    ? rs.getString("Product").substring(0, productWidth - 3) + "..."
                                    : rs.getString("Product"),
                            rs.getString("Brand"),
                            rs.getString("Product Type"),
                            rs.getDouble("Selling Price"),
                            rs.getInt("currentInventory"),
                            rs.getInt("Reorder Threshold"),
                            rs.getInt("Max Stock Capacity"),
                            rs.getBoolean("Is Active") ? "Yes" : "No");

                    productsByBrandTier.add(row);
                    System.out.print(row);
                }
                System.out.println();

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return productsByBrandTier;
    }

//-----------------------------------------------------------------------------------------------------------------    

  /**
 * CRUD Multiple Table API
 * Author: Dulguun Delgerbat **
 * Updates cost price and selling price for all products supplied by a given
 * supplier.
 * This method locks relevant database rows using
 * <code>SELECT FOR UPDATE</code>,
 * retrieves old prices, applies the percentage change, and updates both cost
 * and
 * selling prices. The changes are then displayed for comparison.
 * 
 * @param percentage  Percentage increase/decrease (e.g., 10 for +10%, -5 for
 *                    -5%).
 * @param supplierTag Unique identifier for the supplier whose product prices
 *                    are updated.
 * @return Success message with affected product count or error message on
 *         failure.
 */
    public String updateCostAndSellingPrice(int percentage, String supplierTag) {
        String message = "";

        List<String> priceChanges = new ArrayList<>();

        // query to lock ProductSuppliers and retrieve old cost prices using SKU
        String retrieveCostQuery =  "SELECT P.SKU, PS.costPrice " +
                                    "FROM ProductSuppliers PS " +
                                        "JOIN Products P ON P.ID = PS.productID " + 
                                        "JOIN Suppliers S ON PS.supplierID = S.id " +
                                    "WHERE S.supplierTag = ? " +
                                    "FOR UPDATE;"; // locking rows

        // query to lock Products and retrieve old selling prices
        String retrievePriceQuery = "SELECT P.SKU, P.name, P.currentSellingPrice " +
                                    "FROM Products P " +
                                        "JOIN ProductSuppliers PS ON PS.productID = P.id " +
                                        "JOIN Suppliers S ON PS.supplierID = S.id " +
                                    "WHERE S.supplierTag = ? " +
                                    "FOR UPDATE;"; // locking rows

        // query to update cost prices in ProductSuppliers using SKU
        String updateCostPriceQuery =   "UPDATE ProductSuppliers PS " +
                                        "SET costPrice = costPrice * ? " +
                                        "WHERE PS.supplierID = (SELECT id FROM Suppliers WHERE supplierTag = ?) " +
                                        "AND PS.productID IN (SELECT id FROM Products WHERE SKU IN (SELECT P.SKU FROM Products P " +
                                            "JOIN ProductSuppliers PS ON PS.productID = P.id " +
                                            "JOIN Suppliers S ON PS.supplierID = S.id WHERE S.supplierTag = ?));";

        // query to update selling prices in Products using SKU
        String updatePriceQuery =   "UPDATE Products " +
                                    "SET currentSellingPrice = currentSellingPrice * ? " +
                                    "WHERE SKU IN (SELECT P.SKU FROM Products P " +
                                        "JOIN ProductSuppliers PS ON PS.productID = P.id " +
                                        "JOIN Suppliers S ON PS.supplierID = S.id " +
                                    "WHERE S.supplierTag = ?);";

        try {
            conn.setAutoCommit(false); // start transaction
            try (
                    PreparedStatement retrieveCostStmt = conn.prepareStatement(retrieveCostQuery);
                    PreparedStatement retrievePriceStmt = conn.prepareStatement(retrievePriceQuery);
                    PreparedStatement updateCostStmt = conn.prepareStatement(updateCostPriceQuery);
                    PreparedStatement updatePriceStmt = conn.prepareStatement(updatePriceQuery);
                    PreparedStatement retrieveUpdatedStmt = conn.prepareStatement(retrievePriceQuery)) {

                // lock and retrieve old cost prices
                retrieveCostStmt.setString(1, supplierTag);
                ResultSet oldCostPrices = retrieveCostStmt.executeQuery();

                Map<String, Double> oldCostPriceMap = new HashMap<>();
                while (oldCostPrices.next()) {
                    String sku = oldCostPrices.getString("SKU");
                    double costPrice = oldCostPrices.getDouble("costPrice");
                    oldCostPriceMap.put(sku, costPrice);
                }

                oldCostPrices.close();

                if (oldCostPriceMap.isEmpty()) {
                    message = "No products found for the given Supplier Tag.";
                    conn.rollback();
                    return message;
                }

                // update cost prices
                float costMultiplier = 1 + (percentage / 100.0f);
                updateCostStmt.setFloat(1, costMultiplier);
                updateCostStmt.setString(2, supplierTag);
                updateCostStmt.setString(3, supplierTag);
                int costRowsAffected = updateCostStmt.executeUpdate();

                // lock and retrieve old selling prices
                retrievePriceStmt.setString(1, supplierTag);
                ResultSet oldPrices = retrievePriceStmt.executeQuery();

                Map<String, Double> oldSellingPriceMap = new HashMap<>();
                Map<String, String> productNameMap = new HashMap<>();

                while (oldPrices.next()) {
                    String sku = oldPrices.getString("SKU");
                    oldSellingPriceMap.put(sku, oldPrices.getDouble("currentSellingPrice"));
                    productNameMap.put(sku, oldPrices.getString("name"));
                }
                
                oldPrices.close(); 

                // update selling prices
                float priceMultiplier = 1 + (percentage / 100.0f);
                updatePriceStmt.setFloat(1, priceMultiplier);
                updatePriceStmt.setString(2, supplierTag);
                int priceRowsAffected = updatePriceStmt.executeUpdate();

                // retrieve updated prices after changes
                retrieveUpdatedStmt.setString(1, supplierTag);
                ResultSet newPrices = retrieveUpdatedStmt.executeQuery();

                // print comparison table
                System.out.printf("%-10s %-30s %-12s %-12s %-12s %-12s%n", "SKU", "Product", "Old Cost", "New Cost",
                        "Old Price", "New Price");
                System.out.println(
                        "-------------------------------------------------------------------------------------------");

                while (newPrices.next()) {
                    String sku = newPrices.getString("SKU");
                    String product = productNameMap.get(sku);
                    double oldCostPrice = oldCostPriceMap.getOrDefault(sku, 0.0);
                    double newCostPrice = oldCostPrice * costMultiplier;
                    double oldSellingPrice = oldSellingPriceMap.getOrDefault(sku, 0.0);
                    double newSellingPrice = newPrices.getDouble("currentSellingPrice");

                    String row = String.format("%-10s %-30s $%-11.2f $%-11.2f $%-11.2f $%-11.2f",
                            sku, product, oldCostPrice, newCostPrice, oldSellingPrice, newSellingPrice);
                    priceChanges.add(row);
                    System.out.println(row);
                }   

                // explicitely closing ResultSets
                newPrices.close(); 
                conn.commit(); // commit transaction

                // show success message
                message = String.format(
                        "* The cost price and selling price have been updated successfully for %d products with supplier tag %s .*",
                        Math.max(costRowsAffected, priceRowsAffected), supplierTag);
                System.out.println(message);
                // conn.rollback(); // only for testing 
                System.out.println();
            } catch (SQLException e) {
                conn.rollback();
                e.printStackTrace();
                message = "Error updating prices: " + e.getMessage();
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            message = "Error updating prices: " + e.getMessage();
        }
        return message;
    }

//-----------------------------------------------------------------------------------------------------------------

    /* COMPLEX API - suggestRestockLevels()
     * Authors: Inaya Rizvi, Ayesha Mahmood (50/50 contribution)
     * params: none
     * Retrieves and displays restocking suggestions for each product in inventory, based on data from the last 30 days.
     * returns: 
     *      - SKU
     *      - product name
     *      - suggestedSupplier: display which supplier is ideal for the next restockased on current inventory, 
     *          reorder threshold, and lead times of supplies, . If one supplier has lead time days that exceed the number
     *          of days of inventory left, displays the fastest supplier. Otherwise, displays the cheapest supplier.
     *      - current inventory
     *      - DaysLeftToRestock: the number of days until inventory reaches reorder threshold, based on average sales per day in the past 30 days.
     *      - expectedDemand: using the avg sales per day in the past 30 days as estimators for demand in the next 30 days.
     *      - suggestedRestock: displays quantity to restock in the next 30 days based on expected demand and current inventory. If the expected demand
     *          is less than the (current stock
     */
    public List<String> suggestRestockLevels() {
        List<String> restockLevels = new ArrayList<>();

        String query = "WITH ProductSupplierSelection AS (\n" + //
                "    SELECT  \n" + //
                "        p.ID AS productID, \n" + //
                "        p.sku AS \"SKU\", \n" + //
                "        p.name AS \"Product\",\n" + //
                "\n" + //
                "        -- Compute Suggested Supplier\n" + //
                "        (SELECT s.name\n" + //
                "         FROM ProductSuppliers ps \n" + //
                "         JOIN Suppliers s ON s.id = ps.supplierID\n" + //
                "         WHERE ps.productID = p.ID\n" + //
                "         ORDER BY \n" + //
                "            CASE \n" + //
                "                -- When Days Until Reorder Threshold > Fastest Supplier Lead Time\n" + //
                "                WHEN ( \n" + //
                "                    ( \n" + //
                "                        -- Compute Days Until Reorder\n" + //
                "                        COALESCE(\n" + //
                "                            (\n" + //
                "                                SELECT SUM(\n" + //
                "                                    CASE \n" + //
                "                                        WHEN st.transactionTypeID = 'S' THEN st.quantity \n" + //
                "                                        ELSE 0 \n" + //
                "                                    END\n" + //
                "                                ) \n" + //
                "                                FROM StockTransactions st\n" + //
                "                                WHERE st.productID = p.ID\n" + //
                "                                AND st.timestamp >= NOW() - INTERVAL '30 days'\n" + //
                "                            ), 0)  \n" + //
                "                            / 30::NUMERIC\n" + //
                "                    )  \n" + //
                "                    > \n" + //
                "                    -- Fastest Supplier Lead Time\n" + //
                "                    (SELECT MIN(ps.leadTimeDays) \n" + //
                "                     FROM ProductSuppliers ps WHERE ps.productID = p.ID) \n" + //
                "                ) \n" + //
                "                THEN ps.costPrice  -- Choose the cheapest supplier\n" + //
                "                ELSE ps.leadTimeDays  -- Otherwise, choose the fastest supplier\n" + //
                "            END\n" + //
                "         LIMIT 1\n" + //
                "        ) AS suggestedSupplier\n" + //
                "    FROM Products p\n" + //
                ")\n" + //
                "\n" + //
                "SELECT  \n" + //
                "    p.sku AS \"SKU\", \n" + //
                "    p.name AS \"Product\",\n" + //
                "    ps.suggestedSupplier,  -- Use the suggested supplier from CTE\n" + //
                "\n" + //
                "    -- Suggested Supplier’s Lead Time\n" + //
                "    (SELECT ps2.leadTimeDays\n" + //
                "     FROM ProductSuppliers ps2\n" + //
                "     JOIN Suppliers s ON s.id = ps2.supplierID\n" + //
                "     JOIN ProductSupplierSelection pss ON ps2.productID = pss.productID\n" + //
                "     WHERE ps2.productID = p.id \n" + //
                "     AND s.name = pss.suggestedSupplier\n" + //
                "    ) AS suggestedLeadTime, \n" + //
                "\n" + //
                "    -- Compute Current Inventory\n" + //
                "    COALESCE(\n" + //
                "        SUM(\n" + //
                "            CASE\n" + //
                "                WHEN st.transactiontypeID = 'R' THEN st.quantity  -- Restock adds \n" + //
                "                WHEN st.transactiontypeID = 'S' THEN -st.quantity -- Sale subtracts \n" + //
                "                WHEN st.transactiontypeID = 'A' THEN st.quantity  -- Adjustment either way \n" + //
                "                ELSE 0\n" + //
                "            END\n" + //
                "        ), 0) AS currentInventory,\n" + //
                "\n" + //
                "    -- Calculate Days Left Until Stock Hits Reorder Threshold\n" + //
                "    CASE \n" + //
                "        WHEN (SELECT COALESCE(SUM(CASE WHEN st.transactionTypeID = 'S' THEN st.quantity ELSE 0 END), 0) \n" + //
                "              FROM StockTransactions st \n" + //
                "              WHERE st.productID = p.ID \n" + //
                "              AND st.timestamp >= NOW() - INTERVAL '30 days') / 30::NUMERIC > 0 \n" + //
                "        THEN CEIL(\n" + //
                "            (\n" + //
                "                COALESCE(\n" + //
                "                    SUM(\n" + //
                "                        CASE\n" + //
                "                            WHEN st.transactiontypeID = 'R' THEN st.quantity\n" + //
                "                            WHEN st.transactiontypeID = 'S' THEN -st.quantity\n" + //
                "                            WHEN st.transactiontypeID = 'A' THEN st.quantity\n" + //
                "                            ELSE 0\n" + //
                "                        END\n" + //
                "                    ), 0)  \n" + //
                "                - p.reorderThreshold\n" + //
                "            )\n" + //
                "            / \n" + //
                "            NULLIF((SELECT COALESCE(SUM(CASE WHEN st.transactionTypeID = 'S' THEN st.quantity ELSE 0 END), 0) \n" + //
                "                    FROM StockTransactions st \n" + //
                "                    WHERE st.productID = p.ID \n" + //
                "                    AND st.timestamp >= NOW() - INTERVAL '30 days') / 30::NUMERIC, 0)\n" + //
                "        )\n" + //
                "        ELSE 9999\n" + //
                "    END AS DaysLeftToRestock,\n" + //
                "\n" + //
                "    -- **Expected Demand Calculation using Suggested Supplier's Lead Time**\n" + //
                "    CEIL(\n" + //
                "        COALESCE(\n" + //
                "            ( \n" + //
                "                -- Sum only sales transactions (S)\n" + //
                "                (SELECT SUM(st.quantity) \n" + //
                "                 FROM StockTransactions st \n" + //
                "                 WHERE st.productID = p.id \n" + //
                "                 AND st.transactionTypeID = 'S' \n" + //
                "                 AND st.timestamp >= NOW() - INTERVAL '30 days') \n" + //
                "                / 30::NUMERIC\n" + //
                "            ) * \n" + //
                "            (\n" + //
                "                -- Use the lead time of the `suggestedSupplier`\n" + //
                "                (SELECT ps2.leadTimeDays\n" + //
                "                 FROM ProductSuppliers ps2\n" + //
                "                 JOIN Suppliers s ON s.id = ps2.supplierID\n" + //
                "                 JOIN ProductSupplierSelection pss ON ps2.productID = pss.productID\n" + //
                "                 WHERE ps2.productID = p.id \n" + //
                "                 AND s.name = pss.suggestedSupplier\n" + //
                "                )\n" + //
                "            ), \n" + //
                "            0)\n" + //
                "    ) AS expectedDemand,\n" + //
                "\n" + //
                "    -- Calculating restock based on expected demand and current inventory\n" + //
                "    CEIL(\n" + //
                "        LEAST(\n" + //
                "            GREATEST(\n" + //
                "                COALESCE(\n" + //
                "                    (SUM(st.quantity) / 30::NUMERIC) * \n" + //
                "                    (\n" + //
                "                        SELECT ps2.leadTimeDays \n" + //
                "                        FROM ProductSuppliers ps2 \n" + //
                "                        JOIN Suppliers s ON s.id = ps2.supplierID\n" + //
                "                        JOIN ProductSupplierSelection pss ON ps2.productID = pss.productID\n" + //
                "                        WHERE ps2.productID = p.id \n" + //
                "                        AND s.name = pss.suggestedSupplier\n" + //
                "                    ), 0) \n" + //
                "                + p.reorderThreshold \n" + //
                "                - COALESCE(\n" + //
                "                    SUM(\n" + //
                "                        CASE \n" + //
                "                            WHEN st.transactiontypeID = 'R' THEN st.quantity\n" + //
                "                            WHEN st.transactiontypeID = 'S' THEN -st.quantity\n" + //
                "                            WHEN st.transactiontypeID = 'A' THEN st.quantity\n" + //
                "                            ELSE 0\n" + //
                "                        END\n" + //
                "                    ), 0), \n" + //
                "                0\n" + //
                "            ), \n" + //
                "            p.maxStockCapacity - COALESCE(\n" + //
                "                SUM(\n" + //
                "                    CASE \n" + //
                "                        WHEN st.transactiontypeID = 'R' THEN st.quantity\n" + //
                "                        WHEN st.transactiontypeID = 'S' THEN -st.quantity\n" + //
                "                        WHEN st.transactiontypeID = 'A' THEN st.quantity\n" + //
                "                        ELSE 0\n" + //
                "                    END\n" + //
                "                ), 0)\n" + //
                "        ) \n" + //
                "    ) AS suggestedRestock\n" + //
                "\n" + //
                "FROM Products p\n" + //
                "JOIN ProductSupplierSelection ps ON p.ID = ps.productID  -- Join with CTE\n" + //
                "JOIN Brand b on b.id = p.brandid\n" + //
                "JOIN BrandTier bt ON bt.id = b.brandtierID\n" + //
                "JOIN ProductType pt ON pt.id = p.producttypeID \n" + //
                "LEFT JOIN StockTransactions st ON p.ID = st.productID\n" + //
                "GROUP BY p.ID, p.sku, p.name, ps.suggestedSupplier\n" + //
                "ORDER BY DaysLeftToRestock ASC;"; 
    
                try (
                    PreparedStatement stmt = conn.prepareStatement(query);
                    ResultSet rs = stmt.executeQuery()) {
                
                
                    // Column Widths 
                    int skuWidth = 8;
                    int productWidth = 28;
                    int supplierWidth = 30;
                    int inventoryWidth = 12;
                    int daysLeftWidth = 12;
                    int leadTimeWidth = 11;
                    int demandWidth = 15;
                    int restockWidth = 10;
                    
                    

                     // Print first line of the header (Main Column Titles)
                    System.out.printf(
                        "%-" + skuWidth + "s | %-" + productWidth + "s | %-" + supplierWidth + "s | %-" + inventoryWidth + "s | %-" +
                        daysLeftWidth + "s | %-" + leadTimeWidth + "s | %-" + demandWidth + "s | %-" + restockWidth + "s %n",
                        "SKU", "Product", "Suggested", "Current", "Days Left", "Sugg. Supp.", "Exp. Demand", "Suggested"
                    );

                    // Print second line of the header (Underline to Match Widths)
                    System.out.printf(
                        "%-" + skuWidth + "s | %-" + productWidth + "s | %-" + supplierWidth + "s | %-" + inventoryWidth + "s | %-" +
                        daysLeftWidth + "s | %-" + leadTimeWidth + "s | %-" + demandWidth + "s | %-" + restockWidth + "s %n",
                        " ", " ", "Supplier", "Inventory", "Until RT Met", "Lead Time", "in Lead Time", "Restock"
                    );

                    // Print separator line
                    for (int i = 0; i < skuWidth + productWidth + supplierWidth + inventoryWidth + daysLeftWidth +
                            leadTimeWidth + demandWidth + restockWidth + 25; i++) {
                            System.out.print("-");
                    }
                    System.out.println(); 

                    // Print each row with aligned columns
                    while (rs.next()) {
                        System.out.printf(
                            "%-" + skuWidth + "s | %-" + productWidth + "s | %-" + supplierWidth + "s | %" + inventoryWidth + "d | %" +
                            daysLeftWidth + "s | %" + leadTimeWidth + "d | %" + demandWidth + "d | %" + restockWidth + "d %n",
                            rs.getString("SKU"),
                            rs.getString("Product"),
                            rs.getString("suggestedSupplier"),
                            rs.getInt("currentInventory"),
                            rs.getInt("DaysLeftToRestock") == 9999 ? "NA" : String.valueOf(rs.getInt("DaysLeftToRestock")),
                            rs.getInt("suggestedLeadTime"), 
                            rs.getInt("expectedDemand"),
                            rs.getInt("suggestedRestock")
                        );
                    }

                    System.out.println();

                } catch (SQLException e) {
                    e.printStackTrace();
                }

                return restockLevels;
                    
    }

//-------------------------------------------------------------------------------------------------------------------

}