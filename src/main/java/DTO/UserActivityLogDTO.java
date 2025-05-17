package DTO;

import java.time.LocalDateTime;

public class UserActivityLogDTO
{
    // 필드
    private long logId; // 로그 식별자 아이디
    private String userId; // 사용자 아이디
    private long sessionId; // 비로그인 사용자 식별용 ID
    private String activityType; // 활동 유형
    private String targetEntityType; // 활동 대상 유형
    private String targetEntityIdStr; // 활동 대상 문자열 ID
    private long targetEntityIdInt; // 활동 대상 정수형 ID
    private LocalDateTime createdAt; // 활동 발생 일자
    private String activityDetail; // 활동 상세 내용

    // 기본 생성자
    public UserActivityLogDTO(){}

    // getter

    public long getLogId(){
        return logId;
    }
    public String getUserId(){
        return userId;
    }
    public long getSessionId(){
        return sessionId;
    }
    public String getActivityType(){
        return activityType;
    }
    public String getTargetEntityType(){
        return  targetEntityType;
    }
    public String getTargetEntityIdStr(){
        return targetEntityIdStr;
    }

    public long getTargetEntityIdInt(){
        return  targetEntityIdInt;
    }
    public LocalDateTime getCreatedAt(){
        return  createdAt;
    }
    public String getActivityDetail(){
        return activityDetail;
    }

    // setter

    public void setLogId(long logId){
        this.logId = logId;
    }
    public void setUserId(String userId){
        this.userId = userId;
    }
    public void setSessionId(long sessionId){
        this.sessionId = sessionId;
    }
    public void setActivityType (String activityType)
    {
        this.activityType = activityType;
    }
    public void setTargetEntityType(String targetEntityType){
        this.targetEntityType=targetEntityType;
    }
    public void setTargetEntityIdStr(String targetEntityIdStr)
    {
        this.targetEntityIdStr = targetEntityIdStr;
    }
    public void  setTargetEntityIdInt(long targetEntityIdInt){
        this.targetEntityIdInt = targetEntityIdInt;
    }
    public void setCreatedAt(LocalDateTime createdAt){
        this.createdAt = createdAt;
    }
    public void setActivityDetail (String activityDetail){
        this.activityDetail = activityDetail;
    }











}
