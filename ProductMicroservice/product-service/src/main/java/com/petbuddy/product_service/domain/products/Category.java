package com.petbuddy.product_service.domain.products;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Table;

import java.util.HashSet;
import java.util.Set;

import org.checkerframework.common.aliasing.qual.Unique;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

@Entity
@Table(name="categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="category_id")
    private int categoryId;

    @Column(name="category_name")
    private String categoryName;

    @Column(name="slug")
    @Unique
    private String slug;

    @OneToMany(fetch=FetchType.LAZY, mappedBy = "Category", cascade = CascadeType.ALL)
    private Set<Products> products = new HashSet<>();

}
