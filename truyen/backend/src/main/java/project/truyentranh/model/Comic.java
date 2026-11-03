package project.truyentranh.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "comics")
public class Comic {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String slug;

	private String title;

	@Column(columnDefinition = "text")
	private String description;

	private String coverUrl;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "author_id")
	private Author author;

	@Enumerated(EnumType.STRING)
	private ComicStatus status = ComicStatus.ONGOING;

	@Column(name = "is_hidden")
	private boolean hidden;

	private long views;

	private BigDecimal ratingAvg = BigDecimal.ZERO;

	private int ratingCount;

	private Instant lastChapterAt;

	@CreationTimestamp
	private Instant createdAt;

	@UpdateTimestamp
	private Instant updatedAt;

	private Instant deletedAt;

	@ManyToMany
	@JoinTable(name = "comic_genres", joinColumns = @JoinColumn(name = "comic_id"),
			inverseJoinColumns = @JoinColumn(name = "genre_id"))
	@BatchSize(size = 100)
	private Set<Genre> genres = new HashSet<>();

}
