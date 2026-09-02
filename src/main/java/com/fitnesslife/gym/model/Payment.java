package com.fitnesslife.gym.model;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import com.fitnesslife.gym.enums.PaymentStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "payments")
@CompoundIndexes({
        @CompoundIndex(name = "status_created_idx", def = "{'status': 1, 'createdAt': -1}"),
        @CompoundIndex(name = "user_status_idx", def = "{'user': 1, 'status': 1}"),
        @CompoundIndex(name = "validity_idx", def = "{'status': 1, 'validFrom': 1, 'validUntil': 1}"),
        @CompoundIndex(name = "userId_1_status_1", def = "{'userId': 1, 'status': 1}"),
        @CompoundIndex(name = "userId_1_validUntil_-1", def = "{'userId': 1, 'validUntil': -1}")
})
public class Payment {

    @Id
    private String id;

    @DBRef(lazy = true)
    private User user;

    @DBRef(lazy = true)
    private Plan plan;

    @Indexed
    private String userId;

    @Indexed
    private String externalInvoice;

    private Double amount;
    private String currency;

    @Indexed
    private PaymentStatus status;

    private LocalDateTime validFrom;
    private LocalDateTime validUntil;

    @Indexed
    private String transactionId;
    private String approvalCode;
    private String bankName;
    private String franchise;
    private String responseCode;
    private String responseText;
    private String responseReason;
    private String signature;

    @CreatedDate
    @Indexed
    private LocalDateTime createdAt;

    private String userName;
    private String userEmail;
    private String planName;

    private LocalDateTime transactionDate;

    public boolean isActive() {
        if (validFrom == null || validUntil == null) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        return PaymentStatus.ACCEPTED.equals(status) &&
                now.isAfter(validFrom) &&
                now.isBefore(validUntil);
    }
}
