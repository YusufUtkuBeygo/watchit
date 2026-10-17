-- Kullanıcı adı benzersiz: eskiden aynı adla ikinci hesap açılıp yorumlarda başkası gibi görünülebiliyordu.
-- lower(...) ile büyük/küçük harf farkı gözetilmez: "Ayse" ile "ayse" aynı ad sayılır.
-- (Bu migration yazılırken veritabanında tekrar eden ya da boş kullanıcı adı yoktu.)
ALTER TABLE users ALTER COLUMN username SET NOT NULL;
CREATE UNIQUE INDEX uk_users_username_lower ON users (lower(username));
