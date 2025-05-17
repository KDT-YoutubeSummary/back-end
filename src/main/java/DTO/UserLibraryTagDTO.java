package DTO;

public class UserLibraryTagDTO {

    // 필드
    private long userLibraryID;
    private long tagID;

    // 생성자
    public UserLibraryTagDTO() {}
    public UserLibraryTagDTO(long userLibraryID, long tagID) {
        this.userLibraryID = userLibraryID;
        this.tagID = tagID;
    }

    // getter
    public long getUserLibraryID() {
        return userLibraryID;
    }
    public long getTagID() {
        return tagID;
    }

    // setter
    public void setUserLibraryID(long userLibraryID) {
        this.userLibraryID = userLibraryID;
    }
    public void setTagID(long tagID) {
        this.tagID = tagID;
    }
}
