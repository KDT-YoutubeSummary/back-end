package com.YouSumback.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserLibraryTagId implements Serializable {

    // 필드
    @Column(name = "user_library_id")
    private long userLibraryId;
    @Column(name = "tag_id")
    private long tagId;



    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserLibraryTagId that = (UserLibraryTagId) o;
        return userLibraryId == that.userLibraryId && tagId == that.tagId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(userLibraryId, tagId);
    }

}
