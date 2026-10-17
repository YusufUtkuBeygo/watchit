package com.yutkubeygo.watchit.repository;

import com.yutkubeygo.watchit.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User,Long> {

    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    //IgnoreCase: büyük/küçük harf farkı gözetmeden karşılaştırır ("Ayse" = "ayse")
    boolean existsByUsernameIgnoreCase(String username);

}
