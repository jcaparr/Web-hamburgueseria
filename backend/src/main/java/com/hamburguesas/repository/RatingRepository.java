package com.hamburguesas.repository;

import com.hamburguesas.dto.MyRatingDto;
import com.hamburguesas.dto.RankingItemDto;
import com.hamburguesas.model.Rating;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RatingRepository extends JpaRepository<Rating, Long> {

    Page<Rating> findByBurgerJoint_IdOrderByCreatedAtDesc(Long burgerJointId, Pageable pageable);

    Optional<Rating> findByUser_IdAndBurgerJoint_Id(Long userId, Long burgerJointId);

    @Query("""
        select new com.hamburguesas.dto.RankingItemDto(
            b.id, b.placeId, b.name, b.address, b.area, b.photoUrl, b.latitude, b.longitude,
            avg(r.score), count(r), null)
        from Rating r join r.burgerJoint b
        where (:area is null or b.area = :area)
        group by b.id, b.placeId, b.name, b.address, b.area, b.photoUrl, b.latitude, b.longitude
        order by avg(r.score) desc
        """)
    Page<RankingItemDto> rankingByScore(@Param("area") String area, Pageable pageable);

    @Query("""
        select new com.hamburguesas.dto.RankingItemDto(
            b.id, b.placeId, b.name, b.address, b.area, b.photoUrl, b.latitude, b.longitude,
            avg(r.score), count(r), null)
        from Rating r join r.burgerJoint b
        where (:area is null or b.area = :area)
        group by b.id, b.placeId, b.name, b.address, b.area, b.photoUrl, b.latitude, b.longitude
        order by count(r) desc
        """)
    Page<RankingItemDto> rankingByPopularity(@Param("area") String area, Pageable pageable);

    @Query("""
        select new com.hamburguesas.dto.RankingItemDto(
            b.id, b.placeId, b.name, b.address, b.area, b.photoUrl, b.latitude, b.longitude,
            avg(r.score), count(r), max(case when r.user.id = :userId then r.score else null end))
        from Rating r join r.burgerJoint b
        where b.id in (
            select r2.burgerJoint.id from Rating r2 where r2.user.id = :userId
        )
        group by b.id, b.placeId, b.name, b.address, b.area, b.photoUrl, b.latitude, b.longitude
        order by max(case when r.user.id = :userId then r.score else null end) desc
        """)
    Page<RankingItemDto> personalRanking(@Param("userId") Long userId, Pageable pageable);

    @Query("select avg(r.score) from Rating r where r.burgerJoint.id = :burgerJointId")
    Double averageScoreByBurgerJoint(@Param("burgerJointId") Long burgerJointId);

    long countByBurgerJoint_Id(Long burgerJointId);

    long countByUser_Id(Long userId);

    @Query("select avg(r.score) from Rating r where r.user.id = :userId")
    Double averageScoreByUser(@Param("userId") Long userId);

    @Query("""
        select new com.hamburguesas.dto.MyRatingDto(
            r.id, b.id, b.name, b.photoUrl, r.score, r.comment, r.createdAt)
        from Rating r join r.burgerJoint b
        where r.user.id = :userId
        order by r.createdAt desc
        """)
    List<MyRatingDto> findAllByUserOrderByCreatedAtDesc(@Param("userId") Long userId);
}
