package com.yutkubeygo.watchit.repository;

import com.yutkubeygo.watchit.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User,Long> {

    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    //IgnoreCase: büyük/küçük harf farkı gözetmeden karşılaştırır ("Ayse" = "ayse")
    boolean existsByUsernameIgnoreCase(String username);


    //Her istekte token kontrolü için: kullanıcının tamamı değil, yalnız iki alan okunur (hafif sorgu).
    //Boş dönerse kullanıcı silinmiş demektir.
    interface TokenCheck {
        Long getId();
        LocalDateTime getPasswordChangedAt();
    }

    @Query("SELECT u.id AS id, u.passwordChangedAt AS passwordChangedAt FROM User u WHERE u.id = :id")
    Optional<TokenCheck> findTokenCheckById(@Param("id") Long id);
}
