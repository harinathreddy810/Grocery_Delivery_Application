package com.ecommerce.controller;

import com.ecommerce.model.User;
import com.ecommerce.model.Product;
import com.ecommerce.model.Order;
import com.ecommerce.service.UserService;
import com.ecommerce.service.ProductService;
import com.ecommerce.service.OrderService;
import com.ecommerce.service.SalesReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired
    private UserService userService;

    @Autowired
    private ProductService productService;

    @Autowired
    private SalesReportService salesReportService;

    @Autowired
    private OrderService orderService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        List<User> users = userService.findAllUsers();
        List<User> vendors = userService.findByRole(User.Role.VENDOR);
        List<User> customers = userService.findByRole(User.Role.USER);

        model.addAttribute("totalUsers", users.size());
        model.addAttribute("totalVendors", vendors.size());
        model.addAttribute("totalCustomers", customers.size());
        model.addAttribute("totalProducts", productService.findAllProducts().size());
        model.addAttribute("users", users);

        // Add default sales report (last 30 days)
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(30);
        model.addAttribute("salesReport", salesReportService.getAdminSalesReport(startDate, endDate));

        return "admin/dashboard";
    }

    @PostMapping("/user/{id}/toggle-status")
    public String toggleUserStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        User user = userService.findById(id);
        if (user != null) {
            userService.updateUserStatus(id, !user.isEnabled());
            redirectAttributes.addFlashAttribute("success", "User status updated successfully!");
        } else {
            redirectAttributes.addFlashAttribute("error", "User not found!");
        }
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/user/{id}/delete")
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.deleteUser(id);
            redirectAttributes.addFlashAttribute("success", "User deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting user!");
        }
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/reports/sales")
    public String salesReport(@RequestParam(required = false) String start,
                              @RequestParam(required = false) String end,
                              Model model) {
        LocalDate startDate = start != null ? LocalDate.parse(start) : LocalDate.now().minusDays(30);
        LocalDate endDate = end != null ? LocalDate.parse(end) : LocalDate.now();

        model.addAttribute("salesReport", salesReportService.getAdminSalesReport(startDate, endDate));
        return "admin/sales-report";
    }

    @GetMapping("/reports/products")
    public String productReport(Model model) {
        List<Product> products = productService.findAllProducts();
        model.addAttribute("products", products);
        return "admin/product-report";
    }

    @GetMapping("/reports/orders")
    public String orderReport(Model model) {
        List<Order> orders = orderService.getAllOrders();
        model.addAttribute("orders", orders);
        return "admin/order-report";
    }
    
    @GetMapping("/orders/{id}")
    public String viewOrderDetails(@PathVariable Long id, Model model) {
        Order order = orderService.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Invalid order ID"));
        
        model.addAttribute("order", order);
        model.addAttribute("statuses", Order.OrderStatus.values());
        return "admin/order-details";
    }

    @PostMapping("/orders/{id}/update-status")
    public String updateOrderStatus(@PathVariable Long id,
                                  @RequestParam Order.OrderStatus status,
                                  RedirectAttributes redirectAttributes) {
        try {
            Order updatedOrder = orderService.updateOrderStatus(id, status);
            redirectAttributes.addFlashAttribute("success", 
                "Order status updated to " + status + " successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", 
                "Failed to update order status: " + e.getMessage());
        }
        return "redirect:/admin/orders/" + id;
    }
}
