/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import java.io.FileOutputStream;
import java.io.File;
import java.awt.Desktop;
import com.itextpdf.text.Image;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat; 
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import koneksi.koneksi;
import session.session_siswa;


/**
 *
 * @author 11 64
 */
public class k_riwayatSiswa extends javax.swing.JFrame {

    DefaultTableModel modelPinjam;
    DefaultTableModel modelKembali;
    // Format tanggal Indonesia: Hari-Bulan-Tahun Jam:Menit
    SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm");

    public k_riwayatSiswa() {
        setUndecorated(true);
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        initComponents();

        // Pastikan enabled agar bisa diklik dan diseleksi
        tblPinjam.setEnabled(true); 
        tblKembali.setEnabled(true);

        setupTable();
        
        // Styling putih dengan seleksi biru
        styleTable(tblPinjam, jScrollPane1);
        styleTable(tblKembali, jScrollPane2);

        loadDataPinjam();
        loadDataKembali();

        // EVENT BUTTON
        btnKembali.addActionListener(e -> dispose());
        btnLogout.addActionListener(e -> {
            session_siswa.clearSession();
            dispose();
            new c_loginSiswa().setVisible(true);
        });
    }

    private void styleTable(javax.swing.JTable table, javax.swing.JScrollPane scrollPane) {
        table.setBackground(java.awt.Color.WHITE);
        table.setOpaque(true);
        table.setForeground(java.awt.Color.BLACK); 
        
        // Warna Biru saat diklik
        table.setSelectionBackground(new java.awt.Color(0, 102, 204)); 
        table.setSelectionForeground(java.awt.Color.WHITE);           
        
        scrollPane.setBackground(java.awt.Color.WHITE);
        scrollPane.getViewport().setBackground(java.awt.Color.WHITE);
        scrollPane.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(204, 204, 204)));
        
        table.setRowHeight(35); 
        table.setShowGrid(true);
        table.setGridColor(new java.awt.Color(230, 230, 230)); 
        
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 12));
    }

    private void setupTable() {
        // Kolom sesuai dengan kebutuhan tampilan peminjaman
        modelPinjam = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        tblPinjam.setModel(modelPinjam);
        modelPinjam.addColumn("ID Pinjam");
        modelPinjam.addColumn("Kode Buku");
        modelPinjam.addColumn("Judul Buku");
        modelPinjam.addColumn("Tgl Pinjam");
        modelPinjam.addColumn("Batas Kembali");
        modelPinjam.addColumn("Status");

        // Kolom sesuai dengan kebutuhan tampilan pengembalian
        modelKembali = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        tblKembali.setModel(modelKembali);
        modelKembali.addColumn("ID Kembali");
        modelKembali.addColumn("Kode Buku");
        modelKembali.addColumn("Judul Buku");
        modelKembali.addColumn("Tgl Pinjam");
        modelKembali.addColumn("Tgl Kembali");
        modelKembali.addColumn("Denda");
        modelKembali.addColumn("Status");
    }

    private void loadDataPinjam() {
        try {
            Connection conn = koneksi.KoneksiDB();
            // Query untuk mengambil data peminjaman aktif/riwayat pinjam
            String sql = "SELECT b.id, bi.code, bk.title, b.borrowed_at, b.returned_at, b.status " +
                         "FROM borrowings b " +
                         "JOIN borrowing_details bd ON b.id = bd.borrowing_id " +
                         "JOIN book_items bi ON bd.book_code = bi.code " +
                         "JOIN books bk ON bi.book_id = bk.id " +
                         "WHERE b.student_nisn = ? " +
                         "ORDER BY b.borrowed_at DESC";

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, session_siswa.nisn);
            ResultSet rs = ps.executeQuery();
            modelPinjam.setRowCount(0);

            while (rs.next()) {
                modelPinjam.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getString("code"),
                    rs.getString("title"),
                    rs.getTimestamp("borrowed_at") != null ? sdf.format(rs.getTimestamp("borrowed_at")) : "-",
                    rs.getTimestamp("returned_at") != null ? sdf.format(rs.getTimestamp("returned_at")) : "-",
                    rs.getString("status").toUpperCase() // Status dari DB: process, approved, rejected, done
                });
            }
        } catch (Exception e) {
            System.out.println("Error Pinjam: " + e.getMessage());
        }
    }

    private void loadDataKembali() {
        try {
            Connection conn = koneksi.KoneksiDB();
            // Query untuk mengambil data yang sudah ada di tabel 'returns'
            String sql = "SELECT r.id AS return_id, bi.code, bk.title, b.borrowed_at, r.returned_at AS real_return, r.charge, r.status " +
                         "FROM returns r " +
                         "JOIN borrowings b ON r.borrowing_id = b.id " +
                         "JOIN borrowing_details bd ON b.id = bd.borrowing_id " +
                         "JOIN book_items bi ON bd.book_code = bi.code " +
                         "JOIN books bk ON bi.book_id = bk.id " +
                         "WHERE b.student_nisn = ? " +
                         "ORDER BY r.returned_at DESC";

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, session_siswa.nisn);
            ResultSet rs = ps.executeQuery();
            modelKembali.setRowCount(0);

            while (rs.next()) {
                modelKembali.addRow(new Object[]{
                    rs.getInt("return_id"),
                    rs.getString("code"),
                    rs.getString("title"),
                    rs.getTimestamp("borrowed_at") != null ? sdf.format(rs.getTimestamp("borrowed_at")) : "-",
                    rs.getTimestamp("real_return") != null ? sdf.format(rs.getTimestamp("real_return")) : "-",
                    "Rp " + rs.getInt("charge"),
                    rs.getString("status").toUpperCase() // Status dari tabel returns: process, done
                });
            }
        } catch (Exception e) {
            System.out.println("Error Kembali: " + e.getMessage());
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
        jScrollPane1 = new javax.swing.JScrollPane();
        tblPinjam = new javax.swing.JTable();
        jScrollPane2 = new javax.swing.JScrollPane();
        tblKembali = new javax.swing.JTable();
        btnLogout = new javax.swing.JButton();
        btnKembali = new javax.swing.JButton();
        btnCetak = new javax.swing.JButton();
        btnDashboard = new javax.swing.JButton();
        btnKelolaPeminjaman = new javax.swing.JButton();
        btnKelolaPengembalian = new javax.swing.JButton();
        btnRiwayatSiswa = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        lblNotif = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        getContentPane().setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jPanel1.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        tblPinjam.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane1.setViewportView(tblPinjam);

        jPanel1.add(jScrollPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(310, 190, 970, 160));

        tblKembali.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane2.setViewportView(tblKembali);

        jPanel1.add(jScrollPane2, new org.netbeans.lib.awtextra.AbsoluteConstraints(310, 430, 970, 150));

        btnLogout.setBackground(new java.awt.Color(0,0,0,0));
        jPanel1.add(btnLogout, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 680, 190, 70));
        jPanel1.add(btnKembali, new org.netbeans.lib.awtextra.AbsoluteConstraints(690, 270, 80, 40));

        btnCetak.setBackground(new java.awt.Color(0,0,0,0));
        btnCetak.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCetakActionPerformed(evt);
            }
        });
        jPanel1.add(btnCetak, new org.netbeans.lib.awtextra.AbsoluteConstraints(570, 620, 440, 60));

        btnDashboard.setBackground(new java.awt.Color(0, 0, 0, 0));
        btnDashboard.setBorder(null);
        btnDashboard.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDashboardActionPerformed(evt);
            }
        });
        jPanel1.add(btnDashboard, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 140, 240, 40));

        btnKelolaPeminjaman.setBackground(new java.awt.Color(0, 0, 0, 0));
        btnKelolaPeminjaman.setBorder(null);
        btnKelolaPeminjaman.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnKelolaPeminjamanActionPerformed(evt);
            }
        });
        jPanel1.add(btnKelolaPeminjaman, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 190, 240, 30));

        btnKelolaPengembalian.setBackground(new java.awt.Color(0, 0, 0, 0));
        btnKelolaPengembalian.setBorder(null);
        btnKelolaPengembalian.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnKelolaPengembalianActionPerformed(evt);
            }
        });
        jPanel1.add(btnKelolaPengembalian, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 230, 240, 30));

        btnRiwayatSiswa.setBackground(new java.awt.Color(0, 0, 0, 0));
        btnRiwayatSiswa.setBorder(null);
        btnRiwayatSiswa.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRiwayatSiswaActionPerformed(evt);
            }
        });
        jPanel1.add(btnRiwayatSiswa, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 280, 240, 30));

        jLabel1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/image/RIWAYAT -SISWA.png"))); // NOI18N
        jPanel1.add(jLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, -1, -1));

        lblNotif.setIcon(new javax.swing.ImageIcon(getClass().getResource("/image/PINJAM BUKU - SISWA (1).png"))); // NOI18N
        lblNotif.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                lblNotifMouseClicked(evt);
            }
        });
        jPanel1.add(lblNotif, new org.netbeans.lib.awtextra.AbsoluteConstraints(1260, 10, 90, 70));

        getContentPane().add(jPanel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 1370, 770));

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnCetakActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCetakActionPerformed
                                            
    // Inisialisasi JFileChooser untuk memilih lokasi penyimpanan
    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setDialogTitle("Simpan Laporan Riwayat");
    fileChooser.setFileFilter(new FileNameExtensionFilter("PDF Document", "pdf"));
    fileChooser.setSelectedFile(new File("Laporan_Riwayat_" + session_siswa.nisn + ".pdf"));

    if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
        String path = fileChooser.getSelectedFile().getAbsolutePath();
        if (!path.toLowerCase().endsWith(".pdf")) {
            path += ".pdf";
        }

        Document document = new Document();
        try {
            PdfWriter.getInstance(document, new FileOutputStream(path));
            document.open();

            // Definisi Warna Biru Muda & Font
            BaseColor biruMuda = new BaseColor(180, 215, 255); // Warna Biru Muda
            Font fontJudul = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
            Font fontHeaderTabel = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD);
            Font fontIsiTabel = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);

            // Masukkan Logo (Path sesuai folder src/image)
            try {
                java.net.URL logoUrl = getClass().getResource("/image/logoo.jpeg");
                if (logoUrl != null) {
                    Image logo = Image.getInstance(logoUrl);
                    logo.scaleToFit(80, 80);
                    logo.setAlignment(Element.ALIGN_CENTER);
                    document.add(logo);
                }
            } catch (Exception e) {
                System.out.println("Logo tidak ditemukan: " + e.getMessage());
            }

            // Header Dokumen (Judul)
            Paragraph pJudul = new Paragraph("LAPORAN RIWAYAT EDU-LIBRARY\n\n", fontJudul);
            pJudul.setAlignment(Element.ALIGN_CENTER);
            document.add(pJudul);

            document.add(new Paragraph("NISN: " + session_siswa.nisn, fontIsiTabel));
            document.add(new Paragraph("Nama: " + session_siswa.nameSiswa, fontIsiTabel));
            document.add(new Paragraph("Dicetak pada: " + new java.util.Date().toString() + "\n\n", fontIsiTabel));

            // Tabel Peminjaman (Header Biru Muda)
            document.add(new Paragraph("A. Riwayat Peminjaman\n", fontHeaderTabel));
            PdfPTable table1 = new PdfPTable(tblPinjam.getColumnCount());
            table1.setWidthPercentage(100);
            table1.setSpacingBefore(10f);

            // Membuat Header Tabel 1
            for (int i = 0; i < tblPinjam.getColumnCount(); i++) {
                PdfPCell cell = new PdfPCell(new Phrase(tblPinjam.getColumnName(i), fontHeaderTabel));
                cell.setBackgroundColor(biruMuda); // Warna Biru Muda
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setPadding(5);
                table1.addCell(cell);
            }
            // Isi Tabel 1
            for (int r = 0; r < tblPinjam.getRowCount(); r++) {
                for (int c = 0; c < tblPinjam.getColumnCount(); c++) {
                    Object val = tblPinjam.getValueAt(r, c);
                    table1.addCell(new Phrase(val != null ? val.toString() : "-", fontIsiTabel));
                }
            }
            document.add(table1);
            document.add(new Paragraph("\n"));

            // 6. Tabel Pengembalian (Header Biru Muda)
            document.add(new Paragraph("B. Riwayat Pengembalian\n", fontHeaderTabel));
            PdfPTable table2 = new PdfPTable(tblKembali.getColumnCount());
            table2.setWidthPercentage(100);
            table2.setSpacingBefore(10f);

            // Membuat Header Tabel 2
            for (int i = 0; i < tblKembali.getColumnCount(); i++) {
                PdfPCell cell = new PdfPCell(new Phrase(tblKembali.getColumnName(i), fontHeaderTabel));
                cell.setBackgroundColor(biruMuda); // Warna Biru Muda
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setPadding(5);
                table2.addCell(cell);
            }
            // Isi Tabel 2
            for (int r = 0; r < tblKembali.getRowCount(); r++) {
                for (int c = 0; c < tblKembali.getColumnCount(); c++) {
                    Object val = tblKembali.getValueAt(r, c);
                    table2.addCell(new Phrase(val != null ? val.toString() : "-", fontIsiTabel));
                }
            }
            document.add(table2);

            // Selesai
            document.close();
            
            // Konfirmasi Buka File
            int confirm = JOptionPane.showConfirmDialog(this, "Laporan Berhasil Dibuat. Buka File Sekarang?", "Sukses", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                Desktop.getDesktop().open(new File(path));
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal Mencetak PDF: " + e.getMessage());
        }
    }



    }//GEN-LAST:event_btnCetakActionPerformed

    private void btnDashboardActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDashboardActionPerformed
        // TODO add your handling code here:
        d_dashboardSiswa obj = new  d_dashboardSiswa();
        obj.setVisible(true);
    }//GEN-LAST:event_btnDashboardActionPerformed

    private void btnKelolaPeminjamanActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnKelolaPeminjamanActionPerformed
        // TODO add your handling code here:
        k_kelolaPeminjamanSiswa obj = new k_kelolaPeminjamanSiswa();
        obj.setVisible(true);
    }//GEN-LAST:event_btnKelolaPeminjamanActionPerformed

    private void btnKelolaPengembalianActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnKelolaPengembalianActionPerformed
        // TODO add your handling code here:
        k_kelolaPengembalianSiswa obj = new k_kelolaPengembalianSiswa();
        obj.setVisible(true);
    }//GEN-LAST:event_btnKelolaPengembalianActionPerformed

    private void btnRiwayatSiswaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRiwayatSiswaActionPerformed
        // TODO add your handling code here:
        k_riwayatSiswa obj = new   k_riwayatSiswa();
        obj.setVisible(true);
    }//GEN-LAST:event_btnRiwayatSiswaActionPerformed

    private void lblNotifMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_lblNotifMouseClicked

        // 1. Cek apakah ada session (keamanan tambahan)
        if (session.session_siswa.nisn == null) {
            javax.swing.JOptionPane.showMessageDialog(this, "Silahkan login terlebih dahulu");
            return;
        }

        // 2. Panggil DialogNotifikasi
        // Gunakan 'this' agar pop-up mereferensi ke frame utama
        DialogNotifikasi nav = new DialogNotifikasi(new javax.swing.JFrame(), false);

        // 3. Atur posisi agar muncul tepat di bawah icon lonceng
        int x = lblNotif.getLocationOnScreen().x - 280; // Sesuaikan agar pas
        int y = lblNotif.getLocationOnScreen().y + lblNotif.getHeight();

        nav.setLocation(x, y);
        nav.setVisible(true);

        // 4. Tutup otomatis jika user mengklik di luar dialog
        nav.addWindowFocusListener(new java.awt.event.WindowFocusListener() {
            public void windowGainedFocus(java.awt.event.WindowEvent evt) {}
            public void windowLostFocus(java.awt.event.WindowEvent evt) {
                nav.dispose();
            }
        });
    }//GEN-LAST:event_lblNotifMouseClicked

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
            java.util.logging.Logger.getLogger(k_riwayatSiswa.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(k_riwayatSiswa.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(k_riwayatSiswa.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(k_riwayatSiswa.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new k_riwayatSiswa().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnCetak;
    private javax.swing.JButton btnDashboard;
    private javax.swing.JButton btnKelolaPeminjaman;
    private javax.swing.JButton btnKelolaPengembalian;
    private javax.swing.JButton btnKembali;
    private javax.swing.JButton btnLogout;
    private javax.swing.JButton btnRiwayatSiswa;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JLabel lblNotif;
    private javax.swing.JTable tblKembali;
    private javax.swing.JTable tblPinjam;
    // End of variables declaration//GEN-END:variables
}
