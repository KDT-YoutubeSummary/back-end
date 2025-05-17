package entity;

import jakarta.persistence.*;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Tag {
    @Id
    @Column(name = "tag_id", nullable = false)
    private long tagId; // 태그 ID

    @Column(name = "tag_name" , length = 100, nullable = true)
    private String tagName; // 태그 이름
}
