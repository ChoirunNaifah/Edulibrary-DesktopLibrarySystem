/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import koneksi.koneksi;
import java.awt.Color;
import net.sf.jasperreports.engine.JRResultSetDataSource;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.swing.JRViewer;
import net.sf.jasperreports.view.JasperViewer;

/**
 *
 * @author Dell
 */
public class k_daftarPeminjamanSiswa extends javax.swing.JFrame {

    /**
     * Creates new form peminjamans_page
     */
    public k_daftarPeminjamanSiswa() {
        initComponents();
        this.setSize(835, 576);
        this.setResizable(false);
        setTableTransparent();
        this.setLocationRelativeTo(null);
        
        
        
    

        model = new DefaultTableModel();
        tblPeminjaman.setModel(model);
        model.addColumn("ID Peminjaman");
        model.addColumn("NISN Siswa");
        model.addColumn("Nama SIswa");
        model.addColumn("Kode Buku");
        model.addColumn("Judul Buku");
        model.addColumn("Dipinjam Pada");
        model.addColumn("Dikembalikan Pada");
        model.addColumn("Status");
        loadBorrowingData();

        if (cmbTahun != null) {
            cmbTahun.removeAllItems();
            cmbTahun.addItem("-- Pilih Tahun --");

            // Tambahkan 5 tahun terakhir
            int currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);
            for (int i = currentYear; i >= currentYear - 5; i--) {
                cmbTahun.addItem(String.valueOf(i));
            }
        }
    }
    
    
     private void setTableTransparent() {

    // ===== JTable =====
    tblPeminjaman.setOpaque(false);
    tblPeminjaman.setBackground(new java.awt.Color(0, 0, 0, 0));
    tblPeminjaman.setForeground(java.awt.Color.BLACK);

    // Tinggi baris
    tblPeminjaman.setRowHeight(30);

    // Warna seleksi
    tblPeminjaman.setSelectionBackground(
            new java.awt.Color(30, 144, 255, 180)
    );
    tblPeminjaman.setSelectionForeground(java.awt.Color.WHITE);

    // ===== GARIS TABEL =====
    tblPeminjaman.setShowGrid(true);
    tblPeminjaman.setGridColor(new java.awt.Color(180, 180, 180, 160)); // garis halus

    // ===== JScrollPane =====
    jScrollPane2.setOpaque(false);
    jScrollPane2.getViewport().setOpaque(false);

    // ===== Header =====
    tblPeminjaman.getTableHeader().setOpaque(false);
    tblPeminjaman.getTableHeader().setBackground(
            new java.awt.Color(255, 255, 255, 170)
    );
    tblPeminjaman.getTableHeader().setForeground(java.awt.Color.BLACK);

    // Font
    tblPeminjaman.setFont(
            new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 14)
    );
    tblPeminjaman.getTableHeader().setFont(
            new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 14)
    );
     }

    private DefaultTableModel model;
    ResultSet rs = null;
    PreparedStatement pst = null;
    String lblSummary;

    private void loadBorrowingData() {
        DefaultTableModel model = (DefaultTableModel) tblPeminjaman.getModel();
        model.setRowCount(0);

        // Pastikan ada session siswa yang aktif
        if (session.session_siswa.nisn == null) {
            JOptionPane.showMessageDialog(this, "Session tidak ditemukan. Silakan login kembali.");
            return;
        }

        try (Connection conn = koneksi.KoneksiDB()) {
            // Query dengan filter NISN siswa yang login
            String sql = "SELECT b.id, b.student_nisn, b.status, s.name AS student_name, "
                    + "GROUP_CONCAT(bd.book_code SEPARATOR ', ') AS book_codes, "
                    + "GROUP_CONCAT(bo.title SEPARATOR ', ') AS book_titles, "
                    + "b.borrowed_at, b.returned_at "
                    + "FROM borrowings b "
                    + "JOIN students s ON b.student_nisn = s.nisn "
                    + "JOIN borrowing_details bd ON b.id = bd.borrowing_id "
                    + "JOIN book_items bi ON bd.book_code = bi.code "
                    + "JOIN books bo ON bi.book_id = bo.id "
                    + "WHERE b.student_nisn = ? " // Filter berdasarkan NISN session
                    + "GROUP BY b.id, b.student_nisn, s.name, b.borrowed_at, b.returned_at, b.status "
                    + "ORDER BY b.borrowed_at DESC";

            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setString(1, session.session_siswa.nisn); // Set parameter NISN dari session

            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getString("student_nisn"),
                    rs.getString("student_name"),
                    rs.getString("book_codes"),
                    rs.getString("book_titles"),
                    rs.getTimestamp("borrowed_at"),
                    rs.getDate("returned_at"),
                    rs.getString("status")
                });
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal Load Data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        btnBack3 = new javax.swing.JButton();
        cmbTahun = new javax.swing.JComboBox<>();
        btnFilterTahun = new javax.swing.JButton();
        cmbBulan = new javax.swing.JComboBox<>();
        btnFilterBulan = new javax.swing.JButton();
        btnFilterBulanDanTahun = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        tblPeminjaman = new javax.swing.JTable();
        jLabel2 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jPanel1.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        btnBack3.setBackground(new java.awt.Color(0,0,0,0));
        btnBack3.setBorder(null);
        btnBack3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnBack3ActionPerformed(evt);
            }
        });
        jPanel1.add(btnBack3, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 0, 260, 50));

        cmbTahun.setBackground(new java.awt.Color(0, 0, 0, 0));
        cmbTahun.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        cmbTahun.setBorder(null);
        jPanel1.add(cmbTahun, new org.netbeans.lib.awtextra.AbsoluteConstraints(250, 130, 140, 20));

        btnFilterTahun.setBackground(new java.awt.Color(0, 0, 0, 0));
        btnFilterTahun.setBorder(null);
        btnFilterTahun.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnFilterTahunActionPerformed(evt);
            }
        });
        jPanel1.add(btnFilterTahun, new org.netbeans.lib.awtextra.AbsoluteConstraints(390, 110, 40, 40));

        cmbBulan.setBackground(new java.awt.Color(0, 0, 0, 0));
        cmbBulan.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        cmbBulan.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "-- Pilih Bulan --", "Januari", "Februari", "Maret", "April", "Mei", "Juni", "Juli", "Agustus", "September", "Oktober", "November", "Desember", " " }));
        cmbBulan.setBorder(null);
        jPanel1.add(cmbBulan, new org.netbeans.lib.awtextra.AbsoluteConstraints(80, 130, -1, 20));

        btnFilterBulan.setBackground(new java.awt.Color(0, 0, 0, 0));
        btnFilterBulan.setBorder(null);
        btnFilterBulan.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnFilterBulanActionPerformed(evt);
            }
        });
        jPanel1.add(btnFilterBulan, new org.netbeans.lib.awtextra.AbsoluteConstraints(190, 110, 50, 40));

        btnFilterBulanDanTahun.setBackground(new java.awt.Color(0, 0, 0, 0));
        btnFilterBulanDanTahun.setBorder(null);
        btnFilterBulanDanTahun.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnFilterBulanDanTahunActionPerformed(evt);
            }
        });
        jPanel1.add(btnFilterBulanDanTahun, new org.netbeans.lib.awtextra.AbsoluteConstraints(430, 110, 130, 40));

        tblPeminjaman.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null}
            },
            new String [] {
                "ID Peminjaman", "NISN Siswa", "Nama Siswa", "Kode Buku", "Judul Buku", "Dipinjam pada", "Dikembalikan pada", "Status"
            }
        ));
        tblPeminjaman.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                tblPeminjamanMouseClicked(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                tblPeminjamanMouseEntered(evt);
            }
        });
        jScrollPane2.setViewportView(tblPeminjaman);

        jPanel1.add(jScrollPane2, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 170, 700, 340));

        jLabel2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/image/DAFTAR PEMINJAMAN - SISWA.png"))); // NOI18N
        jPanel1.add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, -1, -1));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnBack3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBack3ActionPerformed
        // TODO add your handling code here:
        this.dispose();
    }//GEN-LAST:event_btnBack3ActionPerformed

    private void tblPeminjamanMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tblPeminjamanMouseClicked
        // TODO add your handling code here:
        
    }//GEN-LAST:event_tblPeminjamanMouseClicked

    private void tblPeminjamanMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tblPeminjamanMouseEntered
        // TODO add your handling code here:
    }//GEN-LAST:event_tblPeminjamanMouseEntered

    private void btnFilterTahunActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnFilterTahunActionPerformed
        // TODO add your handling code here:
        int indexTahun = cmbTahun.getSelectedIndex();

        // Index 0 = belum pilih tahun
        if (indexTahun == 0) {
            JOptionPane.showMessageDialog(this,
                    "Harap pilih tahun terlebih dahulu!",
                    "Peringatan",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Pastikan ada session siswa yang aktif
        if (session.session_siswa.nisn == null) {
            JOptionPane.showMessageDialog(this, "Session tidak ditemukan. Silakan login kembali.");
            return;
        }

        int tahun = Integer.parseInt(cmbTahun.getSelectedItem().toString());

        DefaultTableModel model = (DefaultTableModel) tblPeminjaman.getModel();
        model.setRowCount(0);

        try (Connection conn = koneksi.KoneksiDB()) {

            String sql = "SELECT b.id, b.student_nisn, b.status, s.name AS student_name, "
                    + "GROUP_CONCAT(bd.book_code SEPARATOR ', ') AS book_codes, "
                    + "GROUP_CONCAT(bo.title SEPARATOR ', ') AS book_titles, "
                    + "b.borrowed_at, b.returned_at "
                    + "FROM borrowings b "
                    + "JOIN students s ON b.student_nisn = s.nisn "
                    + "JOIN borrowing_details bd ON b.id = bd.borrowing_id "
                    + "JOIN book_items bi ON bd.book_code = bi.code "
                    + "JOIN books bo ON bi.book_id = bo.id "
                    + "WHERE YEAR(b.borrowed_at) = ? AND b.student_nisn = ? " // Tambah filter NISN
                    + "GROUP BY b.id, b.student_nisn, s.name, b.borrowed_at, b.returned_at, b.status "
                    + "ORDER BY b.borrowed_at DESC";

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, tahun);
            ps.setString(2, session.session_siswa.nisn); // Set NISN dari session

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getString("student_nisn"),
                    rs.getString("student_name"),
                    rs.getString("book_codes"),
                    rs.getString("book_titles"),
                    rs.getTimestamp("borrowed_at"),
                    rs.getDate("returned_at"),
                    rs.getString("status")
                });
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal filter data: " + e.getMessage());
            e.printStackTrace();
        }
    }//GEN-LAST:event_btnFilterTahunActionPerformed

    private void btnFilterBulanActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnFilterBulanActionPerformed
        // TODO add your handling code here:
        int indexBulan = cmbBulan.getSelectedIndex();

        // Index 0 = belum pilih bulan
        if (indexBulan == 0) {
            JOptionPane.showMessageDialog(this,
                    "Harap pilih bulan terlebih dahulu!",
                    "Peringatan",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Pastikan ada session siswa yang aktif
        if (session.session_siswa.nisn == null) {
            JOptionPane.showMessageDialog(this, "Session tidak ditemukan. Silakan login kembali.");
            return;
        }

        int bulan = indexBulan; // karena Januari = index 1

        DefaultTableModel model = (DefaultTableModel) tblPeminjaman.getModel();
        model.setRowCount(0);

        try (Connection conn = koneksi.KoneksiDB()) {

            String sql = "SELECT b.id, b.student_nisn, b.status, s.name AS student_name, "
                    + "GROUP_CONCAT(bd.book_code SEPARATOR ', ') AS book_codes, "
                    + "GROUP_CONCAT(bo.title SEPARATOR ', ') AS book_titles, "
                    + "b.borrowed_at, b.returned_at "
                    + "FROM borrowings b "
                    + "JOIN students s ON b.student_nisn = s.nisn "
                    + "JOIN borrowing_details bd ON b.id = bd.borrowing_id "
                    + "JOIN book_items bi ON bd.book_code = bi.code "
                    + "JOIN books bo ON bi.book_id = bo.id "
                    + "WHERE MONTH(b.borrowed_at) = ? AND b.student_nisn = ? " // Tambah filter NISN
                    + "GROUP BY b.id, b.student_nisn, s.name, b.borrowed_at, b.returned_at, b.status "
                    + "ORDER BY b.borrowed_at DESC";

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, bulan);
            ps.setString(2, session.session_siswa.nisn); // Set NISN dari session

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getString("student_nisn"),
                    rs.getString("student_name"),
                    rs.getString("book_codes"),
                    rs.getString("book_titles"),
                    rs.getTimestamp("borrowed_at"),
                    rs.getDate("returned_at"),
                    rs.getString("status")
                });
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal filter data: " + e.getMessage());
            e.printStackTrace();
        }
    }//GEN-LAST:event_btnFilterBulanActionPerformed

    private void btnFilterBulanDanTahunActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnFilterBulanDanTahunActionPerformed
        // TODO add your handling code here:
        int indexBulan = cmbBulan.getSelectedIndex();
        int indexTahun = cmbTahun.getSelectedIndex();

        // Validasi pilihan
        if (indexBulan == 0 || indexTahun == 0) {
            JOptionPane.showMessageDialog(this,
                    "Harap pilih bulan dan tahun terlebih dahulu!",
                    "Peringatan",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Pastikan ada session siswa yang aktif
        if (session.session_siswa.nisn == null) {
            JOptionPane.showMessageDialog(this, "Session tidak ditemukan. Silakan login kembali.");
            return;
        }

        int bulan = indexBulan; 
        int tahun = Integer.parseInt(cmbTahun.getSelectedItem().toString());

        DefaultTableModel model = (DefaultTableModel) tblPeminjaman.getModel();
        model.setRowCount(0);

        try (Connection conn = koneksi.KoneksiDB()) {

            String sql = "SELECT b.id, b.student_nisn, b.status, s.name AS student_name, "
                    + "GROUP_CONCAT(bd.book_code SEPARATOR ', ') AS book_codes, "
                    + "GROUP_CONCAT(bo.title SEPARATOR ', ') AS book_titles, "
                    + "b.borrowed_at, b.returned_at "
                    + "FROM borrowings b "
                    + "JOIN students s ON b.student_nisn = s.nisn "
                    + "JOIN borrowing_details bd ON b.id = bd.borrowing_id "
                    + "JOIN book_items bi ON bd.book_code = bi.code "
                    + "JOIN books bo ON bi.book_id = bo.id "
                    + "WHERE MONTH(b.borrowed_at) = ? AND YEAR(b.borrowed_at) = ? AND b.student_nisn = ? " // Tambah filter NISN
                    + "GROUP BY b.id, b.student_nisn, s.name, b.borrowed_at, b.returned_at, b.status "
                    + "ORDER BY b.borrowed_at DESC";

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, bulan);
            ps.setInt(2, tahun);
            ps.setString(3, session.session_siswa.nisn); 

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getString("student_nisn"),
                    rs.getString("student_name"),
                    rs.getString("book_codes"),
                    rs.getString("book_titles"),
                    rs.getTimestamp("borrowed_at"),
                    rs.getDate("returned_at"),
                    rs.getString("status")
                });
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal filter data: " + e.getMessage());
            e.printStackTrace();
        }
    }//GEN-LAST:event_btnFilterBulanDanTahunActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(k_daftarPeminjamanSiswa.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(k_daftarPeminjamanSiswa.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(k_daftarPeminjamanSiswa.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(k_daftarPeminjamanSiswa.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new k_daftarPeminjamanSiswa().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnBack3;
    private javax.swing.JButton btnFilterBulan;
    private javax.swing.JButton btnFilterBulanDanTahun;
    private javax.swing.JButton btnFilterTahun;
    private javax.swing.JComboBox<String> cmbBulan;
    private javax.swing.JComboBox<String> cmbTahun;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTable tblPeminjaman;
    // End of variables declaration//GEN-END:variables
}
