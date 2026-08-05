package com.potential.goodquestion.domain.child.entity;

import com.potential.goodquestion.common.base.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Child 엔티티
 */
@Entity
@Table(name = "childs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Child extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
}
