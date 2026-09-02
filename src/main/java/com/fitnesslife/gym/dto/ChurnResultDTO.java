package com.fitnesslife.gym.dto;

import java.util.List;

public class ChurnResultDTO {

    private String  userId;
    private String  name;
    private String  plan;
    private int     planLevel;
    private String  profile;
    private int     nPayments;
    private long    daysSinceLastPayment;
    private int     nAttendances;
    private double  avgSessionMinutes;
    private long    daysSinceLastAtt;
    private boolean participatesInClasses;
    private String  prediction;
    private double  predictionConfidence;
    private String  riskLevel;
    private List<String> reasons;
    private List<String> recommendations;
    private String  email;
    private String  phone;

    public ChurnResultDTO() {}

    public String getUserId()                     { return userId; }
    public void   setUserId(String v)             { this.userId = v; }

    public String getName()                       { return name; }
    public void   setName(String v)               { this.name = v; }

    public String getPlan()                       { return plan; }
    public void   setPlan(String v)               { this.plan = v; }

    public int    getPlanLevel()                  { return planLevel; }
    public void   setPlanLevel(int v)             { this.planLevel = v; }

    public String getProfile()                    { return profile; }
    public void   setProfile(String v)            { this.profile = v; }

    public int    getNPayments()                  { return nPayments; }
    public void   setNPayments(int v)             { this.nPayments = v; }

    public long   getDaysSinceLastPayment()       { return daysSinceLastPayment; }
    public void   setDaysSinceLastPayment(long v) { this.daysSinceLastPayment = v; }

    public int    getNAttendances()               { return nAttendances; }
    public void   setNAttendances(int v)          { this.nAttendances = v; }

    public double getAvgSessionMinutes()          { return avgSessionMinutes; }
    public void   setAvgSessionMinutes(double v)  { this.avgSessionMinutes = v; }

    public long   getDaysSinceLastAtt()           { return daysSinceLastAtt; }
    public void   setDaysSinceLastAtt(long v)     { this.daysSinceLastAtt = v; }

    public boolean isParticipatesInClasses()          { return participatesInClasses; }
    public void    setParticipatesInClasses(boolean v){ this.participatesInClasses = v; }

    public String getPrediction()                 { return prediction; }
    public void   setPrediction(String v)         { this.prediction = v; }

    public double getPredictionConfidence()           { return predictionConfidence; }
    public void   setPredictionConfidence(double v)   { this.predictionConfidence = v; }

    public String getRiskLevel()                  { return riskLevel; }
    public void   setRiskLevel(String v)          { this.riskLevel = v; }

    public List<String> getReasons()              { return reasons; }
    public void   setReasons(List<String> v)      { this.reasons = v; }

    public List<String> getRecommendations()      { return recommendations; }
    public void   setRecommendations(List<String> v) { this.recommendations = v; }

    public String getEmail()                      { return email; }
    public void   setEmail(String v)              { this.email = v; }

    public String getPhone()                      { return phone; }
    public void   setPhone(String v)              { this.phone = v; }
}