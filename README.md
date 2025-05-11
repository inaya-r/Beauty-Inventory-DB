# ADI Inventory Management System

Team Members: Inaya Rizvi, Dulguun Delgerbat, Ayesha Mahmood

## Overview

This project provides an API-based inventory management system for a beauty and personal care company struggling to balance product demand and profitability. The system supports streamlined tracking and decision-making for products across affordable and luxury brand tiers.

## Business Problem

Managing stock levels for both affordable and luxury beauty products is challenging due to unpredictable sales and variable supplier lead times. The goal of this system is to:
- Prevent overstocking of slow-moving items
- Avoid stockouts of high-demand products
- Improve efficiency of restocking decisions

## 🗂️ Features

### ✅ CRUD APIs
- `createProduct`, `updateProduct`
- `createBrand`, `updateBrand`
- `createSupplier`, `updateSupplier`
- `updateCostAndSellingPrice`
- `insertTransactionAndShowUpdatedInventory`

### 📋 List APIs
- `getAllProductInventory`
- `getProductInventoryByProductType`
- `getProductInventoryByBrandName`
- `getProductsByBrandTier`
- `getProductInventoryBySupplier`
- `getSupplierContact`
- `getBrandList`

### 📈 Detail APIs
- `getBrandTierPerformance`
- `getProductTypePerformance`
- `getSupplierDetails`

### 🤖 Complex APIs
- `suggestRestockLevels`  
Uses historical sales trends and supplier lead times to:
  - Predict future demand
  - Identify optimal supplier
  - Recommend precise restock quantities

## Technologies Used
- Java (backend)
- PostgreSQL (database)
- RESTful API architecture

## Example Use Cases
- Update selling prices for all products from a supplier in one call
- View inventory levels across all product types or brand tiers
- Forecast restocking needs based on sales trends
- Analyze supplier lead times and performance

## 👩‍💻 Contributions

| Team Member | Key Contributions |
|-------------|-------------------|
| Inaya Rizvi | `getAllProductInventory`, `getBrandTierPerformance`, `suggestRestockLevels`, Driver program |
| Dulguun Delgerbat | `getProductsByBrandTier`, `updateCostAndSellingPrice`, Database design |
| Ayesha Mahmood | `getProductInventoryByProductType`, `insertTransactionAndShowUpdatedInventory`, `suggestRestockLevels` |


---
