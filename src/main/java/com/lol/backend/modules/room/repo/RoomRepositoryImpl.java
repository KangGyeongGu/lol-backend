package com.lol.backend.modules.room.repo;

import com.lol.backend.modules.game.entity.GameType;
import com.lol.backend.modules.room.entity.Room;
import com.lol.backend.modules.user.entity.Language;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class RoomRepositoryImpl implements RoomRepositoryCustom {

    private final EntityManager em;

    public RoomRepositoryImpl(EntityManager em) {
        this.em = em;
    }

    @Override
    public List<Room> findRoomsWithFilters(String roomName,
                                           Language language,
                                           GameType gameType,
                                           Instant cursorUpdatedAt,
                                           int limit) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Room> cq = cb.createQuery(Room.class);
        Root<Room> root = cq.from(Room.class);

        List<Predicate> predicates = new ArrayList<>();

        if (roomName != null) {
            predicates.add(cb.like(root.get("roomName"), "%" + roomName + "%"));
        }
        if (language != null) {
            predicates.add(cb.equal(root.get("language"), language));
        }
        if (gameType != null) {
            predicates.add(cb.equal(root.get("gameType"), gameType));
        }
        if (cursorUpdatedAt != null) {
            predicates.add(cb.lessThan(root.get("updatedAt"), cursorUpdatedAt));
        }

        cq.where(predicates.toArray(new Predicate[0]));
        cq.orderBy(cb.desc(root.get("updatedAt")));

        TypedQuery<Room> query = em.createQuery(cq);
        query.setMaxResults(limit);

        return query.getResultList();
    }
}
