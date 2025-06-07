package com.kdt.yts.YouSumback.model.entity;

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
    private Long userLibraryId;
    @Column(name = "tag_id")
    private Integer tagId;



    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserLibraryTagId)) return false;
        UserLibraryTagId that = (UserLibraryTagId) o;
        return Objects.equals(userLibraryId, that.userLibraryId)
                && Objects.equals(tagId, that.tagId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userLibraryId, tagId);
    }

}
