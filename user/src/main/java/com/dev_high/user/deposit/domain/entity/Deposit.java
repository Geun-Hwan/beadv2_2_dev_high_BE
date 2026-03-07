package com.dev_high.user.deposit.domain.entity;

import com.dev_high.common.exception.CustomException;
import com.dev_high.common.type.DepositType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Table(name = "deposit", schema = "user")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Deposit {
    @Id
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    @Column(name = "user_id", length = 20, nullable = false)
    private String userId;

    @Column(name = "balance", nullable = false)
    private BigDecimal balance; // 예치금 잔액

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "created_by", length = 20, nullable = false)
    private String createdBy;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "updated_by", length = 20, nullable = false)
    private String updatedBy;

    @Builder
    public Deposit(UUID id, String userId, BigDecimal balance) {
        this.id = id;
        this.userId = userId;
        this.balance = balance;
    }

    @PrePersist
    public void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        this.createdBy = userId;
        this.updatedBy = userId;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = OffsetDateTime.now();
        this.updatedBy = userId;
    }

    public static Deposit create(String userId) {
        return Deposit.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .balance(BigDecimal.ZERO)
                .build();
    }

    public void apply(DepositType type, BigDecimal amount) {
        switch (type) {
            case CHARGE, REFUND -> increaseBalance(amount);
            case USAGE, PAYMENT, DEPOSIT, DEDUCT -> decreaseBalance(amount);
            default -> throw new IllegalArgumentException("지원하지 않는 예치금 유형: " + type);
        }
    }

    public void compensate(DepositType type, BigDecimal amount) {
        switch (type) {
            case CHARGE, REFUND -> decreaseBalance(amount);
            case USAGE, PAYMENT, DEPOSIT, DEDUCT -> increaseBalance(amount);
            default -> throw new IllegalArgumentException("지원하지 않는 예치금 유형: " + type);
        }
    }

    public void increaseBalance(BigDecimal amount) {
        this.balance = this.balance.add(amount);
    }

    public void decreaseBalance(BigDecimal amount) {
        if (this.balance.compareTo(amount) < 0) {
            throw new CustomException(String.format("잔액이 부족합니다. (현재 잔액: %s, 요청 금액: %s)", this.balance.toPlainString(), amount.toPlainString()));
        }
        this.balance = this.balance.subtract(amount);
    }
}
