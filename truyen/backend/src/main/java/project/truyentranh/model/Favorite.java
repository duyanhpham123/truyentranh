package project.truyentranh.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "favorites")
@IdClass(FavoriteId.class)
public class Favorite {

	@Id
	private Long userId;

	@Id
	@Column(name = "comic_id")
	private Long comicId;

	private Instant createdAt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "comic_id", insertable = false, updatable = false)
	private Comic comic;

}
