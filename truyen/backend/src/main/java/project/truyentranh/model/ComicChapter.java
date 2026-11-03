package project.truyentranh.model;

import java.math.BigDecimal;
import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "comic_chapters")
public class ComicChapter {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "comic_id")
	private Comic comic;

	private BigDecimal chapterNumber;

	private String chapterTitle;

	@Enumerated(EnumType.STRING)
	private ChapterStatus status = ChapterStatus.DRAFT;

	private long views;

	private Instant publishedAt;

	@CreationTimestamp
	private Instant createdAt;

	private Instant deletedAt;

}
