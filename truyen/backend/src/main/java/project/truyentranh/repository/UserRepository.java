package project.truyentranh.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import project.truyentranh.model.User;

public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByEmail(String email);

	boolean existsByEmail(String email);

	boolean existsByUsername(String username);

	boolean existsByAvatarUrl(String avatarUrl);

	@Query("""
			select u from User u
			where (:keyword is null or u.email like concat('%', :keyword, '%')
			       or u.username like concat('%', :keyword, '%')
			       or u.fullName like concat('%', :keyword, '%'))
			""")
	Page<User> search(@Param("keyword") String keyword, Pageable pageable);

}
