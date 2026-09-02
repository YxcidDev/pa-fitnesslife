package com.fitnesslife.gym.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.format.annotation.DateTimeFormat;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fitnesslife.gym.enums.ClassesStatus;

@Document(collection = "functionalTrainings")
@CompoundIndexes({
        @CompoundIndex(name = "status_datetime_idx", def = "{'status': 1, 'datetime': 1}"),
        @CompoundIndex(name = "datetime_status_idx", def = "{'datetime': 1, 'status': 1}")
})
public class FunctionalTraining {

    @Id
    private String id;

    @Indexed(unique = true)
    private int idFunctionalTraining;

    @Indexed
    private String nameTraining;

    private String instructor;
    private String description;
    private int maximumCapacity;
    private String duration;

    @Indexed
    private ClassesStatus status;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    @CreatedDate
    private Date createdAt;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    @Indexed
    private Date datetime;

    private String room;

    private List<Long> userIds = new ArrayList<>();

    public FunctionalTraining() {
    }

    public FunctionalTraining(String id, int idFunctionalTraining, String nameTraining, String instructor,
            String description,
            int maximumCapacity, String duration, ClassesStatus status, Date createdAt, Date datetime, String room,
            List<Long> userIds) {
        this.id = id;
        this.idFunctionalTraining = idFunctionalTraining;
        this.nameTraining = nameTraining;
        this.instructor = instructor;
        this.description = description;
        this.maximumCapacity = maximumCapacity;
        this.duration = duration;
        this.status = ClassesStatus.ACTIVE;
        this.createdAt = createdAt;
        this.datetime = (datetime != null) ? datetime : new Date();
        this.room = room;
        this.userIds = (userIds != null) ? userIds : new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getIdFunctionalTraining() {
        return idFunctionalTraining;
    }

    public void setIdFunctionalTraining(int idFunctionalTraining) {
        this.idFunctionalTraining = idFunctionalTraining;
    }

    public String getNameTraining() {
        return nameTraining;
    }

    public void setNameTraining(String nameTraining) {
        this.nameTraining = nameTraining;
    }

    public String getInstructor() {
        return instructor;
    }

    public void setInstructor(String instructor) {
        this.instructor = instructor;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getMaximumCapacity() {
        return maximumCapacity;
    }

    public void setMaximumCapacity(int maximumCapacity) {
        this.maximumCapacity = maximumCapacity;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public ClassesStatus getStatus() {
        return status;
    }

    public void setStatus(ClassesStatus status) {
        this.status = status;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getDatetime() {
        return datetime;
    }

    public void setDatetime(Date datetime) {
        this.datetime = datetime;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    public List<Long> getUserIds() {
        return userIds;
    }

    public void setUserIds(List<Long> userIds) {
        this.userIds = userIds;
    }
}
