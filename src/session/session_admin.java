/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package session;

/**
 *
 * @author Dell
 */
public class session_admin {

    public static int idAdmin;
    public static String nameAdmin;

    public static void setSession(int idAdmin, String nameAdmin) {
        session_admin.idAdmin = idAdmin;
        session_admin.nameAdmin = nameAdmin;
    }

    public static void clearSession() {
        idAdmin = 0;
        nameAdmin = null;
    }
}
