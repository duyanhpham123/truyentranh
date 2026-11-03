package project.truyentranh.model;

import java.time.Instant;

import jakarta.persistence.Entity;
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
@Table(name = "reading_histories")
public class ReadingHistory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private Long userId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "comic_id")
	private Comic comic;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "chapter_id")
	private ComicChapter chapter;

	private int lastPage;

	private Instant lastReadAt;

}
