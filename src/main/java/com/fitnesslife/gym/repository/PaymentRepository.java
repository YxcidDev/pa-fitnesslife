package com.fitnesslife.gym.repository;

import com.fitnesslife.gym.enums.PaymentStatus;
import com.fitnesslife.gym.model.Payment;
import com.fitnesslife.gym.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends MongoRepository<Payment, String> {

        Optional<Payment> findByExternalInvoice(String externalInvoice);

        Optional<Payment> findByTransactionId(String transactionId);

        List<Payment> findByUser(User user);

        List<Payment> findByUserOrderByCreatedAtDesc(User user);

        List<Payment> findByUserAndStatus(User user, PaymentStatus status);

        List<Payment> findByUserAndStatusAndValidFromLessThanEqualAndValidUntilGreaterThanEqual(
                        User user,
                        PaymentStatus status,
                        LocalDateTime validFrom,
                        LocalDateTime validUntil);

        Optional<Payment> findFirstByUserAndStatusOrderByCreatedAtDesc(User user, PaymentStatus status);

        @Query("{ 'status': 'ACCEPTED', 'validUntil': { $lt: ?0 } }")
        List<Payment> findExpiredPayments(LocalDateTime now);

        Page<Payment> findAll(Pageable pageable);

        Page<Payment> findByStatus(PaymentStatus status, Pageable pageable);

        @Query("{ $or: [ " +
                        "{ 'userName': { $regex: ?0, $options: 'i' } }, " +
                        "{ 'userEmail': { $regex: ?0, $options: 'i' } }, " +
                        "{ 'transactionId': { $regex: ?0, $options: 'i' } }, " +
                        "{ 'externalInvoice': { $regex: ?0, $options: 'i' } } " +
                        "] }")
        Page<Payment> searchPayments(String searchTerm, Pageable pageable);

        @Query("{ 'status': ?0, $or: [ " +
                        "{ 'userName': { $regex: ?1, $options: 'i' } }, " +
                        "{ 'userEmail': { $regex: ?1, $options: 'i' } }, " +
                        "{ 'transactionId': { $regex: ?1, $options: 'i' } }, " +
                        "{ 'externalInvoice': { $regex: ?1, $options: 'i' } } " +
                        "] }")
        Page<Payment> searchPaymentsByStatus(PaymentStatus status, String searchTerm, Pageable pageable);

        @Query(value = "{ 'status': 'ACCEPTED', 'createdAt': { $gte: ?0, $lte: ?1 } }")
        List<Payment> findAcceptedPaymentsBetween(LocalDateTime start, LocalDateTime end);

        @Query(value = "{ 'status': 'ACCEPTED', 'validUntil': { $gte: ?0 } }", count = true)
        long countActivePlans(LocalDateTime now);

        @Query(value = "{ 'status': 'ACCEPTED', 'validUntil': { $gte: ?0, $lte: ?1 } }")
        List<Payment> findExpiringPlans(LocalDateTime start, LocalDateTime end);

        @Query(value = "{ 'status': 'ACCEPTED' }", sort = "{ 'createdAt': -1 }")
        List<Payment> findRecentPayments(Pageable pageable);

        @Query(value = "{ 'status': 'ACCEPTED', 'createdAt': { $gte: ?0, $lte: ?1 } }")
        List<Payment> findAcceptedPaymentsForStats(LocalDateTime start, LocalDateTime end);

        @Query(value = "{ 'userId': { $in: ?0 }, 'status': 'ACCEPTED' }", fields = "{ 'userId': 1, 'validUntil': 1, 'status': 1 }")
        List<Payment> findChurnFieldsByUserIdIn(List<String> userIds);
}
