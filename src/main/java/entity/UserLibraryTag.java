package entity;

import jakarta.persistence.*;

@Entity
@Table(name = "UserLibraryTag")
public class UserLibraryTag {

    @EmbeddedId
    private UserLibraryTagId id;

    @ManyToOne
    @MapsId("userLibraryId")
    @JoinColumn(name = "user_library_id")
    private UserLibrary userLibrary; // 라이브러리 식별자

    @ManyToOne
    @MapsId("tagId")
    @JoinColumn(name = "tag_id")
    private Tag tag; // 태그 식별자
}