package com.fitnesslife.gym.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import com.fitnesslife.gym.enums.AccessResult;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "attendances")
@CompoundIndexes({
        @CompoundIndex(name = "user_checkin_idx", def = "{'user': 1, 'checkIn': -1}"),
        @CompoundIndex(name = "user_active_idx", def = "{'user': 1, 'checkOut': 1}"),
        @CompoundIndex(name = "result_checkin_idx", def = "{'result': 1, 'checkIn': -1}"),
        @CompoundIndex(name = "idx_userId_result", def = "{'userId': 1, 'result': 1}"),
        @CompoundIndex(name = "idx_userId_result_checkIn", def = "{'userId': 1, 'result': 1, 'checkIn': -1}")
})
public class Attendance {

    @Id
    private String id;

    @DBRef(lazy = true)
    private User user;

    @Indexed
    private String userId;

    @Indexed
    private String qrCode;

    @Indexed
    private LocalDateTime checkIn;

    private LocalDateTime checkOut;

    @Indexed
    private AccessResult result;

    private String userName;
    private String userEmail;
    private String userPhoto;
    private String userPlan;
}
