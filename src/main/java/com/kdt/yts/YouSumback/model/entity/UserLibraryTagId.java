package com.kdt.yts.YouSumback.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class UserLibraryTagId implements Serializable {

    // 필드
    @Column(name = "user_library_id")
    private long userLibraryId;
    @Column(name = "tag_id")
    private Integer tagId;

    // 생성자
    public UserLibraryTagId() {
    }

    public UserLibraryTagId(long userLibraryId, Integer tagId) {
        this.userLibraryId = userLibraryId;
        this.tagId = tagId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserLibraryTagId that = (UserLibraryTagId) o;
        return userLibraryId == that.userLibraryId && tagId.equals(that.tagId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userLibraryId, tagId);
    }

}
