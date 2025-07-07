package com.ecommerce.service;

import com.ecommerce.model.*;
import com.ecommerce.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartService cartService;
    private final ProductService productService;

    @Autowired
    public OrderService(OrderRepository orderRepository,
                        CartService cartService,
                        ProductService productService) {
        this.orderRepository = orderRepository;
        this.cartService = cartService;
        this.productService = productService;
    }

    @Transactional
    public Order createOrder(User user, String paymentMethod, String shippingAddress) {
        Cart cart = cartService.getOrCreateCart(user);

        if (cart.getCartItems().isEmpty()) {
            throw new IllegalStateException("Cannot create order from empty cart");
        }

        // Generate unique order number
        String orderNumber = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // Create order
        Order order = new Order(
                user,
                orderNumber,
                cart.getTotalAmount(),
                paymentMethod,
                shippingAddress
        );

        // Convert cart items to order items
        Set<OrderItem> orderItems = new HashSet<>();
        for (CartItem cartItem : cart.getCartItems()) {
            Product product = cartItem.getProduct();

            // Check stock availability
            if (product.getQuantity() < cartItem.getQuantity()) {
                throw new IllegalStateException(
                        "Insufficient stock for product: " + product.getName());
            }

            // Reduce product quantity
            product.setQuantity(product.getQuantity() - cartItem.getQuantity());
            productService.updateProduct(product);

            // Create order item
            OrderItem orderItem = new OrderItem(
                    order,
                    product,
                    cartItem.getQuantity(),
                    cartItem.getPrice()
            );
            orderItems.add(orderItem);
        }

        order.setOrderItems(orderItems);

        // Save order
        Order savedOrder = orderRepository.save(order);

        // Clear cart
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
    public Order updateOrder(Order order) {
        return orderRepository.save(order);
    }
}
