package com.fitnesslife.gym.repository;

import com.fitnesslife.gym.enums.AccessResult;
import com.fitnesslife.gym.model.Attendance;
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
public interface AttendanceRepository extends MongoRepository<Attendance, String> {

        @Query("{ 'user.$id': { $oid: ?0 }, 'checkOut': null, 'result': 'ALLOWED' }")
        Optional<Attendance> findActiveAttendanceByUserId(String userId);

        List<Attendance> findByUserOrderByCheckInDesc(User user);

        @Query("{ 'checkOut': null, 'result': 'ALLOWED' }")
        List<Attendance> findAllActiveAttendances();

        Page<Attendance> findAll(Pageable pageable);

        Page<Attendance> findByResult(AccessResult result, Pageable pageable);

        @Query("{ $or: [ " +
                        "{ 'userName':  { $regex: ?0, $options: 'i' } }, " +
                        "{ 'userEmail': { $regex: ?0, $options: 'i' } }, " +
                        "{ 'qrCode':    { $regex: ?0, $options: 'i' } } " +
                        "] }")
        Page<Attendance> searchAttendances(String searchTerm, Pageable pageable);

        @Query("{ 'result': ?0, $or: [ " +
                        "{ 'userName':  { $regex: ?1, $options: 'i' } }, " +
                        "{ 'userEmail': { $regex: ?1, $options: 'i' } }, " +
                        "{ 'qrCode':    { $regex: ?1, $options: 'i' } } " +
                        "] }")
        Page<Attendance> searchAttendancesByResult(AccessResult result, String searchTerm, Pageable pageable);

        @Query(value = "{ 'checkIn': { $gte: ?0, $lte: ?1 } }", count = true)
        long countByCheckInBetween(LocalDateTime start, LocalDateTime end);

        @Query(value = "{ 'result': 'ALLOWED', 'checkIn': { $gte: ?0, $lte: ?1 } }", count = true)
        long countAllowedAccessesBetween(LocalDateTime start, LocalDateTime end);

        @Query("{ 'result': 'ALLOWED', 'checkIn': { $gte: ?0, $lte: ?1 } }")
        List<Attendance> findAllowedAccessesBetween(LocalDateTime start, LocalDateTime end);

        @Query("{ 'result': 'ALLOWED', 'checkIn': { $gte: ?0, $lte: ?1 } }")
        List<Attendance> findAllowedAccessesForPeakHours(LocalDateTime start, LocalDateTime end);

        List<Attendance> findByUserAndResult(User user, AccessResult result);

        @Query("{ 'userId': { $in: ?0 }, 'result': 'ALLOWED' }")
        List<Attendance> findByUserIdInAndResultAllowed(List<String> userIds);

        @Query(value = "{ 'userId': { $in: ?0 }, 'result': 'ALLOWED' }", fields = "{ 'userId': 1, 'checkIn': 1, 'checkOut': 1 }")
        List<Attendance> findChurnFieldsByUserIdIn(List<String> userIds);
}