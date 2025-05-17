package DTO;

import java.time.LocalDateTime;

public class ReminderDTO {

     // 필드
    private long reminderId; // 리마인드 식별자
    private long userId; // 사용자 식별자
    private long userLibraryId; // 라이브러리 식별자
    private String reminderType; //알림 주기 유형
    private long frequencyInterval; // 알람 간격
    private int dayOfWeek; // 주간 반복 시 요일
    private int dayOfMonth; // 월간 반복 시 요일
    private LocalDateTime baseDatetimeForRecurrence; // 반복 패턴의 기준이 되는 날짜 및 시간
    private LocalDateTime nextNotificationDatetime; // 다음 알림 예정 시간
    private String reminderNote; // 사용자 메모
    private boolean isActive; // 알림 활성화 여부
    private LocalDateTime createdAt ; // 생성 시간
    private LocalDateTime lastSentAt; // 마지막 알림 발송 시간

    // 기본 생성자
    public ReminderDTO(){}

    // getter

    public long getReminderId(){
        return reminderId;
    }
    public long getUserId(){
        return userId;
    }
    public long getUserLibraryId(){
        return userLibraryId;
    }
    public String getReminderType(){
        return reminderType;
    }
    public long getFrequencyInterval(){
        return frequencyInterval;
    }
    public int getDayOfWeek (){
        return dayOfWeek;
    }
    public int getDayOfMonth(){
        return dayOfMonth;
    }
    public LocalDateTime getBaseDatetimeForRecurrence(){
        return baseDatetimeForRecurrence;
    }
    public LocalDateTime getNextNotificationDatetime(){
        return nextNotificationDatetime;
    }
    public String getReminderNote(){
        return reminderNote;
    }
    public boolean getIsActive(){
        return isActive;
    }
    public LocalDateTime getCreatedAt(){
        return createdAt;
    }
    public LocalDateTime getLastSentAt(){
        return lastSentAt;
    }

    // setter

    public void setReminderId(long reminderId){
        this.reminderId = reminderId;
    }
    public void setUserId(long userId){
        this.userId = userId;
    }
    public void setUserLibraryId(long userLibraryId){
        this.userLibraryId = userLibraryId;
    }
    public void setReminderType(String reminderType){
        this.reminderType = reminderType;
    }
    public void setFrequencyInterval(long frequencyInterval){
        this.frequencyInterval = frequencyInterval;
    }
    public void setDayOfWeek(int dayOfWeek){
        this.dayOfWeek = dayOfWeek;
    }
    public void setDayOfMonth(int dayOfMonth){
        this.dayOfMonth = dayOfMonth;
    }
    public void setBaseDatetimeForRecurrence(LocalDateTime baseDatetimeForRecurrence){
        this.baseDatetimeForRecurrence = baseDatetimeForRecurrence;
    }
    public void setNextNotificationDatetime(LocalDateTime nextNotificationDatetime){
        this.nextNotificationDatetime = nextNotificationDatetime;
    }
    public void setReminderNote(String reminderNote){
        this.reminderNote = reminderNote;
    }
    public void setIsActive(boolean isActive){
        this.isActive = isActive;
    }
    public  void setCreatedAt(LocalDateTime createdAt){
        this.createdAt = createdAt;
    }
    public void setLastSentAt(LocalDateTime lastSentAt){
        this.lastSentAt=lastSentAt;
    }

}