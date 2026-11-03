package project.truyentranh.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import project.truyentranh.model.Author;

public interface AuthorRepository extends JpaRepository<Author, Long> {

	List<Author> findAllByOrderByNameAsc();

}
