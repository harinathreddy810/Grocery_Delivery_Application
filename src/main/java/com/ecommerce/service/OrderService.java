package com.ecommerce.service;

import java.security.Principal;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.memory.UserAttribute;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.model.Cart;
import com.ecommerce.model.CartItem;
import com.ecommerce.model.Order;
import com.ecommerce.model.OrderItem;
import com.ecommerce.model.Product;
import com.ecommerce.model.User;
import com.ecommerce.repository.OrderRepository;

@Service
public class OrderService {

    private final UserService userService;

    private final EmailService emailService;

    private final OrderRepository orderRepository;
    private final CartService cartService;
    private final ProductService productService;


    @Autowired
    public OrderService(OrderRepository orderRepository,
    					UserService userService,
                        CartService cartService,EmailService emailService,
                        ProductService productService) {
        this.orderRepository = orderRepository;
        this.cartService = cartService;
        this.productService = productService;
        this.emailService = emailService;
        this.userService = userService;
    }

    @Transactional
    public Order createOrder(User user, String paymentMethod, String address) {
        Cart cart = cartService.getOrCreateCart(user);
        
        if (cart.getCartItems().isEmpty()) {
            throw new IllegalStateException("Cart is empty");
        }

        // Create order first
        String orderNumber = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Order order = new Order(user, orderNumber, cart.getTotalAmount(), paymentMethod, address);

        // Process items and update inventory
        Set<OrderItem> orderItems = new HashSet<>();
        for (CartItem cartItem : cart.getCartItems()) {
            Product product = cartItem.getProduct();
            
            // Verify stock with clear error message
            if (product.getQuantity() < cartItem.getQuantity()) {
                throw new IllegalStateException(
                    "Only " + product.getQuantity() + " items available for " + product.getName() + 
                    ", but " + cartItem.getQuantity() + " requested");
            }

            // Update product quantity (can go to zero)
            int newQuantity = product.getQuantity() - cartItem.getQuantity();
            product.setQuantity(newQuantity);
            productService.updateProduct(product);

            // Create order item
            OrderItem orderItem = new OrderItem(
                order, product, cartItem.getQuantity(), cartItem.getPrice()
            );
            orderItems.add(orderItem);
        }

        order.setOrderItems(orderItems);
        Order savedOrder = orderRepository.save(order);
        
        // Clear cart only after successful order creation
        cartService.clearCart(user);
        
        return savedOrder;
    }
    public List<Order> getUserOrders(User user) {
        return orderRepository.findByUser(user);
    }

    public Optional<Order> findById(Long id) {
        return orderRepository.findById(id);
    }

    public List<Order> getAllOrders() {
        return orderRepository.findByOrderByCreatedAtDesc();
    }

    public Order updateOrderStatus(Long orderId, Order.OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        order.setStatus(status);
        return orderRepository.save(order);
    }

    // ✅ Newly added method
    public Order updateOrder(Order order ) {
    	Order order1 = orderRepository.save(order);
    	User user = order1.getUser();
    	emailService.sendSuccessEmail(order1.getOrderNumber(),order1.getTotalAmount(),user.getEmail(),order1.getShippingAddress());
        System.out.println(user);
    	return order1 ;
    }
}
