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
public class session_siswa {

    public static String nisn;
    public static String nameSiswa;
    public static String kelasSiswa;

    public static void setSession(String nisn, String nameSiswa, String kelasSiswa) {
        session_siswa.nisn = nisn;
        session_siswa.nameSiswa = nameSiswa;
        session_siswa.kelasSiswa = kelasSiswa;
    }

    public static void clearSession() {
        nisn = null;
        nameSiswa = null;
        kelasSiswa = null;
    }
}
