package DTO;

public class TagDTO {

    // 필드
    private long tagId; // 태그 ID
    private String tagName; // 태그 이름

    // 생성자
    public TagDTO() {}

    // getter
    public long getTagId() {
        return tagId;
    }
    public String getTagName() {
        return tagName;
    }

    // setter
    public void setTagId(long tagId) {
        this.tagId = tagId;
    }
    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

}
