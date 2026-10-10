package com.example.service;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;

public class CrudService<T> {

    private final EntityManagerFactory emf;
    private final Class<T> entityClass;

    public CrudService(
            EntityManagerFactory emf,
            Class<T> entityClass
    ) {
        this.emf = emf;
        this.entityClass = entityClass;
    }

    public void creer(T entite) {
        dansTransaction(em -> {
            em.persist(entite);
            return null;
        });
    }

    public T trouver(Long id) {
        return avecEntityManager(
                em -> em.find(entityClass, id)
        );
    }
    public List<T> rechercher(
            String attribut,
            String prefixe,
            Class<? extends T> sousType
    ) {
        return avecEntityManager(em -> {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<T> query =
                    cb.createQuery(entityClass);
            Root<T> root = query.from(entityClass);

            query.select(root)
                    .where(cb.and(
                            cb.like(
                                    cb.lower(root.<String>get(attribut)),
                                    prefixe.toLowerCase(
                                            java.util.Locale.ROOT
                                    ) + "%"
                            ),
                            cb.equal(root.type(), sousType)
                    ))
                    .orderBy(cb.asc(root.get("id")));

            return em.createQuery(query).getResultList();
        });
    }
    public List<T> lister() {
        return avecEntityManager(em -> {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<T> query =
                    cb.createQuery(entityClass);
            Root<T> root = query.from(entityClass);

            query.select(root)
                    .orderBy(cb.asc(root.get("id")));

            return em.createQuery(query).getResultList();
        });
    }

    public boolean modifier(
            Long id,
            Consumer<T> modification
    ) {
        return dansTransaction(em -> {
            T entite = em.find(entityClass, id);

            if (entite == null) {
                return false;
            }

            modification.accept(entite);
            return true;
        });
    }

    public boolean supprimer(Long id) {
        return dansTransaction(em -> {
            T entite = em.find(entityClass, id);

            if (entite == null) {
                return false;
            }

            em.remove(entite);
            return true;
        });
    }

    public List<T> rechercherParMarqueEtType(
            String debutMarque,
            Class<? extends T> sousType
    ) {
        return avecEntityManager(em -> {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<T> query =
                    cb.createQuery(entityClass);
            Root<T> root = query.from(entityClass);

            query.select(root)
                    .where(cb.and(
                            cb.like(
                                    cb.lower(root.<String>get("marque")),
                                    debutMarque
                                            .toLowerCase(Locale.ROOT) + "%"
                            ),
                            cb.equal(root.type(), sousType)
                    ))
                    .orderBy(cb.asc(root.get("id")));

            return em.createQuery(query).getResultList();
        });
    }

    private <R> R avecEntityManager(
            Function<EntityManager, R> operation
    ) {
        EntityManager em = emf.createEntityManager();

        try {
            return operation.apply(em);
        } finally {
            em.close();
        }
    }

    private <R> R dansTransaction(
            Function<EntityManager, R> operation
    ) {
        return avecEntityManager(em -> {
            EntityTransaction transaction =
                    em.getTransaction();

            transaction.begin();

            try {
                R resultat = operation.apply(em);
                transaction.commit();
                return resultat;

            } catch (RuntimeException exception) {
                if (transaction.isActive()) {
                    transaction.rollback();
                }
                throw exception;
            }
        });
    }
}