package com.ecommerce.dto;

import java.math.BigDecimal;

import com.ecommerce.model.QuantityType;
import org.springframework.web.multipart.MultipartFile;

public class ProductWithImageDto {

    private String name;
    private String description;
    private BigDecimal price;
    private Integer quantity;
    private Long categoryId;
    private MultipartFile imageFile;
    private QuantityType quantityType; // Added quantityType field

    // Getters and Setters
    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }

    public void setDescription(String description) { this.description = description; }

    public BigDecimal getPrice() { return price; }

    public void setPrice(BigDecimal price) { this.price = price; }

    public Integer getQuantity() { return quantity; }

    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public Long getCategoryId() { return categoryId; }

    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public MultipartFile getImageFile() { return imageFile; }

    public void setImageFile(MultipartFile imageFile) { this.imageFile = imageFile; }

    public QuantityType getQuantityType() { return quantityType; }

    public void setQuantityType(QuantityType quantityType) { this.quantityType = quantityType; }
}
