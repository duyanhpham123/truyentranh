package project.truyentranh.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import project.truyentranh.model.TokenType;
import project.truyentranh.model.UserToken;

public interface UserTokenRepository extends JpaRepository<UserToken, Long> {

	Optional<UserToken> findByTokenHashAndType(String tokenHash, TokenType type);

}
