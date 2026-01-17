package com.petbuddy.product_service.domain.products;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name="brands")
public class Brand {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="brand_id")
    private int brandId;

    @Column(name="brand_name")
    private String brandName;

    @Column(name="logo_url")
    private String logoUrl;

    @OneToMany(fetch=FetchType.LAZY, mappedBy = "Brand", cascade = CascadeType.ALL)
    private Set<Products> products = new HashSet<>();
    
}
