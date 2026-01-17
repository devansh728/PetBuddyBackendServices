package com.petbuddy.product_service.domain.products;


import java.util.Date;

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
@Table(name="reviews")
public class Reviews {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int reviewId;
    @Column(name="user_id")
    private int userId;
    
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Products product;

    private int rating;
    private String comment;
    private Date reviewDate;
    private boolean isDeleted;
}
