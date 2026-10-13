import { createContext, useContext } from 'react';

//Giriş bilgisinin bütün bileşenlere dağıtıldığı ortak kutu
export const AuthContext = createContext(null);

//Bileşenler giriş bilgisine bununla ulaşır: const { user, signIn, signOut } = useAuth();
export function useAuth() {
  return useContext(AuthContext);
}
