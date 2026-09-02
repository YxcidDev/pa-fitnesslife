package com.fitnesslife.gym.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import com.fitnesslife.gym.enums.BloodType;
import com.fitnesslife.gym.enums.Role;
import com.fitnesslife.gym.enums.Sex;

@Document(collection = "users")
@CompoundIndexes({
        @CompoundIndex(name = "role_active_idx", def = "{'role': 1, 'isActive': 1}"),
        @CompoundIndex(name = "role_created_idx", def = "{'role': 1, 'createdAt': -1}"),
        @CompoundIndex(name = "active_created_idx", def = "{'isActive': 1, 'createdAt': -1}")
})
public class User implements Serializable {

    @Id
    private String id;

    @Indexed(unique = true)
    private Long identification;

    @Indexed
    private String name;

    @Indexed
    private String lastname;

    private Sex sex;
    private LocalDate birthDate;
    private BloodType bloodType;

    @Indexed(unique = true)
    private String email;

    private String phone;
    private String password;
    private String photoProfile;

    @Indexed
    private Role role;

    private String plan;
    private boolean isActive;
    private String qrCodePath;

    @CreatedDate
    @Indexed
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    private LocalDateTime lastLogin;

    public User() {
    }

    public User(String id, Long identification, String name, String lastname, Sex sex, LocalDate birthDate,
            BloodType bloodType, String email, String phone, String password, String photoProfile,
            Role role, String plan, boolean isActive, String qrCodePath, LocalDateTime lastLogin) {

        this.id = id;
        this.identification = identification;
        this.name = name;
        this.lastname = lastname;
        this.sex = sex;
        this.birthDate = birthDate;
        this.bloodType = bloodType;
        this.email = email;
        this.phone = phone;
        this.password = password;
        this.photoProfile = photoProfile;
        this.role = Role.USER;
        this.plan = plan;
        this.isActive = true;
        this.qrCodePath = qrCodePath;
        this.lastLogin = lastLogin;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getIdentification() {
        return identification;
    }

    public void setIdentification(Long identification) {
        this.identification = identification;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public Sex getSex() {
        return sex;
    }

    public void setSex(Sex sex) {
        this.sex = sex;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public BloodType getBloodType() {
        return bloodType;
    }

    public void setBloodType(BloodType bloodType) {
        this.bloodType = bloodType;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPhotoProfile() {
        return photoProfile;
    }

    public void setPhotoProfile(String photoProfile) {
        this.photoProfile = photoProfile;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getPlan() {
        return plan;
    }

    public void setPlan(String plan) {
        this.plan = plan;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean isActive) {
        this.isActive = isActive;
    }

    public String getQrCodePath() {
        return qrCodePath;
    }

    public void setQrCodePath(String qrCodePath) {
        this.qrCodePath = qrCodePath;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }
}
