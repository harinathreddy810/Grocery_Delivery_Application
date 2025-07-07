package com.ecommerce.service;

import com.ecommerce.model.*;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SalesReportService {

    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private ProductRepository productRepository;
    
    

    public Map<String, Object> getAdminSalesReport(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> report = new HashMap<>();
        
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);
        
        List<Order> orders = orderRepository.findByCreatedAtBetween(start, end);
        
        // Total sales
        BigDecimal totalSales = orders.stream()
            .map(Order::getTotalAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Sales by status
        Map<Order.OrderStatus, BigDecimal> salesByStatus = orders.stream()
            .collect(Collectors.groupingBy(
                Order::getStatus,
                Collectors.reducing(
                    BigDecimal.ZERO,
                    Order::getTotalAmount,
                    BigDecimal::add
                )
            ));
        
        // Top selling products
        List<Map<String, Object>> topProducts = productRepository.findAll().stream()
            .map(product -> {
                long soldCount = orders.stream()
                    .flatMap(order -> order.getOrderItems().stream())
                    .filter(item -> item.getProduct().getId().equals(product.getId()))
                    .mapToLong(OrderItem::getQuantity)
                    .sum();
                
                Map<String, Object> productData = new HashMap<>();
                productData.put("product", product);
                productData.put("soldCount", soldCount);
                return productData;
            })
            .sorted((a, b) -> Long.compare((Long)b.get("soldCount"), (Long)a.get("soldCount")))
            .limit(10)
            .collect(Collectors.toList());
        
        report.put("totalSales", totalSales);
        report.put("totalOrders", orders.size());
        report.put("salesByStatus", salesByStatus);
        report.put("topProducts", topProducts);
        report.put("startDate", startDate);
        report.put("endDate", endDate);
        
        return report;
    }

    public Map<String, Object> getVendorSalesReport(User vendor, LocalDate startDate, LocalDate endDate) {
        Map<String, Object> report = new HashMap<>();
        
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);
        
        List<Order> orders = orderRepository.findByCreatedAtBetween(start, end);
        
        // Filter orders containing vendor's products
        List<Order> vendorOrders = orders.stream()
            .filter(order -> order.getOrderItems().stream()
                .anyMatch(item -> item.getProduct().getUser().getId().equals(vendor.getId())))
            .collect(Collectors.toList());
        
        // Vendor's total sales
        BigDecimal vendorSales = vendorOrders.stream()
            .flatMap(order -> order.getOrderItems().stream())
            .filter(item -> item.getProduct().getUser().getId().equals(vendor.getId()))
            .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Vendor's product performance
        List<Product> vendorProducts = productRepository.findByUser(vendor);
        List<Map<String, Object>> productPerformance = vendorProducts.stream()
            .map(product -> {
                long soldCount = vendorOrders.stream()
                    .flatMap(order -> order.getOrderItems().stream())
                    .filter(item -> item.getProduct().getId().equals(product.getId()))
                    .mapToLong(OrderItem::getQuantity)
                    .sum();
                
                Map<String, Object> productData = new HashMap<>();
                productData.put("product", product);
                productData.put("soldCount", soldCount);
                return productData;
            })
            .sorted((a, b) -> Long.compare((Long)b.get("soldCount"), (Long)a.get("soldCount")))
            .collect(Collectors.toList());
        
        report.put("totalSales", vendorSales);
        report.put("totalOrders", vendorOrders.size());
        report.put("productPerformance", productPerformance);
        report.put("startDate", startDate);
        report.put("endDate", endDate);
        
        return report;
        
        
        
    }
    
    public class ProductSalesData {
        private long soldQuantity;
        private BigDecimal totalRevenue;

        public ProductSalesData(long soldQuantity, BigDecimal totalRevenue) {
            this.soldQuantity = soldQuantity;
            this.totalRevenue = totalRevenue;
        }

        public long getSoldQuantity() {
            return soldQuantity;
        }

        public void setSoldQuantity(long soldQuantity) {
            this.soldQuantity = soldQuantity;
        }

        public BigDecimal getTotalRevenue() {
            return totalRevenue;
        }

        public void setTotalRevenue(BigDecimal totalRevenue) {
            this.totalRevenue = totalRevenue;
        }
    }
    
    public Map<Long, ProductSalesData> getProductSalesData(User vendor, LocalDate startDate, LocalDate endDate) {
        Map<Long, ProductSalesData> productSalesMap = new HashMap<>();

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);
        
        List<Order> orders = orderRepository.findByCreatedAtBetween(start, end);

        // Filter only the vendor's products
        for (Order order : orders) {
            for (OrderItem item : order.getOrderItems()) {
                Product product = item.getProduct();
                if (product.getUser().getId().equals(vendor.getId())) {
                    long quantity = item.getQuantity();
                    BigDecimal revenue = item.getPrice().multiply(BigDecimal.valueOf(quantity));

                    productSalesMap.merge(
                        product.getId(),
                        new ProductSalesData(quantity, revenue),
                        (oldData, newData) -> {
                            long totalQty = oldData.getSoldQuantity() + newData.getSoldQuantity();
                            BigDecimal totalRevenue = oldData.getTotalRevenue().add(newData.getTotalRevenue());
                            return new ProductSalesData(totalQty, totalRevenue);
                        }
                    );
                }
            }
        }

        return productSalesMap;
    }

}