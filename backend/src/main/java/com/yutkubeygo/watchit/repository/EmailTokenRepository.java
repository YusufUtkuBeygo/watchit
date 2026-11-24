package com.yutkubeygo.watchit.repository;

import com.yutkubeygo.watchit.entity.EmailToken;
import com.yutkubeygo.watchit.enums.EmailTokenPurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmailTokenRepository extends JpaRepository<EmailToken, Long> {

    //Kullanıcının bu amaçla gönderilmiş, henüz kullanılmamış en son kodu
    Optional<EmailToken> findFirstByUserIdAndPurposeAndUsedAtIsNullOrderByCreatedAtDescIdDesc(Long userId, EmailTokenPurpose purpose);

    //Bağlantıdaki token'ın hash'iyle arama (şifre sıfırlama: kullanıcı kim, bağlantıdan anlaşılır)
    List<EmailToken> findByTokenHashAndPurpose(String tokenHash, EmailTokenPurpose purpose);

    //Son X dakikada kaç kod gönderilmiş (tekrar gönderme ve şifre sıfırlama isteği sınırı)
    long countByUserIdAndPurposeAndCreatedAtAfter(Long userId, EmailTokenPurpose purpose, LocalDateTime after);

    //Hesap silinirken kodları da sil (UserService.deleteUserById)
    void deleteByUserId(Long userId);

    //Yeni kod gönderilince ya da biri kullanılınca eskilerin hepsi geçersiz olur: aynı anda tek geçerli kod
    @Modifying
    @Transactional
    @Query("UPDATE EmailToken t SET t.usedAt = :now WHERE t.user.id = :userId AND t.purpose = :purpose AND t.usedAt IS NULL")
    int invalidateAll(@Param("userId") Long userId, @Param("purpose") EmailTokenPurpose purpose, @Param("now") LocalDateTime now);
}
