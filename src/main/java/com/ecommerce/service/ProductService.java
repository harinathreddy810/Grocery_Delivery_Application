package com.ecommerce.service;

import com.ecommerce.model.Product;
import com.ecommerce.model.Category;
import com.ecommerce.model.User;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.CategoryRepository;
import com.ecommerce.dto.ProductWithImageDto;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    // Save product (basic)
    public Product saveProduct(Product product) {
        return productRepository.save(product);
    }

    // Save product with image
    public Product saveProductWithImage(ProductWithImageDto productDto, User vendor) {
        Product product = new Product();
        product.setName(productDto.getName());
        product.setDescription(productDto.getDescription());
        product.setPrice(productDto.getPrice());
        product.setQuantity(productDto.getQuantity());
        product.setUser(vendor);

        Category category = categoryRepository.findById(productDto.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid category ID"));
        product.setCategory(category);

        try {
            if (productDto.getImageFile() != null && !productDto.getImageFile().isEmpty()) {
                product.setImageName(productDto.getImageFile().getOriginalFilename());
                product.setImageType(productDto.getImageFile().getContentType());
                product.setImageData(productDto.getImageFile().getBytes());
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to store image data", e);
        }

        return productRepository.save(product);
    }

    public List<Product> findAllProducts() {
        return productRepository.findAll();
    }

    public List<Product> findActiveProducts() {
        return productRepository.findByActiveTrue();
    }

    public Optional<Product> findById(Long id) {
        return productRepository.findById(id);
    }

    public List<Product> findByCategory(Category category) {
        return productRepository.findByCategoryAndActiveTrue(category);
    }

    public List<Product> findByVendor(User vendor) {
        return productRepository.findByUser(vendor);
    }

    public List<Product> searchProducts(String keyword) {
        return productRepository.findByNameContainingIgnoreCase(keyword);
    }

    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }

    public Product updateProduct(Product product) {
        return productRepository.save(product);
    }
}
