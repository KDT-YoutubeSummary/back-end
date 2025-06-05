package com.kdt.yts.YouSumback.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
//@Table(name = "user_library_tag_id")
public class UserLibraryTagId implements Serializable {

    // 필드
    @Column(name = "user_library_id")
    private Long id;
    @Column(name = "tag_id")
    private Long tagId;

    // 생성자
    public UserLibraryTagId() {
    }

    public UserLibraryTagId(Long userLibraryId, Long tagId) {
        this.id = userLibraryId;
        this.tagId = tagId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserLibraryTagId that = (UserLibraryTagId) o;
        return id == that.id && tagId == that.tagId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, tagId);
    }

}
