package com.dev_high.user.deposit.domain.entity;

import com.dev_high.common.type.DepositType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Table(name = "deposit_history", schema = "user")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DepositHistory {
    @Id
    @Column(name = "id", updatable = false, nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "user_id", length = 20, nullable = false, updatable = false)
    private String userId;

    /*
     * 추후 deposit_order 테이블의 외래 키 관계 명확화를 할수 있음
     * */
    @Column(name = "order_id", length = 20, updatable = false)
    private String orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20, updatable = false)
    private DepositType type;

    @Column(name = "amount", nullable = false, updatable = false)
    private BigDecimal amount;

    @Column(name = "balance", nullable = false, updatable = false)
    private BigDecimal balance;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "created_by", nullable = false, length = 20, updatable = false)
    private String createdBy;

    @Builder
    public DepositHistory(String userId,  String orderId, DepositType type, BigDecimal amount, BigDecimal balance) {
        this.userId = userId;
        this.orderId = orderId;
        this.type = type;
        this.amount = amount;
        this.balance = balance;
        this.createdBy = userId;
    }

    @PrePersist
    public void prePersist() {
        this.createdAt = OffsetDateTime.now();
    }

    public static DepositHistory create(String userId, String orderId, DepositType type, BigDecimal amount, BigDecimal currentBalance) {
        return DepositHistory.builder()
                .userId(userId)
                .orderId(orderId)
                .type(type)
                .amount(amount)
                .balance(currentBalance)
                .build();
    }
}
