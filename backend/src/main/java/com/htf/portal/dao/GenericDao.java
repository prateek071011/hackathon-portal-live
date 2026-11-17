package com.htf.portal.dao;

import java.util.List;
import java.util.Optional;

/**
 * The basic operations every DAO offers, written once with generics.
 * T is the model type (Team, User, ...) and ID is the type of its primary key (Long for us).
 * Each table's DAO interface extends this and adds its own queries, e.g. TeamDao.findOpenTeams(...).
 */
public interface GenericDao<T, ID> {

    // Optional instead of null: the caller must decide what "not found" means (usually orElseThrow).
    Optional<T> findById(ID id);

    List<T> findAll();

    // Inserts the row, copies the generated id into the entity, and returns it.
    T insert(T entity);

    // Returns true if a row was changed.
    boolean update(T entity);

    // Returns true if a row was deleted.
    boolean deleteById(ID id);
}
