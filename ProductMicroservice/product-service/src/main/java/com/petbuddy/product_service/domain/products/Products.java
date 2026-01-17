package com.petbuddy.product_service.domain.products;

import java.util.List;

import org.hibernate.annotations.Type;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "products")
public class Products {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="product_id")
    private int productId;
    @Column(name="product_name")
    private String productName;
    @Column(name="product_description")
    private String productDescription;
    @Column(name="base_price")
    private int productPrice;
    @Column(name="media_urls")
    private List<String> productImageUrl;
    @Column(name="stock")
    private int productStock;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id", nullable = false)
    private Brand brand;

    private boolean isDeleted;

    @Column(name="attr",columnDefinition = "jsonb")
    private String attributes;
    
    
}
