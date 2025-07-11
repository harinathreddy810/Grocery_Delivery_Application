package com.ecommerce.controller;

import com.ecommerce.model.Product;
import com.ecommerce.model.Category;
import com.ecommerce.model.User;
import com.ecommerce.service.ProductService;
import com.ecommerce.service.UserService;
import com.ecommerce.service.SalesReportService;
import com.ecommerce.service.SalesReportService.ProductSalesData;
import com.ecommerce.repository.CategoryRepository;
import com.ecommerce.dto.EditProductDto;
import com.ecommerce.dto.ProductWithImageDto;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/vendor")
@PreAuthorize("hasRole('VENDOR')")
public class VendorController {

    @Autowired
    private ProductService productService;

    @Autowired
    private UserService userService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private SalesReportService salesReportService;

    @GetMapping("/dashboard")
    public String dashboard(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        User vendor = userService.findByUsername(userDetails.getUsername()).orElse(null);
        List<Product> products = productService.findByVendor(vendor);

        model.addAttribute("vendor", vendor);
        model.addAttribute("products", products);
        model.addAttribute("totalProducts", products.size());

        // Add default sales report (last 30 days)
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(30);
        model.addAttribute("salesReport", salesReportService.getVendorSalesReport(vendor, startDate, endDate));

        return "vendor/dashboard";
    }

    // --- Add Product (with Image) ---

    @GetMapping("/add-product")
    public String showAddProductForm(Model model) {
        model.addAttribute("product", new ProductWithImageDto());
        model.addAttribute("categories", categoryRepository.findAll());
        return "vendor/add-product";
    }

    @PostMapping("/add-product")
    public String addProduct(@ModelAttribute("product") ProductWithImageDto productDto,
                             @AuthenticationPrincipal UserDetails userDetails,
                             RedirectAttributes redirectAttributes) {

        User vendor = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        productService.saveProductWithImage(productDto, vendor);
        redirectAttributes.addFlashAttribute("success", "Product added successfully!");
        return "redirect:/vendor/dashboard";
    }

    // --- Edit Product ---

    @GetMapping("/edit-product/{id}")
    public String editProductForm(@PathVariable Long id,
                                @AuthenticationPrincipal UserDetails userDetails,
                                Model model) {
        User vendor = userService.findByUsername(userDetails.getUsername()).orElse(null);
        Product product = productService.findById(id).orElse(null);

        if (product == null || !product.getUser().getId().equals(vendor.getId())) {
            return "redirect:/vendor/dashboard";
        }

        // Convert Product to EditProductDto
        EditProductDto productDto = new EditProductDto();
        productDto.setName(product.getName());
        productDto.setDescription(product.getDescription());
        productDto.setPrice(product.getPrice());
        productDto.setQuantity(product.getQuantity());
        productDto.setCategoryId(product.getCategory().getId());

        model.addAttribute("product", productDto);
        model.addAttribute("productId", id); // Pass ID separately
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("currentImage", product.getImageData() != null);
        return "vendor/edit-product";
    }

    @PostMapping("/edit-product/{id}")
    public String editProduct(@PathVariable Long id,
                            @Valid @ModelAttribute("product") EditProductDto productDto,
                            BindingResult result,
                            @AuthenticationPrincipal UserDetails userDetails,
                            Model model,
                            RedirectAttributes redirectAttributes) {

        // Validate input
        if (result.hasErrors()) {
            model.addAttribute("categories", categoryRepository.findAll());
            model.addAttribute("productId", id);
            model.addAttribute("currentImage", productService.findById(id)
                .map(p -> p.getImageData() != null)
                .orElse(false));
            return "vendor/edit-product";
        }

        User vendor = userService.findByUsername(userDetails.getUsername()).orElse(null);
        Product existingProduct = productService.findById(id).orElse(null);

        if (existingProduct == null || !existingProduct.getUser().getId().equals(vendor.getId())) {
            redirectAttributes.addFlashAttribute("error", "Product not found!");
            return "redirect:/vendor/dashboard";
        }

        // Update product
        existingProduct.setName(productDto.getName());
        existingProduct.setDescription(productDto.getDescription());
        existingProduct.setPrice(productDto.getPrice());
        existingProduct.setQuantityType(productDto.getQuantityType());
        
        // Update category
        Category category = categoryRepository.findById(productDto.getCategoryId())
            .orElseThrow(() -> new IllegalArgumentException("Invalid category ID"));
        existingProduct.setCategory(category);

        // Update image if provided
        try {
            if (productDto.getImageFile() != null && !productDto.getImageFile().isEmpty()) {
                existingProduct.setImageName(productDto.getImageFile().getOriginalFilename());
                existingProduct.setImageType(productDto.getImageFile().getContentType());
                existingProduct.setImageData(productDto.getImageFile().getBytes());
            }
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error", "Failed to process image");
            return "redirect:/vendor/edit-product/" + id;
        }

        productService.updateProduct(existingProduct);
        redirectAttributes.addFlashAttribute("success", "Product updated successfully!");
        return "redirect:/vendor/dashboard";
    }

    // --- Delete Product ---

    @PostMapping("/delete-product/{id}")
    public String deleteProduct(@PathVariable Long id,
                                @AuthenticationPrincipal UserDetails userDetails,
                                RedirectAttributes redirectAttributes) {
        User vendor = userService.findByUsername(userDetails.getUsername()).orElse(null);
        Product product = productService.findById(id).orElse(null);

        if (product == null || !product.getUser().getId().equals(vendor.getId())) {
            redirectAttributes.addFlashAttribute("error", "Product not found!");
            return "redirect:/vendor/dashboard";
        }

        productService.deleteProduct(id);
        redirectAttributes.addFlashAttribute("success", "Product deleted successfully!");
        return "redirect:/vendor/dashboard";
    }

    // --- Sales Report ---

    @GetMapping("/reports/sales")
    public String salesReport(@AuthenticationPrincipal UserDetails userDetails,
                              @RequestParam(required = false) String start,
                              @RequestParam(required = false) String end,
                              Model model) {
        User vendor = userService.findByUsername(userDetails.getUsername()).orElse(null);
        LocalDate startDate = start != null ? LocalDate.parse(start) : LocalDate.now().minusDays(30);
        LocalDate endDate = end != null ? LocalDate.parse(end) : LocalDate.now();

        model.addAttribute("salesReport", salesReportService.getVendorSalesReport(vendor, startDate, endDate));
        return "vendor/sales-report";
    }

    // --- Product Report ---

    @GetMapping("/reports/products")
    public String productReport(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User vendor = userService.findByUsername(userDetails.getUsername()).orElse(null);
        List<Product> products = productService.findByVendor(vendor);

        Map<Long, ProductSalesData> salesData = salesReportService.getProductSalesData(
                vendor,
                LocalDate.now().minusDays(30),
                LocalDate.now()
        );

        if (salesData == null) {
            salesData = new HashMap<>();
        }

        int maxStock = Math.max(
                products.stream().mapToInt(Product::getQuantity).max().orElse(10),
                10
        );

        model.addAttribute("products", products);
        model.addAttribute("salesData", salesData);
        model.addAttribute("maxStock", maxStock);

        return "vendor/product-report";
    }
}
