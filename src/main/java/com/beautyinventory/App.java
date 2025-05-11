package com.beautyinventory;

import com.beautyinventory.dao.*;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;

public class App {
    private static final ProductDAO productDAO = new ProductDAO(); 
    private static final BrandDAO brandDAO = new BrandDAO();

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        // shutdown hook to close the database connection when the application exits
        Runtime.getRuntime().addShutdownHook(new Thread(DatabaseConnection::closeConnection));

        System.out.println("\nWelcome to Beauty Inventory Management System!");

        while (true) {
            System.out.println("\nMenu:");
            System.out.println("1. Get All Product Inventory");
            System.out.println("2. Get Product Inventory by Product Type");
            System.out.println("3. Get Product Inventory By Brand Tier");
            System.out.println("4. Get Brand Tier Performance");
            System.out.println("5. Suggest Restock Levels");
            System.out.println("6. Update Cost & Selling Price By Supplier");
            System.out.println("7. Insert a Transaction (Sale, Restock, or Adjustment)");
            System.out.println("8. Exit");
            System.out.print("Enter your choice: ");
            
            int choice;
            try {
                choice = Integer.parseInt(scanner.nextLine());
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number between 1 and 8.");
                continue;
            }

            switch (choice) {
                case 1:
                    getAllProductInventory();
                    break;
                case 2:
                    getProductInventoryByProductType(scanner);    
                    break;
                case 3:
                    getProductsByBrandTier(scanner);
                    break;
                case 4:
                    getBrandTierPerformance(scanner);
                    break;
                case 5:
                    suggestRestockLevels();
                    break;
                case 6:
                    updateCostAndSellingPriceBySupplier(scanner);
                    break;
                case 7:
                    insertTransactionAndShowUpdatedInventory(scanner);
                    break;
                case 8:
                    System.out.println("Exiting the application...");
                    scanner.close();
                    return;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }
    }

/* ---------------------------------------------------------------------------------------------------------
 * DULGUUN'S APIS
 * ---------------------------------------------------------------------------------------------------------
*/
    private static void getProductsByBrandTier(Scanner scanner) {
        System.out.print("Enter Brand Tier (A - Affordable, L - Luxury): ");
        String brandTierInput = scanner.nextLine().trim().toUpperCase(); // ensure input is correct

        if (!(brandTierInput.equals("A") || brandTierInput.equals("L"))) {
            System.out.println("Invalid input. Please enter 'A' for Affordable or 'L' for Luxury.");
            return;
        }

        char brandTierID = brandTierInput.charAt(0); // Convert to char
        
        System.out.println("\nFetching products for Brand Tier: \n" + brandTierID);

        List<String> products = productDAO.getProductsByBrandTier(brandTierID);

        if (products.isEmpty()) {
            System.out.println("No products found for the given brand tier.");
        } 
    }

    private static void updateCostAndSellingPriceBySupplier(Scanner scanner) {
        System.out.println("\n\t\t **Price Update by Supplier**");
        System.out.println(
                "This feature allows you to increase or decrease prices for all products associated with a specific supplier.");

        // Display supplier options
        System.out.println("\nAvailable Suppliers:");
        System.out.println("  HSC - Herbal Skincare Co");
        System.out.println("  LBS - Luxury Beauty Supplies");
        System.out.println("  EFB - Eco-Friendly Beauty");
        System.out.println("  NGP - Natural Glow Products");
        System.out.println("  ABL - Aesthetics Beauty Labs");
        System.out.println("  RSS - Radiant Skin Suppliers");
        System.out.println("  BCI - Beauty Cosmetics Inc.");

        // validate supplier input
        String[] validSuppliers = { "HSC", "LBS", "EFB", "NGP", "ABL", "RSS", "BCI" };
        String supplier;
        boolean isValidSupplier = false;

        do {
            System.out.print("\nEnter the Supplier Tag (e.g., HSC, LBS): ");
            supplier = scanner.nextLine().trim().toUpperCase();

            // check if the entered supplier is valid
            for (String valid : validSuppliers) {
                if (supplier.equals(valid)) {
                    isValidSupplier = true;
                    break;
                }
            }

            if (!isValidSupplier) {
                System.out.println("Invalid supplier tag. Please enter a valid tag from the list above.");
            }
        } while (!isValidSupplier);

        // validate percentage input
        int percentage = 0;
        boolean validPercentage = false;

        do {
            System.out.print(
                    "\nEnter the percentage to increase or decrease the price (e.g., 10 for +10%, -5 for -5%): ");

            if (scanner.hasNextInt()) {
                percentage = scanner.nextInt();
                scanner.nextLine(); // consume newline
                validPercentage = true;
            } else {
                System.out.println("Invalid input. Please enter a valid integer for the percentage.");
                scanner.next(); // consume invalid input
            }
        } while (!validPercentage);

        // confirmation message before proceeding
        System.out.println("\nProcessing Price Update");
        System.out.println("   Supplier: " + supplier);
        System.out.println("   Price Adjustment: " + percentage + "%");
        System.out.println(
                "-------------------------------------------------------------------------------------------");
        // call the update 
        productDAO.updateCostAndSellingPrice(percentage, supplier);
    }
   

/* ---------------------------------------------------------------------------------------------------------
 * INAYA'S APIS
 * ---------------------------------------------------------------------------------------------------------
*/

    /** getAllProductInventory
     * - params: none
     * - Retrieves and displays all product inventory data. 
     * - calls ProductDAO.getAllProductInventory() method to fetch inventory details.
     */
    private static void getAllProductInventory() {
        System.out.println("Fetching all product inventory...");
        List<String> inventory = productDAO.getAllProductInventory();

        if (inventory.isEmpty()) {
            System.out.println("Finished.");
        } else {
            for (String row : inventory) {
                System.out.println(row);
            }
        }
    }

    /** getBrandTierPerformance
     * - params: scanner - captures user input for date range
     * - Retrieves and displays brand tier performance data within a specific date range.  
     * - calls ProductDAO.getBrandTierPerformance() method to fetch performance details.
     */
    private static void getBrandTierPerformance(Scanner scanner) {
        
        System.out.println("Enter Start Date (YYYY-MM-DD) or type 'today' for today's date: ");
        String startDateStr = scanner.nextLine().trim().toUpperCase();
        System.out.println("Enter End Date (YYYY-MM-DD) or type 'today' for today's date: ");
        String endDateStr = scanner.nextLine().trim().toUpperCase();

        try {
            Date startDate = (startDateStr.equals("TODAY")) 
                ? Date.valueOf(LocalDate.now()) 
                : Date.valueOf(startDateStr);
        
        Date endDate = (endDateStr.equals("TODAY")) 
                ? Date.valueOf(LocalDate.now()) 
                : Date.valueOf(endDateStr);

            System.out.println("Fetching performance from " + startDateStr + " to " + endDateStr + 
                                " for each Brand Tier \n");

            List<String> performance = brandDAO.getBrandTierPerformance(startDate, endDate);

            System.out.println("\nData Retrieved.");

            if (performance.isEmpty()) {
                System.out.println("Finished.");
            } 
            
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid date format. Please enter the date in YYYY-MM-DD format.");
            return;
        }   
    }

/* ---------------------------------------------------------------------------------------------------------
 * AYESHA & INAYA
 * ---------------------------------------------------------------------------------------------------------
*/

    /* suggestRestockLevels()
     * params: none.
     * Returns suggested restocks based on data from the past 30 days. 
     */
    private static void suggestRestockLevels() {
        System.out.println("Fetching suggested restock levels...\n");
    
        List<String> restockLevels = productDAO.suggestRestockLevels();

        if (restockLevels.isEmpty()) {
            System.out.println("Finished.");
        } else {
            // Print formatted results
            System.out.println("\nSuggested Restock Levels:");
            for (String restockInfo : restockLevels) {
                System.out.println(restockInfo);
            }
        }
    }

/* ---------------------------------------------------------------------------------------------------------
 * AYESHA'S APIS
 * ---------------------------------------------------------------------------------------------------------
*/

    private static void getProductInventoryByProductType(Scanner scanner) {
        System.out.print("Select a Product Type to Recieve Inventory For: \n");
        System.out.println("\tSK - Skincare");
        System.out.println("\tMU - Makeup");
        System.out.println("\tHC - Haircare");
        System.out.println("\tFR - Fragrances");
        System.out.println("\tBB - Bath & Body");
        System.out.println("\tBT - Beauty Tools & Accessories\n");
        System.out.print("Enter Product Type:");

        String productType = scanner.nextLine().toUpperCase();
        System.out.println("Input received: " + productType); 
        productDAO.getProductInventoryByProductType(productType); 
    }


    private static void insertTransactionAndShowUpdatedInventory(Scanner scanner) {
        System.out.println("\n Test Transaction Insertion ");
    
        // Simulate user input for transaction
        System.out.print("Enter Product SKU: ");
        int productId = Integer.parseInt(scanner.nextLine());
    
    
        System.out.print("Enter Quantity: ");
        int quantity = Integer.parseInt(scanner.nextLine());
    
        System.out.print("Enter Transaction Type (R/S/A): ");
        String transactionType = scanner.nextLine().toUpperCase();
        
        // Validate transaction type
        if (!transactionType.equals("R") && !transactionType.equals("S") && !transactionType.equals("A")) {
            System.out.println("Invalid transaction type. You can only enter 'R' for Restock, 'S' for Sale, or 'A' for Adjustment.");
            return;
        }

        // Call DAO method
        productDAO.insertTransactionAndShowUpdatedInventory(
            productId,
            quantity,
            transactionType
    
        );
    }
    

}
