package com.ecommerce.controller;

import com.ecommerce.model.*;
import com.ecommerce.service.*;
import com.ecommerce.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;

@Controller
@RequestMapping("/user")
@PreAuthorize("hasRole('USER')")
public class UserController {

    private final ProductService productService;
    private final UserService userService;
    private final CartService cartService;
    private final CategoryRepository categoryRepository;
    private final OrderService orderService;

    @Autowired
    public UserController(ProductService productService,
                          UserService userService,
                          CartService cartService,
                          CategoryRepository categoryRepository,
                          OrderService orderService) {
        this.productService = productService;
        this.userService = userService;
        this.cartService = cartService;
        this.categoryRepository = categoryRepository;
        this.orderService = orderService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, @RequestParam(required = false) String category) {
        List<Product> products;

        if (category != null && !category.isEmpty()) {
            Category cat = categoryRepository.findByName(category).orElse(null);
            products = (cat != null) ? productService.findByCategory(cat) : productService.findActiveProducts();
        } else {
            products = productService.findActiveProducts();
        }

        model.addAttribute("products", products);
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("selectedCategory", category);

        return "user/dashboard";
    }

    @PostMapping("/add-to-cart/{productId}")
    public String addToCart(@PathVariable Long productId,
                            @RequestParam(defaultValue = "1") Integer quantity,
                            @AuthenticationPrincipal UserDetails userDetails,
                            RedirectAttributes redirectAttributes) {

        User user = userService.findByUsername(userDetails.getUsername()).orElse(null);
        Product product = productService.findById(productId).orElse(null);

        if (product == null) {
            redirectAttributes.addFlashAttribute("error", "Product not found!");
            return "redirect:/user/dashboard";
        }

        if (product.getQuantity() < quantity) {
            redirectAttributes.addFlashAttribute("error", "Insufficient stock!");
            return "redirect:/user/dashboard";
        }

        cartService.addToCart(user, product, quantity);
        redirectAttributes.addFlashAttribute("success", "Product added to cart!");
        return "redirect:/user/dashboard";
    }

    @GetMapping("/cart")
    public String viewCart(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByUsername(userDetails.getUsername()).orElse(null);
        Cart cart = cartService.getOrCreateCart(user);

        model.addAttribute("cart", cart);
        return "user/cart";
    }

    @PostMapping("/cart/remove/{productId}")
    public String removeFromCart(@PathVariable Long productId,
                                 @AuthenticationPrincipal UserDetails userDetails,
                                 RedirectAttributes redirectAttributes) {

        User user = userService.findByUsername(userDetails.getUsername()).orElse(null);
        cartService.removeFromCart(user, productId);
        redirectAttributes.addFlashAttribute("success", "Product removed from cart!");
        return "redirect:/user/cart";
    }

    @GetMapping("/checkout")
    public String checkout(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByUsername(userDetails.getUsername()).orElse(null);
        Cart cart = cartService.getOrCreateCart(user);

        if (cart.getCartItems().isEmpty()) {
            return "redirect:/user/cart";
        }

        model.addAttribute("cart", cart);
        model.addAttribute("user", user);
        return "user/checkout";
    }

    @PostMapping("/checkout/process")
    public String processCheckout(@RequestParam String paymentMethod,
                                  @RequestParam String address,
                                  @AuthenticationPrincipal UserDetails userDetails,
                                  RedirectAttributes redirectAttributes) {

        User user = userService.findByUsername(userDetails.getUsername()).orElse(null);

        try {
            Order order = orderService.createOrder(user, paymentMethod, address);

            if ("ONLINE".equalsIgnoreCase(paymentMethod)) {
                return "redirect:/user/payment/" + order.getId();
            }

            redirectAttributes.addFlashAttribute("success",
                    "Order #" + order.getOrderNumber() + " placed successfully!");
            return "redirect:/user/orders";

        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/user/cart";
        }
    }

    @GetMapping("/orders")
    public String viewOrders(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByUsername(userDetails.getUsername()).orElse(null);
        List<Order> orders = orderService.getUserOrders(user);
        model.addAttribute("orders", orders);
        return "user/orders";
    }

    @GetMapping("/orders/{orderId}")
    public String viewOrderDetails(@PathVariable Long orderId,
                                   @AuthenticationPrincipal UserDetails userDetails,
                                   Model model) {

        User user = userService.findByUsername(userDetails.getUsername()).orElse(null);
        Order order = orderService.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid order ID"));

        if (!order.getUser().getId().equals(user.getId())) {
            return "redirect:/user/orders";
        }

        model.addAttribute("order", order);
        return "user/order-details";
    }

    @GetMapping("/payment/{orderId}")
    public String paymentPage(@PathVariable Long orderId,
                              @AuthenticationPrincipal UserDetails userDetails,
                              Model model) {

        User user = userService.findByUsername(userDetails.getUsername()).orElse(null);
        Order order = orderService.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid order ID"));

        if (!order.getUser().getId().equals(user.getId())) {
            return "redirect:/user/orders";
        }

        model.addAttribute("order", order);
        return "user/payment";
    }

    @PostMapping("/payment/complete/{orderId}")
    public String completePayment(@PathVariable Long orderId,
                                  @RequestParam String paymentType,
                                  @RequestParam(required = false) String cardNumber,
                                  @RequestParam(required = false) String cardName,
                                  @RequestParam(required = false) String expiryDate,
                                  @RequestParam(required = false) String cvv,
                                  @RequestParam(required = false) String upiId,
                                  @AuthenticationPrincipal UserDetails userDetails,
                                  RedirectAttributes redirectAttributes) {

        User user = userService.findByUsername(userDetails.getUsername()).orElse(null);
        Order order = orderService.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid order ID"));

        if (!order.getUser().getId().equals(user.getId())) {
            return "redirect:/user/orders";
        }

        // Store the selected payment method (you can enhance this to store actual payment details securely)
        order.setPaymentMethod("ONLINE (" + paymentType + ")");
        orderService.updateOrder(order);

        redirectAttributes.addFlashAttribute("success",
                "Order #" + order.getOrderNumber() + " placed successfully!");
        return "redirect:/user/orders";
    }
}
