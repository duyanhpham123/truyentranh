package project.truyentranh.model;

import java.io.Serializable;

public record FavoriteId(Long userId, Long comicId) implements Serializable {

}
